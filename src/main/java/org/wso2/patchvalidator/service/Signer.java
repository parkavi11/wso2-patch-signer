package org.wso2.patchvalidator.service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.wso2.patchvalidator.client.GregClient;
import org.wso2.patchvalidator.client.PmtClient;
import org.wso2.patchvalidator.client.SvnClient;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.entryvalidator.PatchInfo;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;
import org.wso2.patchvalidator.util.Util;
import org.wso2.patchvalidator.validators.EmailSender;
import org.wso2.patchvalidator.validators.PatchValidator;
import org.wso2.patchvalidator.validators.UpdateValidator;

import static org.wso2.patchvalidator.constants.Constants.*;
import static org.wso2.patchvalidator.validators.EmailSender.setCCList;

/**
 * Patch signing process handled by this class.
 */
class Signer {

    private static Logger LOG = LogBuilder.getInstance().LOG;
    private static Properties prop = PropertyLoader.getInstance().prop;
    private static PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
    private static final String testingState = prop.getProperty("pmtLcTestingState");
    private static final String readyToSignState = prop.getProperty("pmtLcReadyToSignState");
    private static final String adminStgState = prop.getProperty("pmtLcAdminStgState");
    private static final String commitUrlSplitter = prop.getProperty("commitUrlSpliter");
    private static final String uatStgState = prop.getProperty("pmtLcUATStgState");

