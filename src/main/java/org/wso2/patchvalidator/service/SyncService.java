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

package org.wso2.patchvalidator.service;

import java.util.Properties;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.wso2.patchvalidator.client.PmtClient;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.revertor.Reverter;
import org.wso2.patchvalidator.store.PatchRequestDatabaseHandler;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;

import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.wso2.patchvalidator.constants.Constants.FAILURE_MESSAGE;
import static org.wso2.patchvalidator.constants.Constants.SUCCESS_MESSAGE;
import static org.wso2.patchvalidator.service.Signer.sign;

/**
 * All the endpoints in the microservice is defined here.
 */
//@Path("/patch-validator")
@Path("/request")
public class SyncService {

    private static Logger LOG = LogBuilder.getInstance().LOG;
    private static Properties prop = PropertyLoader.getInstance().prop;
    private static boolean signLock = true; //if this is true signing process will be stopped

    /**
     * Main microservice end point.
     * Validate and Sign patches.
     *
     * @return response message
     */
    @POST
    @Path("/sign")
    @Produces(MediaType.TEXT_PLAIN)
    public Response postRequest() {

        StringBuilder returnMessage;
        if (!signLock) {
            returnMessage = sign();
        } else {
            returnMessage = new StringBuilder().append("Cannot sign, UAT build process has started.");
            LOG.info(returnMessage.toString() + " signLock:" + signLock);
        }
        return Response.ok(returnMessage.toString(), MediaType.TEXT_PLAIN)
                .header("Access-Control-Allow-Credentials", true)
                .build();
    }

