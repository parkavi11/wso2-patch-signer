package org.wso2.patchvalidator.client;

import java.io.InputStream;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;

import static org.wso2.patchvalidator.util.Util.convertStreamToString;

/**
 * Client for accessing PMT API.
 */
public class PmtClient {


    private static Properties prop = PropertyLoader.getInstance().prop;
    private static Logger LOG = LogBuilder.getInstance().LOG;

    //get patch information from PMT API
    public static JSONObject getPatchInfo(String version, String patchId) {

        JSONParser parser = new JSONParser();
        JSONObject jsonObj = new JSONObject();
        HttpClient httpClient;
        String httpUri = prop.getProperty("getJsonHttpUri") + version + "-" + patchId; // 4.4.0-3097

        try {
            httpClient = HttpClientBuilder.create().build();
            HttpGet request = new HttpGet(httpUri);
            request.addHeader("Authorization", prop.getProperty("bearerAuthorization"));

            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                InputStream inStream = response.getEntity().getContent();
                String result = convertStreamToString(inStream);
                JSONArray jsonArr = (JSONArray) parser.parse(result);
                jsonObj.put("pmtResult", jsonArr);
                return jsonObj;
            } else {
                throw new ServiceException("Error occurred when retrieving PMT patch Json, version:" + version +
                        " patchId:" + patchId + " url: " + httpUri + ", statusCode:" + statusCode,
                        "Cannot retrieve patch information from PMT for the patch \"" + version + "-"
                                + patchId + "\" , Please contact admin.");
            }
        } catch (Exception ex) {
            throw new ServiceException("Exception occurred when retrieving PMT patch Json, version:" + version +
                    " patchId:" + patchId + " url: " + httpUri,
                    "Cannot retrieve patch information from PMT for the patch \"" + version + "-"
                            + patchId + "\" , Please contact admin.", ex);
        }
    }

    //update LC state in PMT using PMT API
    public static String updatePmtLcState(String patchId, String version, String lifeCycleState) {

        String orgPatch = prop.getProperty("orgPatch");
        String patchName = orgPatch + version + "-" + patchId;

        //send request
        JSONObject json = new JSONObject();
        json.put("patchName", patchName);
        json.put("lifeCycleState", lifeCycleState);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpPost request = new HttpPost(prop.getProperty("httpUri"));
            StringEntity params = new StringEntity(json.toString());

            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", prop.getProperty("authHeader"));
            request.setEntity(params);

            HttpResponse response = httpClient.execute(request);
            String responseAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                if (lifeCycleState.equals(prop.getProperty("pmtLcAdminStgState"))) {
                    return Constants.PMT_UPDATE_ADMIN_STG_SUCCESSFUL;
                } else if (lifeCycleState.equals(prop.getProperty("pmtLcUATStgState"))) {
                    return Constants.PMT_UPDATE_UAT_STG_SUCCESSFUL;
                } else if (lifeCycleState.equals(prop.getProperty("pmtLcTestingState"))) {
                    return Constants.PMT_UPDATE_TESTING_SUCCESSFUL;
                } else if (lifeCycleState.equals(prop.getProperty("pmtLcReleasedState"))) {
                    return Constants.PMT_UPDATE_RELEASED_SUCCESSFUL;
                } else if (lifeCycleState.equals(prop.getProperty("pmtLcReleasedNotAutomatedState"))) {
                    return Constants.PMT_UPDATE_RELEASED_NOT_AUTOMATED_SUCCESSFUL;
                } else if (lifeCycleState.equals(prop.getProperty("pmtLcReleasedNotInPublicSvnState"))) {
                    return Constants.PMT_UPDATE_RELEASED_NOT_IN_PUBLIC_SVN_SUCCESSFUL;
                } else {
                    LOG.error("PMT update failed, patchName:" + patchName + " lifeCycleState:" + lifeCycleState +
                            " statusCode:" + statusCode + " response:" + responseAsString);
                    return Constants.PMT_UPDATE_FAIL;
                }
            } else {
                LOG.error("PMT update failed, patchName:" + patchName + " lifeCycleState:" + lifeCycleState +
                        " statusCode:" + statusCode + " request:" + request + " json:" + json + " response:" +
                        responseAsString);
                return Constants.PMT_UPDATE_FAIL;
            }
        } catch (Exception ex) {
            throw new ServiceException("Exception occurred, when updating PMT LC state, patchName:" + patchName +
                    " lifeCycleState:" + lifeCycleState, ex);
        }

    }

    //revert PMT LC state in PMT using PMT API
    public static boolean revertPmtLcState(String patchId, String version) {

        String orgPatch = prop.getProperty("orgPatch");
        String patchName = orgPatch + version + "-" + patchId;

        JSONObject json = new JSONObject();
        json.put("patchName", patchName);
        json.put("lifeCycleState", prop.getProperty("pmtLcRevertedState"));

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpPost request = new HttpPost(prop.getProperty("httpUri"));
            StringEntity params = new StringEntity(json.toString());

            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", prop.getProperty("authHeader"));
            request.setEntity(params);

            HttpResponse response = httpClient.execute(request);
            String responseAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return responseAsString.contains("Successfully moved to Testing state");
            } else {
                throw new ServiceException("pmt updating to Reverted state failed, " +
                        "patchName:" + patchName + " lifeCycleState:" + prop.getProperty("pmtLcRevertedState") +
                        " statusCode:" + statusCode + " response:" + responseAsString,
                        "Reverting PMT life cycle state failed for the patch \"" + patchName + "\"," +
                                "Please contact admin.");
            }
        } catch (Exception ex) {
            throw new ServiceException("Exception occurred, when updating PMT LC state, patchName:" + patchName +
                    " lifeCycleState:" + prop.getProperty("pmtLcRevertedState"),
                    "Reverting PMT life cycle state failed for the patch \"" + patchName + "\"," +
                            "Please contact admin.", ex);
        }
    }

    //change PMT LC state to Released state using PMT API
    public static boolean updatePmtStateAfterBuild(String patchName, String state) {

        JSONObject json = new JSONObject();
        String releasedState = getReleasedState(state);
        json.put("patchName", patchName);
        json.put("lifeCycleState", releasedState);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpPost request = new HttpPost(prop.getProperty("httpUri"));
            StringEntity params = new StringEntity(json.toString());

            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", prop.getProperty("authHeader"));
            request.setEntity(params);

            HttpResponse response = httpClient.execute(request);
            String responseAsString = EntityUtils.toString(response.getEntity());

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                String statusMessage = "Successfully moved to " + releasedState + " state";
                return responseAsString.contains(statusMessage);
            } else {
                throw new ServiceException("PMT updating to " + releasedState + " state failed, " +
                        "patchName:" + patchName + " lifeCycleState:" + prop.getProperty("pmtLcRevertedState") +
                        " statusCode:" + statusCode + " response:" + responseAsString,
                        "Updating PMT life cycle to \"" + releasedState + "\" state failed for the patch \""
                                + patchName + "\"," + "Please contact admin.");
            }
        } catch (Exception ex) {
            throw new ServiceException("Exception occurred, when updating PMT LC state, patchName:" + patchName +
                    " lifeCycleState:" + state,
                    "Updating PMT life cycle to \"" + releasedState + "\" state failed for the patch \""
                            + patchName + "\"," + "Please contact admin.", ex);
        }
    }

    private static String getReleasedState(String state){

        if(state.trim().equals(prop.getProperty("pmtLcReleasedState")))
            return prop.getProperty("pmtLcReleasedState");
        else if(state.trim().equals(prop.getProperty("pmtLcReleasedNotAutomatedState")))
            return prop.getProperty("pmtLcReleasedNotAutomatedState");
        else if(state.trim().equals(prop.getProperty("pmtLcReleasedNotInPublicSvnState")))
            return prop.getProperty("pmtLcReleasedNotInPublicSvnState");
        else if(state.trim().equals(prop.getProperty("pmtLcUatFailed")))
            return prop.getProperty("pmtLcUatFailed");
        else
            throw new ServiceException("Invalid released state, state:" + state,
                    "Invalid released state, Please contact admin.");
    }

}