    /**
     * Signing patches to be signed.
     *
     * @return patch status message
     */
    static StringBuilder sign() {

        boolean patchValidationStatus = true;
        boolean updateValidationStatus = true;
        ArrayList<String> patchesList = new ArrayList<>();
        StringBuilder returnMessage = new StringBuilder();

        // call governance registry and get all the patches in the ReadyToSign state
        try {
            patchesList = GregClient.search();
        } catch (Exception ex) {
            LOG.error("Exception occurred when searching patches in the governance registry", ex);
        }

        if (patchesList.size() < 1) {
            LOG.info("No patches in the \"Ready to sign\" state");
        }

        // iterate the patches list
        for (String patch : patchesList) {

            String carbonVersion;
            String patchId;
            String pmtUpdateStatus;
            String releasedState;
            String developer;
            String patchValidateStatus = "N/A";
            String updateValidateStatus = "N/A";
            String currentState = readyToSignState;
            String patchName = patch.replace("/patchs/", "");
            StringBuilder developerMessage = new StringBuilder();
            JSONArray pmtResultArr;
            JSONObject patchJson;
            PatchInfo patchInfo;
            PatchValidator patchValidator = new PatchValidator();
            UpdateValidator updateValidator = new UpdateValidator();

            //get version and patch id from greg patch name string
            try {
                //'/patchs/WSO2-CARBON-PATCH-4.4.0-2912'
                String[] patchReplacedNameArr = patch.replace(prop.getProperty("gregNameReplaceTerm"), "")
                        .split("-");
                carbonVersion = patchReplacedNameArr[0].trim();//carbon version - 4.2.0/4.4.0/5.0.0
                patchId = patchReplacedNameArr[1].trim(); //2912
            } catch (Exception ex) {
                LOG.error("Patch name retrieved from greg not in the correct format, patch:" + patch, ex);
                developerMessage = developerMessage.append("Patch name retrieved not in the correct format. ");
                //update developer error database
                patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState, developerMessage.toString(),
                        FAILURE_MESSAGE);
                continue;
            }

            //get PMT patch JSON
            try {
                patchJson = PmtClient.getPatchInfo(carbonVersion, patchId);
                pmtResultArr = (JSONArray) patchJson.get("pmtResult");
            } catch (ServiceException ex) {
                LOG.error("Retrieving patch json failed, patch:" + carbonVersion + "-" + patch, ex);
                //update PMT to `Testing`
                pmtUpdateStatus = updatePmtLcState(patchId, carbonVersion, testingState);
                developerMessage.append(ex.getDeveloperMessage()).append(" ").append(pmtUpdateStatus);
                //update developer error database
                if (pmtUpdateStatus.equals(Constants.PMT_UPDATE_TESTING_SUCCESSFUL)) {
                    currentState = testingState;
                }
                patchRequestDatabaseHandler.insertDataToErrorLog(patchName, currentState,
                        developerMessage.toString(), FAILURE_MESSAGE);
                continue;
            }

            //create pmt result object from pmt patch json
            try {
                patchInfo = new PatchInfo(pmtResultArr);
            } catch (Exception ex) {
                LOG.error("Creating object from pmt patch json failed, patch name: \"" + patch + "\". ", ex);
                //update PMT to `Testing`
                pmtUpdateStatus = updatePmtLcState(patchId, carbonVersion, testingState);
                developerMessage.append(INTERNAL_PROBLEM).append(CONTACT_ADMIN).append(pmtUpdateStatus);

                //update developer error database
                if (pmtUpdateStatus.equals(Constants.PMT_UPDATE_TESTING_SUCCESSFUL)) {
                    currentState = testingState;
                }
                patchRequestDatabaseHandler.insertDataToErrorLog(patchName, currentState,
                        developerMessage.toString(), FAILURE_MESSAGE);
                continue;
            }

            //get "released state" from wum status in patch info [ReleasedNotAutomated/ReleasedNotInPublicSVN/Released]
            try {
                releasedState = patchInfo.getWumStatus();
            } catch (ServiceException ex) {
                LOG.error("wum_status is not valid in pmt patch json, " + " patch: " + patch, ex);
                //update PMT to `Testing`
                pmtUpdateStatus = updatePmtLcState(patchId, carbonVersion, testingState);
                developerMessage.append(ex.getDeveloperMessage()).append(pmtUpdateStatus);
                //update developer error database
                if (pmtUpdateStatus.equals(Constants.PMT_UPDATE_TESTING_SUCCESSFUL)) {
                    currentState = testingState;
                }
                patchRequestDatabaseHandler.insertDataToErrorLog(patchName, currentState, developerMessage.toString(),
                        FAILURE_MESSAGE);
                continue;
            }

            //validate pmt entry
//
//            boolean isEntryValid;
//            PmtEntryValidator pmtEntryValidator = new  PmtEntryValidator();
//            try {
//                isEntryValid = pmtEntryValidator.validatePmtEntry(pmtPatchJson);
//            } catch (Exception ex){
//                LOG.error("PMT entry validation failed, patch name:" + patch, ex);
//                continue;
//            }
//            if (!isEntryValid){
//                LOG.error("PMT entry validation failed, patch name:" + patch);
//                continue;
//            }

            //get product list from PMT patch JSON
            List<String> productsList = patchInfo.getOverviewProducts();
            //convert product array list to an array
            String[] productNameArray = new String[productsList.size()];
            productNameArray = productsList.toArray(productNameArray);
            //get the developer of the patch
            List<String> developedBy = patchInfo.getPeopleInvolvedDevelopedBy();
            developer = developedBy.get(0);


            //get all products list, retrieving products by kernel version (carbon-4.4.1 .. )
            String[] productList;
            try {
                productList = Util.getProductList(productNameArray);
            } catch (Exception ex) {
                String kernelError = "Problem with retrieving products by kernel version from database. ";
                LOG.error(kernelError, ex);
                //update PMT to `Testing`
                pmtUpdateStatus = updatePmtLcState(patchId, carbonVersion, testingState);
                developerMessage.append(kernelError).append(CONTACT_ADMIN)
                        .append(pmtUpdateStatus);
                //update developer error database
                if (pmtUpdateStatus.equals(Constants.PMT_UPDATE_TESTING_SUCCESSFUL)) {
                    currentState = testingState;
                }
                patchRequestDatabaseHandler.insertDataToErrorLog(patchName, currentState, developerMessage.toString(),
                        FAILURE_MESSAGE);
                continue;
            }

            //iterating products list
            for (String product : productList) {

                String statusOfUpdateValidation = prop.getProperty("orgUpdate") + carbonVersion + "-" + patchId + "' "
                        + Constants.UPDATE_VALIDATED;
                //get product type from db, product details table
                int productType;
                try {
                    productType = patchRequestDatabaseHandler.getProductType(product);

                } catch (ServiceException ex) {
                    LOG.error(ex.getDeveloperMessage() + "patch:" + patchName + " product:" + product + ". ", ex);
                    developerMessage.append(ex.getDeveloperMessage());
                    patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState,
                            developerMessage.toString(), FAILURE_MESSAGE);
                    break;
                }

                try {
                    //check product type ( 1 = patch / 2 = update / 3 = patch and update )
                    if (productType == PATCH_ONLY) { //patch validation
                        patchValidateStatus = patchValidator.zipPatchValidate(patchId, carbonVersion, productType,
                                productNameArray);
                        if ((patchValidateStatus.trim().equals(Constants.SUCCESSFULLY_VALIDATED))) {
                            patchValidationStatus = true;
                        } else {
                            developerMessage.append(product).append("\" validation failed: ")
                                    .append(patchValidateStatus);
                            patchValidationStatus = false;
                            break;
                        }

                    } else if (productType == UPDATE_ONLY) { //update validation
                        updateValidateStatus = updateValidator.zipUpdateValidate(patchId, carbonVersion, productType,
                                product);

                        if (updateValidateStatus.equals(statusOfUpdateValidation)) {
                            updateValidationStatus = true;
                        } else {
                            developerMessage = developerMessage.append(" Update validation failed at ")
                                    .append(updateValidateStatus);
                            updateValidationStatus = false;
                            break;
                        }

                    } else if (productType == PATCH_AND_UPDATE) { //patch and update validation
                        patchValidateStatus = patchValidator.zipPatchValidate(patchId, carbonVersion, productType,
                                productNameArray);
                        updateValidateStatus = updateValidator.zipUpdateValidate(patchId, carbonVersion, productType,
                                product);

                        if ((patchValidateStatus.trim().equals(Constants.SUCCESSFULLY_VALIDATED))) {
                            patchValidationStatus = true;
                        } else {
                            developerMessage.append(" Patch validation failed at ")
                                    .append(patchValidateStatus);
                            patchValidationStatus = false;
                            break;
                        }

                        if (updateValidateStatus.equals(statusOfUpdateValidation)) {
                            updateValidationStatus = true;
                        } else {
                            developerMessage.append(" Update validation failed at ").append(updateValidateStatus);
                            updateValidationStatus = false;
                            break;
                        }
                    } else {
                        developerMessage.append("Product is not in the database, Please contact admin and insert " +
                                "product \"").append(product).append("\"");
                        patchValidationStatus = false;
                        updateValidationStatus = false;
                        break;
                    }
                } catch (ServiceException ex) {
                    developerMessage.append(ex.getDeveloperMessage());
                    LOG.error("Internal error: ", ex);
                }
            }