    /**
     * Microservice end point.
     * Reverting signed patches.
     *
     * @param patchName full patch name eg:WSO2-CARBON-PATCH-4.4.0-1001
     * @return response message
     */
    @POST
    @Path("/revert")
    @Produces(MediaType.TEXT_PLAIN)
    public Response revertPatch(@FormParam("patch") String patchName) {

        PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
        String version;
        String patchId;
        String message;

        try {
            String[] tempArr = patchName.replace("WSO2-CARBON-PATCH-", "").split("-");
            version = tempArr[0];
            patchId = tempArr[1];
        } catch (Exception ex) {
            message = "Reverting Process Failed. Patch name \" " + patchName + "\" is not in the valid format" +
                    " (Valid format eg:\"WSO2-CARBON-PATCH-4.4.0-1001\").";
            LOG.error("Exception occurred, patch name is not in valid format. patch:" + patchName, ex);
            return Response.ok(message, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }

        try {
            Reverter.patchRevert(version, patchId);
            message = "\"" + patchName + "\" reverted Successfully.";
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, prop.getProperty("pmtLcTestingState"), message,
                    SUCCESS_MESSAGE);
            LOG.info(message);
            return Response.ok(message, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        } catch (ServiceException ex) {
            message = "\"" + patchName + "\" reverting failed, " + ex.getDeveloperMessage();
            patchRequestDatabaseHandler.insertDataToErrorLog(patchName, prop.getProperty("pmtLcAdminStgState"),
                    ex.getDeveloperMessage(), FAILURE_MESSAGE);
            LOG.error("Exception occurred when reverting, patchId:" + patchId + " version:" + version, ex);
            return Response.ok(message, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

    /**
     * Microservice end point.
     * Add new product to the product details table of patch validation database.
     *
     * @param productName         name of the product
     * @param productVersion      version of the product
     * @param carbonVersion       carbon version
     * @param kernelVersion       kernel version
     * @param productAbbreviation abbreviation of the product
     * @param wumSupported        is this product supported by latest wum version
     * @param type                patch type (patch, update, patch and update)
     * @param productUrl          atuwa product url
     * @return response message
     */
    @POST
    @Path("/addProduct")
    @Produces(MediaType.TEXT_PLAIN)
    public Response addProduct(@FormParam("product") String productName,
                               @FormParam("productVersion") String productVersion,
                               @FormParam("carbonVersion") String carbonVersion,
                               @FormParam("kernelVersion") String kernelVersion,
                               @FormParam("productAbbreviation") String productAbbreviation,
                               @FormParam("WUMSupported") Integer wumSupported,
                               @FormParam("type") Integer type,
                               @FormParam("productUrl") String productUrl) {

        try {
            PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
            //insert requests to database
            patchRequestDatabaseHandler.insertProductToTrackDatabase(productName, productVersion, carbonVersion,
                    kernelVersion, productAbbreviation, wumSupported, type, productUrl);
            LOG.error("Product successfully added to the database, productName:" + productName + " productVersion:" +
                    productVersion + " carbonVersion:" + carbonVersion + " kernelVersion:" + kernelVersion +
                    " productAbbreviation:" + productAbbreviation + " wumSupported:" + wumSupported +
                    " type:" + type + " productUrl:" + productUrl);
            return Response.ok("Adding product to DB Finished Successfully\n", MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        } catch (ServiceException ex) {
            LOG.error("Exception occurred when adding product, productName:" + productName + " productVersion:" +
                    productVersion + " carbonVersion:" + carbonVersion + " kernelVersion:" + kernelVersion +
                    " productAbbreviation:" + productAbbreviation + " wumSupported:" + wumSupported +
                    " type:" + type + " productUrl:" + productUrl, ex);
            return Response.ok(ex.getDeveloperMessage(), MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

    /**
     * Microservice end point.
     * Clearing TEMP_JAR_TIMESTAMP table.
     * Updating latest timestamp in JAR_TIMESTAMP table.
     *
     * @return response message
     */
    @POST
    @Path("/clearTemp")
    @Produces(MediaType.TEXT_PLAIN)
    public Response clearTempJarTimestamp() {

        PatchRequestDatabaseHandler patchRequestDatabaseHandler = new PatchRequestDatabaseHandler();
        String message;

        try {
            patchRequestDatabaseHandler.clearTempJarTimestampTable();
            message = "TEMP_JAR_TIMESTAMP table cleared Successfully.";
            LOG.info(message);
            return Response.ok(message, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        } catch (ServiceException ex) {
            message = "Clearing TEMP_JAR_TIMESTAMP table failed, " + ex.getDeveloperMessage();
            LOG.error("Exception occurred when clearing TEMP_JAR_TIMESTAMP table.", ex);
            return Response.ok(message, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

    /**
     * Microservice end point.
     * Stop and start signing process
     *
     * @param lock if lock is true stop signing, if lock is false start signing
     * @return response message
     */
    @POST
    @Path("/lockSigning")
    @Produces(MediaType.TEXT_PLAIN)
    public Response lockSigning(@FormParam("lock") Boolean lock) {

        String message;
        signLock = lock;
        if (signLock) {
            message = "Signing process stopped successfully.";
        } else {
            message = "Signing process started successfully.";
        }
        LOG.info(message);
        return Response.ok(message, MediaType.TEXT_PLAIN)
                .header("Access-Control-Allow-Credentials", true)
                .build();
    }

    /**
     * Microservice end point.
     * Get patch information.
     *
     * @param patchName full patch name eg:WSO2-CARBON-PATCH-4.4.0-1001
     * @return patch info json
     */
    @POST
    @Path("/getPatchInfo")
    @Produces(MediaType.TEXT_PLAIN)
    public Response getPatchInfo(@FormParam("patch") String patchName) {

        String version;
        String patchId;
        String message;

        try {
            String[] tempArr = patchName.replace("WSO2-CARBON-PATCH-", "").split("-");
            version = tempArr[0];
            patchId = tempArr[1];
        } catch (Exception ex) {
            LOG.error("Exception occurred, patch name is not in valid format. patch:" + patchName, ex);
            return Response.ok("Patch name is not valid", MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }

        try {
            JSONObject patchInfo = PmtClient.getPatchInfo(version, patchId);
            LOG.info("Patch info retrieved successfully. patch:" + patchName);
            return Response.ok(patchInfo, MediaType.APPLICATION_JSON)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        } catch (ServiceException ex) {
            message = "\"" + patchName + "\" retrieving patch info failed, " + ex.getDeveloperMessage();
            LOG.error("Exception occurred when reverting, patchId:" + patchId + " version:" + version, ex);
            return Response.ok(message, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

    /**
     * Microservice end point.
     * Update PMT state to Released state.
     *
     * @param patchName full patch name eg:WSO2-CARBON-PATCH-4.4.0-1001
     * @return PMT state change status
     */
    @POST
    @Path("/updatePmtToReleased")
    @Produces(MediaType.TEXT_PLAIN)
    public Response updatePmtToReleased(@FormParam("patch") String patchName,
                                        @FormParam("releasedState") String releasedState) {

        String message;

        try {
            boolean isUpdatedToReleased = PmtClient.updatePmtStateAfterBuild(patchName, releasedState);
            if(isUpdatedToReleased) {
                LOG.info("PMT LC state updated successfully. patch:" + patchName + " releasedState:" + releasedState);
                return Response.ok("Successfully moved to " + releasedState + " state", MediaType.TEXT_PLAIN)
                        .header("Access-Control-Allow-Credentials", true)
                        .build();
            }else{
                LOG.info("Updating PMT LC state failed. patch:" + patchName + " releasedState:" + releasedState);
                return Response.ok("Updating to " + releasedState + " failed", MediaType.TEXT_PLAIN)
                        .header("Access-Control-Allow-Credentials", true)
                        .build();
            }
        } catch (ServiceException ex) {
            message = "\"" + patchName + "\" Updating PMT LC state to \"" + releasedState + "\" failed, " +
                    ex.getDeveloperMessage();
            LOG.error("Exception occurred when updating PMT LC state, patch:" + patchName + " state:" + releasedState,
                    ex);
            return Response.ok(message, MediaType.TEXT_PLAIN)
                    .header("Access-Control-Allow-Credentials", true)
                    .build();
        }
    }

}