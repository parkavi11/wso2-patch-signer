package org.wso2.patchvalidator.client;

import java.io.InputStream;

import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.Util;

/**
 * Client for accessing WUM UAT API.
 * Only used for reverting a signed update.
 */
public class UatClient {


    public static JSONObject getUatAccessToken(String grantType, String username, String password,
                                               String scope, String key, String uri) {

        String httpBody = "grantType=" + grantType + "&username=" + username + "&password=" + password + "&scope=" +
                scope + "&key=" + key + "&uri=" + uri;

        JSONParser parser = new JSONParser();
        JSONObject resultObject;

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpPost request = new HttpPost(uri);

            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");

            StringEntity params = new StringEntity(httpBody);

            request.setEntity(params);

            request.setHeader("Authorization", key);
            request.setHeader("Content-Type", "application/x-www-form-urlencoded");

            HttpResponse response = httpClient.execute(request);

            InputStream inStream = response.getEntity().getContent();
            String result = Util.convertStreamToString(inStream);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                resultObject = (JSONObject) parser.parse(result);
                return resultObject;
            } else {
                throw new ServiceException("retrieving UAT access token failed, " + "statusCode:" + statusCode +
                        " uri:" + uri + " grantType: password grant, authToken:" + key,
                        "Retrieving UAT access token failed, Please contact admin.");
            }
        } catch (Exception ex) {
            throw new ServiceException("Exception occurred, when retrieving access token from UAT, " +
                    " uri:" + uri + " grantType:" + grantType + " username:" + username +
                    " app-key:" + key, "Retrieving UAT access token failed, Please contact admin.", ex);
        }
    }

    public static boolean deleteUatUpdate(String updateId, String uri, String jwtAssertionValue,
                                          String forwardedForValue, String authorizationValue) {

        uri = uri + updateId;

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpDelete request = new HttpDelete(uri);

            request.addHeader("X-JWT-Assertion", jwtAssertionValue);
            request.addHeader("X-Forwarded-For", forwardedForValue);
            request.addHeader("Authorization", authorizationValue);

            HttpResponse response = httpClient.execute(request);
            InputStream inStream = response.getEntity().getContent();
            String result = Util.convertStreamToString(inStream);
            int statusCode = response.getStatusLine().getStatusCode();

            if (statusCode == 200) {
                return true;
            } else {
                throw new ServiceException("delete UAT update failed, " + " response:" + result +
                        "statusCode:" + statusCode + " request:" + request + " updateId:" + updateId +
                        " uri:" + uri + " jwtAssertionValue:" + jwtAssertionValue + " forwardedForValue:" +
                        forwardedForValue + " authorizationValue:" + authorizationValue,
                        "Deleting UAT update failed for the update \"" + updateId + "\", " +
                                "Please contact admin.");
            }
        } catch (Exception ex) {
            throw new ServiceException("Exception occurred, when deleting UAT update, " + " updateId:" + updateId +
                    " uri:" + uri + " jwtAssertionValue:" + jwtAssertionValue + " forwardedForValue:" +
                    forwardedForValue + " authorizationValue:" + authorizationValue,
                    "Deleting UAT update failed for the update \"" + updateId + "\", Please contact admin.",
                    ex);
        }

    }

}