            if ((patchValidationStatus && updateValidationStatus) ||
                    (patchValidationStatus && updateValidateStatus.equals("N/A")) ||
                    (patchValidateStatus.equals("N/A") && updateValidationStatus)) {
                developerMessage.append("Patch validated successfully. ");
                LOG.info("Patch validated successfully, patch:" + patchName);
            }

            getAction(patchValidator, updateValidator, patchValidationStatus, updateValidationStatus,
                    patchValidateStatus, updateValidateStatus, patchId, carbonVersion, developer, patchName,
                    releasedState, developerMessage.toString());

            returnMessage.append(developerMessage);
        }
        return returnMessage;
    }

    /**
     * Commit patch folder in Svn and send mail to developer.
     *
     * @param objPatchValidator      PatchValidator object
     * @param objUpdateValidator     UpdateValidator object
     * @param patchValidationStatus  is patch validated
     * @param updateValidationStatus is update validated
     * @param patchValidateStatus    patch validation status message
     * @param updateValidateStatus   update validation status message
     * @param patchId                4 digit patch id
     * @param version                carbon version
     * @param developedBy            developer of the patch
     * @param patchName              full patch name
     * @param releasedState          patch released state
     * @param developerMessage       message entered to the error log table
     */
    private static void getAction(PatchValidator objPatchValidator, UpdateValidator objUpdateValidator,
                                  boolean patchValidationStatus, boolean updateValidationStatus,
                                  String patchValidateStatus, String updateValidateStatus, String patchId,
                                  String version, String developedBy, String patchName, String releasedState,
                                  String developerMessage) {

        String pmtUpdateStatus;
        ArrayList<String> toList = new ArrayList<>();
        ArrayList<String> ccList = new ArrayList<>();

        if (patchValidationStatus && updateValidateStatus.equals("N/A")) {
            pmtUpdateStatus = getActionPassedPatch(patchId, version, patchValidateStatus, releasedState,
                    objPatchValidator, patchName);
        } else if (!patchValidationStatus && updateValidateStatus.equals("N/A")) {
            pmtUpdateStatus = getActionFailedPatch(patchId, version, developerMessage, patchName);
        } else if (updateValidationStatus && patchValidateStatus.equals("N/A")) {
            pmtUpdateStatus = getActionPassedUpdate(patchId, version, updateValidateStatus, objUpdateValidator,
                    patchName);
        } else if (!updateValidationStatus && patchValidateStatus.equals("N/A")) {
            pmtUpdateStatus = getActionFailedUpdate(patchId, version, developerMessage, patchName);
        } else if (patchValidationStatus && updateValidationStatus) {
            pmtUpdateStatus = getActionPassedPatchAndUpdate(patchId, version, patchValidateStatus,
                    updateValidateStatus, objPatchValidator, objUpdateValidator, patchName);
        } else {
            pmtUpdateStatus = getActionFailedPatchAndUpdate(patchId, version, patchValidationStatus,
                    updateValidationStatus, patchValidateStatus, updateValidateStatus, patchName, developerMessage);
        }
        //send email
        setCCList(developedBy, toList, ccList);
        EmailSender.executeSendMail(toList, ccList, patchId, version, patchValidateStatus, updateValidateStatus,
                pmtUpdateStatus);
        //delete downloaded files
        deleteDownloadedPatchFilesFromServer();
    }

    /**
     * Update PMT LC state.
     *
     * @param patchId 4 digit patch id
     * @param version carbon version
     * @param state   state to be updated in the PMT
     * @return PMT update status message
     */
    private static String updatePmtLcState(String patchId, String version, String state) {

        //changed for staging => remove this when you are deploying in live
        String cVersion = version;
        if (cVersion.equals("wilkes")) {
            cVersion = "4.4.0";
        } else if (cVersion.equals("hamming")) {
            cVersion = "5.0.0";
        } else if (cVersion.equals("turing")) {
            cVersion = "4.2.0";
        }
        String pmtUpdateStatus;
        try {
            pmtUpdateStatus = PmtClient.updatePmtLcState(patchId, cVersion, state);
        } catch (Exception ex) {
            pmtUpdateStatus = "PMT update failed";
            LOG.error("Exception occurred when updating PMT LC state, patchId:" + patchId + " version:" + version +
                    " state:" + state, ex);
        }
        return pmtUpdateStatus;
    }

    /**
     * Commit keys and update PMT for validated patch.
     * Update error log database.
     *
     * @param patchId             4 digit patch id
     * @param version             carbon version
     * @param patchValidateStatus patch validation status message
     * @param releasedState       released state to be moved
     * @param objPatchValidator   object of PatchValidator
     * @param patchName           full patch name
     * @return PMT update status message
     */
    private static String getActionPassedPatch(String patchId, String version, String patchValidateStatus,
                                               String releasedState, PatchValidator objPatchValidator,
                                               String patchName) {

        String pmtUpdateStatus;
        String developerMessage;

        patchValidateStatus += " " + SvnClient.commitKeys((objPatchValidator.patchUrl.split(commitUrlSplitter)[1]),
                objPatchValidator.patchDestination, patchId, version, prop.getProperty("patch"));

        //if successful, update PMT to Released state
        if (patchValidateStatus.contains(Constants.SUCCESSFULLY_VALIDATED) &&
                patchValidateStatus.contains(Constants.SUCCESSFULLY_KEY_COMMITTED)) {
            //update PMT to `Released`
            pmtUpdateStatus = updatePmtLcState(patchId, version, releasedState);
            developerMessage = patchValidateStatus + pmtUpdateStatus;
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, releasedState, developerMessage,
                    SUCCESS_MESSAGE);

        } else {
            //update PMT to `Testing`
            pmtUpdateStatus = updatePmtLcState(patchId, version, testingState);
            developerMessage = patchValidateStatus + pmtUpdateStatus;
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState, developerMessage,
                    FAILURE_MESSAGE);
            LOG.error(developerMessage + " patch:" + patchName);
        }
        return pmtUpdateStatus;
    }

    /**
     * Update PMT state to testing for the validation failed patch.
     * Update error log database.
     *
     * @param patchId          4 digit patch id
     * @param version          carbon version
     * @param developerMessage message sent to developer about patch signing status
     * @param patchName        full patch name
     * @return PMT update status message
     */
    private static String getActionFailedPatch(String patchId, String version,
                                               String developerMessage, String patchName) {

        String pmtUpdateStatus = updatePmtLcState(patchId, version, testingState);
        developerMessage += pmtUpdateStatus;
        patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState, developerMessage, FAILURE_MESSAGE);
        LOG.error(developerMessage + " patch:" + patchName);
        return pmtUpdateStatus;
    }

    /**
     * Commit keys and update PMT for validated update.
     * Update error log database.
     *
     * @param patchId              4 digit patch id
     * @param version              carbon version
     * @param updateValidateStatus update validation status message
     * @param objUpdateValidator   UpdateValidator object
     * @param patchName            full patch name
     * @return PMT update status message
     */
    private static String getActionPassedUpdate(String patchId, String version, String updateValidateStatus,
                                                UpdateValidator objUpdateValidator, String patchName) {

        String pmtUpdateStatus;
        String developerMessage;

        updateValidateStatus += " " + SvnClient.commitKeys(
                (objUpdateValidator.updateUrl.split(commitUrlSplitter)[1]),
                objUpdateValidator.updateDestination, patchId, version, prop.getProperty("update"));

        //if successful, update PMT to UAT Staging state
        if (updateValidateStatus.contains(Constants.UPDATE_VALIDATED) &&
                updateValidateStatus.contains(Constants.SUCCESSFULLY_KEY_COMMITTED)) {
            //update PMT to `UATStaging`
            pmtUpdateStatus = updatePmtLcState(patchId, version, uatStgState);
            developerMessage = updateValidateStatus.replace(WUM_UC_SUCCESS_MESSAGE, "")
                    + pmtUpdateStatus;
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, uatStgState, developerMessage, SUCCESS_MESSAGE);
        } else {
            //update PMT to `Testing`
            pmtUpdateStatus = updatePmtLcState(patchId, version, testingState);
            developerMessage = updateValidateStatus
                    .replace(WUM_UC_ERROR_MESSAGE, "")
                    .replace(WUM_UC_SUCCESS_MESSAGE, "")
                    + pmtUpdateStatus;
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState, developerMessage, FAILURE_MESSAGE);
            LOG.error(developerMessage);
        }
        return pmtUpdateStatus;
    }

    /**
     * Update PMT state to testing for the validation failed update.
     * Update error log database.
     *
     * @param patchId          4 digit patch id
     * @param version          carbon version
     * @param developerMessage patch status message for the developer
     * @param patchName        full patch name
     * @return PMT update status message
     */
    private static String getActionFailedUpdate(String patchId, String version, String developerMessage,
                                                String patchName) {

        String pmtUpdateStatus = updatePmtLcState(patchId, version, testingState);
        developerMessage = developerMessage.replace(WUM_UC_ERROR_MESSAGE, "") + pmtUpdateStatus;
        patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState, developerMessage, FAILURE_MESSAGE);
        LOG.error(developerMessage + " patch:" + patchName);
        return pmtUpdateStatus;
    }

    /**
     * Commit keys and update PMT for the validated patch and update.
     * Update error log database.
     *
     * @param patchId              4 digit patch id
     * @param version              carbon version
     * @param patchValidateStatus  patch validate status message
     * @param updateValidateStatus update validate status message
     * @param objPatchValidator    PatchValidator object
     * @param objUpdateValidator   UpdateValidator object
     * @param patchName            full patch name
     * @return PMT update status message
     */
    private static String getActionPassedPatchAndUpdate(String patchId, String version, String patchValidateStatus,
                                                        String updateValidateStatus, PatchValidator objPatchValidator,
                                                        UpdateValidator objUpdateValidator, String patchName) {

        String developerMessage;
        String pmtUpdateStatus;

        patchValidateStatus = patchValidateStatus + SvnClient.commitKeys(
                (objPatchValidator.patchUrl.split(commitUrlSplitter)[1]), objPatchValidator.patchDestination,
                patchId, version, prop.getProperty("patch"));
        updateValidateStatus = updateValidateStatus + SvnClient.commitKeys(
                (objUpdateValidator.updateUrl.split(commitUrlSplitter)[1]), objUpdateValidator.updateDestination,
                patchId, version, prop.getProperty("update"));

        //if successful, update PMT to Signed
        if (updateValidateStatus.contains(Constants.UPDATE_VALIDATED) &&
                updateValidateStatus.contains(Constants.SUCCESSFULLY_KEY_COMMITTED)) {
            //update PMT to `UATStaging`
            pmtUpdateStatus = updatePmtLcState(patchId, version, uatStgState);
            developerMessage = patchValidateStatus +
                    updateValidateStatus.replace(WUM_UC_SUCCESS_MESSAGE, "") +
                    pmtUpdateStatus;
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, uatStgState, developerMessage,
                    SUCCESS_MESSAGE);
        } else {
            //update PMT to `Testing`
            pmtUpdateStatus = updatePmtLcState(patchId, version, testingState);
            developerMessage = patchValidateStatus +
                    updateValidateStatus.replace(WUM_UC_ERROR_MESSAGE, "") +
                    pmtUpdateStatus;
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState, developerMessage,
                    FAILURE_MESSAGE);
            LOG.error(developerMessage + " patch:" + patchName);
        }
        return pmtUpdateStatus;
    }

    /**
     * Update PMT state to testing for the validation failed patch and update.
     * Update error log database.
     *
     * @param patchId                4 digit patch id
     * @param version                carbon version
     * @param patchValidationStatus  is patch validated
     * @param updateValidationStatus is update validated
     * @param patchValidateStatus    patch validation status message
     * @param updateValidateStatus   update validation status message
     * @param patchName              full patch name
     * @param developerMessage       patch validation status message for developer
     * @return PMT update status message
     */
    private static String getActionFailedPatchAndUpdate(String patchId, String version, boolean patchValidationStatus,
                                                        boolean updateValidationStatus, String patchValidateStatus,
                                                        String updateValidateStatus, String patchName,
                                                        String developerMessage) {

        //update PMT state to `Testing`
        String pmtUpdateStatus = updatePmtLcState(patchId, version, testingState);

        if (patchValidationStatus) { //patch validation successful
            developerMessage = patchValidateStatus +
                    developerMessage.replace(WUM_UC_ERROR_MESSAGE, "") +
                    pmtUpdateStatus;
        } else if (updateValidationStatus) { //update validation successful
            developerMessage = developerMessage +
                    updateValidateStatus.replace(WUM_UC_SUCCESS_MESSAGE, "") +
                    pmtUpdateStatus;
        } else {
            developerMessage = developerMessage + pmtUpdateStatus;
        }
        patchRequestDatabaseHandler.insertDataToErrorLog(patchName, testingState, developerMessage,
                FAILURE_MESSAGE);
        LOG.error(developerMessage + " patch:" + patchName);
        return pmtUpdateStatus;
    }

    /**
     * Delete downloaded patch folder from server
     */
    private static void deleteDownloadedPatchFilesFromServer() {

        String destFilePath = prop.getProperty("destFilePath");
        try {
            FileUtils.deleteDirectory(new File(destFilePath));
        } catch (IOException ex) {
            throw new ServiceException("IO Exception occurred when deleting files from directory, destFilePath:" +
                    destFilePath, ex);
        }
    }
}
