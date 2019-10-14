/*
 * Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.patchvalidator.validators;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.interfaces.CommonValidator;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;
import java.util.Properties;

import static org.wso2.patchvalidator.constants.Constants.*;

/**
 * <h1>Patch Validator</h1>
 * Validate patches considering all the file structure and content.
 *
 * @author Kosala Herath, Senthan Prasanth, Thushanthan
 * @version 1.2
 * @since 2017-12-14
 */
public class PatchValidator {

    private static final Logger LOG = LogBuilder.getInstance().LOG;
    private static Properties prop = PropertyLoader.getInstance().prop;

    public String patchUrl = "null";
    public String patchDestination = "null";

    static PatchValidateFactory getPatchValidateFactory(String filepath) {

        if (filepath.endsWith(".zip")) {
            return new PatchValidateFactory();
        }
        return null;
    }

    public String zipPatchValidate(String patchId, String version, int type, String[] productNameArray) {

        StringBuilder developerMessage = new StringBuilder();

        String typeof = null;
        if (type == 1 || type == 3) {
            typeof = "patch";
        }

        ZipDownloadPath zipDownloadPath = new ZipDownloadPath(typeof, version, patchId);
        String filepath = zipDownloadPath.getFilepath();
        patchUrl = zipDownloadPath.getUrl();
        patchDestination = zipDownloadPath.getZipDownloadDestination();
        String destFilePath = zipDownloadPath.getDestFilePath();
        String unzippedFolderPath = zipDownloadPath.getUnzippedFolderPath();

        version = prop.getProperty(version);

        PatchValidateFactory patchValidateFactory = PatchValidator.getPatchValidateFactory(filepath);
        assert patchValidateFactory != null;
        CommonValidator commonValidator = patchValidateFactory.getCommonValidation(filepath);

        String result = commonValidator.downloadZipFile(patchUrl, version, patchId, patchDestination);
        if (!Objects.equals(result, "")) {
            return result;
        }

        File fl = new File(patchDestination);
        for (File file : Objects.requireNonNull(fl.listFiles())) {
            if (file.getName().endsWith(".md5") || file.getName().endsWith((".asc"))
                    || file.getName().endsWith((".sha1"))) {
                developerMessage.append("patch: \"").append(patchId).append(" \" was already signed. ");
                try {
                    FileUtils.deleteDirectory(new File(destFilePath));
                } catch (Exception ex) {
                    throw new ServiceException("delete the temporary file",
                            " Cannot delete temporary file. Internal Problem. Contact Admin. ", ex);
                }
                LOG.error(developerMessage.toString());
                return developerMessage.toString();

            }
        }


        //unzip the patch in the temp directory
        try {
            commonValidator.unZip(new File(filepath), patchDestination);
        } catch (Exception ex) {
            LOG.error("unzipping the patch at the destination failed", ex);
            developerMessage.append(INTERNAL_PROBLEM).append("patch: \"").append(patchId)
                    .append("\". ").append(CONTACT_ADMIN);
            return developerMessage.toString();
        }
        try {
            //validate patch from checking standards
            developerMessage.append(commonValidator.checkContent(unzippedFolderPath, patchId));
            developerMessage.append(commonValidator.checkLicense(unzippedFolderPath + "LICENSE.txt"));
            developerMessage.append(commonValidator.checkReadMe(unzippedFolderPath, patchId, productNameArray));
            developerMessage.append(commonValidator.checkNotAContribution(unzippedFolderPath +
                    "NOT_A_CONTRIBUTION.txt"));

            if (!commonValidator.checkPatch(unzippedFolderPath +
                    "patch" + patchId + "/", patchId)) {
                developerMessage.append("Relevant jar files do not exist within the zip file. resubmit the files. ")
                        .append("patch: \"").append(patchId).append("\". ");
            }


        } catch (IOException | SQLException exe) {
            developerMessage.append("Internal Problem, ").append("patch:").append(patchId).append(". ")
                    .append(CONTACT_ADMIN);
            throw new ServiceException("Getting information for validate the patch have some error. patchId: \"" +
                    patchId + "\"", developerMessage.toString(), exe);
        }


        if (Objects.equals(developerMessage.toString(), "")) {
            return Constants.SUCCESSFULLY_VALIDATED;
        } else {
            try {
                FileUtils.deleteDirectory(new File(destFilePath));
            } catch (Exception ex) {
                throw new ServiceException("delete the temporary file",
                        " Cannot delete temporary file. Internal Problem. Contact Admin. ", ex);
            }
            LOG.error("Patch validation failed: " + developerMessage);
            return developerMessage.toString();
        }

    }

}

