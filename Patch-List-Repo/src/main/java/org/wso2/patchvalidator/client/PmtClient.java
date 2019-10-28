package org.wso2.patchvalidator.client;

import java.io.InputStream;
import java.util.Properties;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.PropertyLoader;

import static org.wso2.patchvalidator.util.Util.convertStreamToString;

/**
 * Client for accessing PMT API.
 */
public class PmtClient {


    private static Properties prop = PropertyLoader.getInstance().prop;

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

}
