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

package org.wso2.patchvalidator.entryvalidator;

import java.util.*;

import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;

import java.sql.SQLException;

/**
 * Validate the PMT patch information entry.
 */
public class PmtEntryValidator {

    public boolean validatePmtEntry(PatchInfo patchInfo) {

        boolean isValidOverviewCompatibleProducts = false;
        boolean isValidOverviewProducts = false;
        boolean isValidPatchInformationJarsInvolved = false;

        try {
            //get Wum product JSON object <- base64
            WumProductsInfo wumProductsInfo = new WumProductsInfo();
            String base64WumProductsJson = patchInfo.getWumProductsJSON();
            WumProductsInfo wumProductsInfoObj = WumProductsInfo.
                    createWumProductsInfoObjectFromBase64(base64WumProductsJson);

            //1. overview_compatibleProducts validation
            isValidOverviewCompatibleProducts = validateOverviewCompatibleProducts(patchInfo, wumProductsInfo);
            //2. overview_products validation
            isValidOverviewProducts = validateOverviewProducts(patchInfo, wumProductsInfo);
            //3. patchInformation_jarsInvolved validation
            isValidPatchInformationJarsInvolved = validatePatchInformationJarsInvolved(patchInfo, wumProductsInfo);
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred when validating pmt entry", ex.getDeveloperMessage(), ex);
        }
        return isValidOverviewCompatibleProducts && isValidOverviewProducts && isValidPatchInformationJarsInvolved;
    }

    private static boolean validateOverviewCompatibleProducts(PatchInfo pmtRes, WumProductsInfo wumRes) {

        List<String> pmtCompProducts = (List<String>) pmtRes.getOverviewCompatibleProducts();
        List<WumProduct> wumCompProducts = wumRes.getCompatibleProducts();

        try {
            boolean isValid = checkAllWumProductsIsInPmtProducts(wumCompProducts, pmtCompProducts);
            return isValid;
        } catch (ServiceException ex) {
            throw new ServiceException(
                    "Exception occurred when validating compatible products," + "wumCompProducts:" + wumCompProducts
                            + " pmtCompProducts:" + pmtCompProducts,
                    ex.getDeveloperMessage() + " \"Compatible products\"", ex);
        }
    }

    private static boolean validateOverviewProducts(PatchInfo pmtRes, WumProductsInfo wumRes) {

        List<String> pmtProducts = (List<String>) pmtRes.getOverviewProducts();
        List<WumProduct> wumCompatibleProducts = wumRes.getCompatibleProducts();
        List<WumProduct> wumPartialProducts = wumRes.getPartiallyApplicableProducts();

        boolean isCompValid = false;
        boolean isPartValid = false;

        try {
            isCompValid = checkAllWumProductsIsInPmtProducts(wumCompatibleProducts, pmtProducts);
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred when validating overview products: compatible products,"
                    + "wumCompatibleProducts:" + wumCompatibleProducts + " pmtProducts:" + pmtProducts,
                    ex.getDeveloperMessage() + " \"Products\"", ex);
        }
        try {
            isPartValid = checkAllWumProductsIsInPmtProducts(wumPartialProducts, pmtProducts);
        } catch (ServiceException ex) {
            throw new ServiceException(
                    "Exception occurred when validating overview products: partial products," + "wumCompatibleProducts:"
                            + wumPartialProducts + " pmtProducts:" + pmtProducts,
                    ex.getDeveloperMessage() + " \"Products\"", ex);
        }
        return (isCompValid && isPartValid);
    }

    private static boolean validatePatchInformationJarsInvolved(PatchInfo pmtRes, WumProductsInfo wumRes) {

        List<String> pmtJars = pmtRes.getPatchInformationJarsInvolved();
        List<WumProduct> wumCompatibleProducts = wumRes.getCompatibleProducts();

        for (WumProduct wumProduct : wumCompatibleProducts) {
            List<String> addedFiles = wumProduct.getAddedFiles();
            List<String> modifiedFiles = wumProduct.getModifiedFiles();
            List<String> removedFiles = wumProduct.getRemovedFiles();
            try {
                boolean isAddedFilesValid = checkPatchInfoJarsContainsAllWumProductsJsonFiles(pmtJars, addedFiles);
            } catch (ServiceException ex) {
                throw new ServiceException(
                        "Exception occurred when validating patch information jars involved:" + " added files,",
                        ex.getDeveloperMessage(), ex);
            }
            try {
                boolean isModifiedFilesValid = checkPatchInfoJarsContainsAllWumProductsJsonFiles(pmtJars,
                        modifiedFiles);
            } catch (ServiceException ex) {
                throw new ServiceException(
                        "Exception occurred when validating patch information jars involved:" + " modified files,",
                        ex.getDeveloperMessage(), ex);
            }
            try {
                boolean isRemovedFilesValid = checkPatchInfoJarsContainsAllWumProductsJsonFiles(pmtJars, removedFiles);
            } catch (ServiceException ex) {
                throw new ServiceException(
                        "Exception occurred when validating patch information jars involved:" + " removed files,",
                        ex.getDeveloperMessage(), ex);
            }
        }
        return true;
    }

    /**
     * check all the products in the wumProducts is in the pmtProducts
     */
    private static boolean checkAllWumProductsIsInPmtProducts(List<WumProduct> wumProducts, List<String> pmtProducts) {

        for (WumProduct wumProduct : wumProducts) {

            String wumProductAbbr = wumProduct.getProductAbbreviation();

            //get product name & version using abbreviation from WSO2_PATCH_VALIDATION_DATABASE
            PatchRequestDatabaseHandler db = new PatchRequestDatabaseHandler();
            Map<String, String> res;
            try {
                res = db.getProductDetails(wumProductAbbr);
            } catch (SQLException ex) {
                throw new ServiceException("SQL exception occurred when retrieving product name and version"
                        + "using product abbreviation, productAbbreviation:" + wumProductAbbr,
                        "Cannot retrieve product name and version using abbreviation from database, Please"
                                + "contact admin.", ex);
            }

            //check product is in the PMT compatible products
            String product = res.get("productName") + " " + res.get("productVersion");
            if (!pmtProducts.contains(product)) {
                throw new ServiceException(
                        "Exception occurred, This item not in the pmt patch json," + "item:" + product + " wumList:"
                                + wumProducts + " pmtList:" + pmtProducts,
                        "Product \"" + product + "\" is not in the products list, Please amend and " + "re-submit.");
            }
        }
        return true;
    }

    /**
     * check patch information jars involved files contains all wum products json files (3 types)
     */
    private static boolean checkPatchInfoJarsContainsAllWumProductsJsonFiles(List<String> patchInfoJarsInvolved,
            List<String> wumProductsJsonFiles) {

        if (wumProductsJsonFiles.size() > 0) {
            for (String element : wumProductsJsonFiles) {
                String[] tmp = element.split("/");
                String fileName = tmp[tmp.length - 1].trim();
                if (!patchInfoJarsInvolved.contains(fileName)) {
                    throw new ServiceException(
                            "Exception occurred, This file not contains in patch information " + "jars involved, file:"
                                    + fileName + " patchInformation_jarsInvolved:" + patchInfoJarsInvolved
                                    + "wumProductsJsonFileList:" + wumProductsJsonFiles,
                            "\"" + fileName + "\" JAR is not in the JARs involved list, "
                                    + "Please amend and re-submit.");
                }
            }
        }
        return true;
    }
}
