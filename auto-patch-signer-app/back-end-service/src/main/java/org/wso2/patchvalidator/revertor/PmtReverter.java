package org.wso2.patchvalidator.revertor;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wso2.patchvalidator.client.PmtClient;
import org.wso2.patchvalidator.enums.PatchType;
import org.wso2.patchvalidator.enums.ValidationType;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.PropertyLoader;
import org.wso2.patchvalidator.util.Util;

/**
 * <h1>PMT reverter</h1>
 * Revert PMT state when reverting patch.
 * Pre validate and Post validate to check whether particular patch can be reverted in PMT
 *
 * @author Pramodya Mendis
 * @version 1.3
 * @since 2018-07-12
 */

class PmtReverter {

    private static List<String> overviewProducts = Collections.emptyList();
    private static List<String> overviewCompatibleProducts = Collections.emptyList();
    private static String wumProductsJson = "";
    private static String lifeCycleState = "";

    /**
     * Revert PMT LC state.
     *
     * @param version   carbon version
     * @param patchId   4 digit patch id
     * @param patchType patch type (patch, update, patch and update)
     * @return is PMT reverted for the given patch
     */
    static boolean revertPmt(String version, String patchId, PatchType patchType) {

        boolean isPreValidated = false;
        boolean isPmtReverted = false;
        boolean isPostValidated = false;

        //pre validation, validate pmt json
        //pre validation does not apply for patches
        if (!(patchType == PatchType.PATCH)) {
            try {
                JSONObject pmtJson = PmtClient.getPatchInfo(version, patchId);
                isPreValidated = validatePatchInfo(pmtJson, ValidationType.PRE_VALIDATION);
            } catch (ServiceException ex) {
                throw new ServiceException("Pre validation failed, patch:" + version + "-" + patchId,
                        ex.getDeveloperMessage(), ex);
            }
        }

        //revert pmt state
        if (isPreValidated || patchType == PatchType.PATCH) {
            try {
                isPmtReverted = PmtClient.revertPmtLcState(patchId, version);
            } catch (ServiceException ex) {
                throw new ServiceException("Exception occurred when reverting pmt state, patch:" + version + "-"
                        + patchId, ex.getDeveloperMessage(), ex);
            }
        }

        //post validation, check pmt json whether patch is reverted
        //post validation does not apply for patches
        if (!(patchType == PatchType.PATCH)) {
            if (isPmtReverted) {
                JSONObject pmtJsonReverted = PmtClient.getPatchInfo(version, patchId);
                try {
                    isPostValidated = validatePatchInfo(pmtJsonReverted, ValidationType.POST_VALIDATION);
                } catch (ServiceException ex) {
                    throw new ServiceException("Exception occurred when post validating pmt json, patch:" + version + "-"
                            + patchId, ex.getDeveloperMessage(), ex);
                }
            }
        }

        if (isPostValidated || patchType == PatchType.PATCH) {
            return true;
        } else if (isPmtReverted) {
            throw new ServiceException("pre validation successful, reverting pmt lc state successful," +
                    " post validation failed," + " patch:" + version + "-" + patchId,
                    "Pre validation successful, Reverting pmt lc state successful, Post validation failed," +
                            " for the" + " patch \"" + version + "-" + patchId + "\", Please contact admin.");
        } else if (isPreValidated) {
            throw new ServiceException("pre validation successful, reverting pmt lc state failed," +
                    " patch:" + version + "-" + patchId,
                    "Pre validation successful, reverting pmt lc state failed, for the patch \"" + version +
                            "-" + patchId + "\", Please contact admin.");
        } else {
            throw new ServiceException("pre validation failed" + " patch:" + version + "-" + patchId,
                    "Pre validation failed for the" + " patch \"" + version + "-" + patchId + "\", " +
                            "Please contact admin.");
        }
    }

    /**
     * Check patch info is valid.
     *
     * @param pmtJson        patch info json
     * @param validationType validation type (pre, post)
     * @return is patch info validated
     */
    private static boolean validatePatchInfo(JSONObject pmtJson, ValidationType validationType) {

        JSONArray pmtArray = (JSONArray) pmtJson.get("pmtResult");

        for (Object aJsonArray : pmtArray) {

            JSONObject element = (JSONObject) aJsonArray;

            try {
                if (element.get("name").equals("overview_products")) {
                    overviewProducts = Util.createListFromJsonArray((JSONArray) element.get("value"));
                } else if (element.get("name").equals("overview_compatibleProducts")) {
                    overviewCompatibleProducts = Util.createListFromJsonArray((JSONArray) element.get("value"));
                } else if (element.get("name").equals("wum_productsJSON")) {
                    wumProductsJson = Util.createListFromJsonArray((JSONArray) element.get("value")).get(0);
                } else if (element.get("name").equals("registry.lifecycle.PatchLifeCycle.state")) {
                    lifeCycleState = Util.createListFromJsonArray((JSONArray) element.get("value")).get(0);
                }
            } catch (Exception ex) {
                throw new ServiceException("Exception occurred, pmt patch info is not valid",
                        "Invalid patch information.", ex);
            }
        }

        if (validationType == ValidationType.PRE_VALIDATION) {
            if (overviewProducts.size() < 1) {
                throw new ServiceException("products list is empty", "Invalid patch information, " +
                        "products list is empty. Please amend and re-submit.");
            } else if (overviewCompatibleProducts.size() < 1) {
                throw new ServiceException("compatible products list is empty", "Invalid patch information" +
                        ", compatible products list is empty. Please amend and re-submit.");
            } else if (wumProductsJson.equals("")) {
                throw new ServiceException("products json is empty", "Invalid patch information" +
                        ", compatible products list is empty. Please amend and re-submit.");
            } else if (!(lifeCycleState.equals("Staging"))) {
                throw new ServiceException("life cycle state should be Staging, lifeCycleState:" + lifeCycleState,
                        "Invalid life cycle state \"" + lifeCycleState + "\". Please change life cycle " +
                                "stage to \"Staging\" and re-submit.");
            } else {
                return true;
            }
        } else { //post validation
            if (overviewProducts.size() < 1) {
                throw new ServiceException("products list is empty", "Products list is empty after reverting" +
                        " PMT, Please contact admin.");
            } else if (!(Util.listEqualsIgnoreOrder(overviewProducts, overviewCompatibleProducts))) {
                throw new ServiceException("products and compatible products lists are not having same products, "
                        + "products:" + overviewProducts.toString() + " compatibleProducts:" +
                        overviewCompatibleProducts.toString(),
                        "products list and compatible products list is different after reverting PMT," +
                                " Please contact admin.");
            } else if (!(wumProductsJson.equals(""))) {
                throw new ServiceException("products json should be empty, productsJson:" + wumProductsJson,
                        "Products json is not empty after reverting PMT, Please contact admin.");
            } else if (!(lifeCycleState.equals("Testing"))) {
                throw new ServiceException("life cycle state should be Testing, state:" + lifeCycleState,
                        "Life cycle state is \"" + lifeCycleState + "\" after reverting PMT, It should be" +
                                "\"Testing\", Please contact admin");
            } else {
                return true;
            }
        }
    }
}
