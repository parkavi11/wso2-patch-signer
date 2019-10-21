/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 * CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.patchvalidator.validators;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;
import org.wso2.patchvalidator.util.Util;

import static org.wso2.patchvalidator.constants.Constants.*;
import static org.wso2.patchvalidator.validators.UpdateJarValidator.validateUpdateFiles;

/**
 * Validate updates considering all the file structure and content.
 */
public class UpdateValidator {

    private static Properties prop = PropertyLoader.getInstance().prop;
    private static final Logger LOG = LogBuilder.getInstance().LOG;

    public String updateUrl = "null";
    public String updateDestination = "null";

    public String zipUpdateValidate(String updateId, String version, int type, String product, String channel) {

        String typeof = null;
        if (type == 2 || type == 3) {
            typeof = "update";
        }

        //define download svn url and destination directories and file names
        ZipDownloadPath zipDownloadPath = new ZipDownloadPath(typeof, version, updateId);

        String filepath = zipDownloadPath.getFilepath();
        updateUrl = zipDownloadPath.getUrl();
        updateDestination = zipDownloadPath.getZipDownloadDestination();
        String destFilePath = zipDownloadPath.getDestFilePath();
        StringBuilder errorMessage = new StringBuilder();
        StringBuilder outMessage = new StringBuilder();
        version = prop.getProperty(version);

        PatchValidateFactory patchValidateFactory = PatchValidator.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        //use commonValidator methods and download zip
        String result = commonValidator.downloadZipFile(updateUrl, version, updateId, updateDestination);
        if (!Objects.equals(result, "")) {
            return result;
        }

        //check sign status of the update
        File fl = new File(updateDestination);
        for (File file : Objects.requireNonNull(fl.listFiles())) {
            if (file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
                    || file.getName().endsWith((".sha1")) || file.getName().endsWith((".sha256"))
                    || file.getName().endsWith((".sha512"))) {
                errorMessage.append("update \"").append(updateId).append("\" was already signed. ");
                LOG.error(errorMessage.toString());
                return errorMessage.toString();
            }
        }

        //Validating update jars
        for (File file : Objects.requireNonNull(fl.listFiles())) {
            if (file.getName().endsWith(".zip")) {
                String updateName = file.getName().replace(".zip","");
                String unzipFolder = updateDestination + updateName;
                try {
                    Util.unZip(file, unzipFolder);
                } catch (ServiceException ex) {
                    LOG.error("Unzipping the update at the destination failed, updateDestination:" + updateDestination,
                            ex);
                    errorMessage.append(INTERNAL_PROBLEM).append(ex.getDeveloperMessage()).append(" for the patch \"")
                            .append(updateId).append("\". ").append(CONTACT_ADMIN);
                    return errorMessage.toString();
                }
                try {
                    validateUpdateFiles(unzipFolder, updateName);
                } catch (ServiceException ex){
                    LOG.error("Validating jars failed for the update \"" + updateName + "\"", ex);
                    errorMessage.append(INTERNAL_PROBLEM).append(ex.getDeveloperMessage()).append(" for the patch \"")
                            .append(updateId).append("\". ").append(CONTACT_ADMIN);
                    return errorMessage.toString();
                }
            }
        }

        //update validation using WUM validator
        String updateValidateScriptPath = prop.getProperty("updateValidateScriptPath");
        String productDownloadPath = prop.getProperty("productDestinationPath");

        //get the url from database
        String productUrl = "";
        try {
            PatchRequestDatabaseHandler productData = new PatchRequestDatabaseHandler();
            productUrl = productData.getProductURL(product);
        } catch (SQLException ex) {
            errorMessage.append(INTERNAL_PROBLEM).append("Cannot access database for" +
                    " get the vanilla pack URL. ").append(CONTACT_ADMIN);
            LOG.error(errorMessage.toString());
        }
        boolean check =false;
        LOG.info("Channel :" +channel);
        if (channel.equals("full")){
            check = new File(productDownloadPath, "wso2" + product + ".zip").exists();
            LOG.info("Validation Path:"+ productDownloadPath);
        } else if (channel.equals("security" ) || channel.equals("fidelity") || channel.equals("cloud")){
            check = new File(productDownloadPath+channel, "wso2" + product + ".zip").exists();
            productDownloadPath =productDownloadPath+channel+"/";
            LOG.info("Validation Path:"+ productDownloadPath);
        }else {
            LOG.info("Channel is not available");
        }
        LOG.info("check existence:" + check);
        if (!productUrl.equals("") && !check) {
            try {
                Process executor = Runtime.getRuntime().exec("bash " + productDownloadPath +
                        "download-product.sh " + productUrl);
                executor.waitFor();
                check = true;
            } catch (InterruptedException | IOException e) {
                errorMessage.append(INTERNAL_PROBLEM).append("Cannot access the file in the server. ")
                        .append(CONTACT_ADMIN);
                LOG.error(INTERNAL_PROBLEM + "Cannot access the download-product.sh file in the server. ", e);
            }
        }

        if (check) {
            try {
                Process executor = Runtime.getRuntime().exec(updateValidateScriptPath + "wum-uc validate " +
                        filepath + " " + productDownloadPath + "wso2" + product + ".zip");
                executor.waitFor();

                BufferedReader validateMessage = new BufferedReader(new InputStreamReader(executor.getInputStream()));
                String validateReturn;
                while ((validateReturn = validateMessage.readLine()) != null) {
                    //check whether WUM error contains "zip: not a valid zip file"
                    if (validateReturn.contains("zip: not a valid zip file")) {
                        validateReturn = validateReturn.replace(Constants.WUM_UC_ERROR_MESSAGE, "");
                        validateReturn = validateReturn.replace("zip:", "") + ". ";
                        validateReturn = product + ".zip" + validateReturn + Constants.CONTACT_ADMIN;
                    }
                    outMessage.append(validateReturn);
                }
                //return result got from the WUM-UC validator
                return outMessage.toString();
            } catch (IOException | InterruptedException e) {
                errorMessage.append("Internal Problem: Cannot connect with the WUM-UC validator tool");
                LOG.error(errorMessage.toString(), e);
            }


        } else {
            errorMessage.append("Product vanilla pack URL is incorrect or empty. Contact admin and " +
                    "update the database. Product: \"").append(product).append("\". ");
            LOG.error(errorMessage.toString());
            return errorMessage.toString();
        }
        try {
            FileUtils.deleteDirectory(new File(destFilePath));
        } catch (Exception ex) {
            throw new ServiceException("delete the temporary file",
                    "Cannot delete temporary file. Internal Problem. Contact Admin. ", ex);
        }
        return errorMessage.toString();
    }
}


