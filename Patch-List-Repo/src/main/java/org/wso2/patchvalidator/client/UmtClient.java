package org.wso2.patchvalidator.client;

import java.io.InputStream;
import java.util.Properties;
import java.util.ArrayList;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.HttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.PropertyLoader;


import static org.wso2.patchvalidator.util.Util.convertStreamToString;

/**
 * Client for accessing UMT API.
 */
public class UmtClient {


    private static Properties prop = PropertyLoader.getInstance().prop;

    //read patch list using UMT API from UMT DB
    public static ArrayList<String> getPatchList(String state) {
        JSONParser parser = new JSONParser();
        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {

            HttpPost request = new HttpPost(prop.getProperty("umtUri"));
            String jsonInputString = "{'state':'" + state + "'}";
            StringEntity params = new StringEntity(jsonInputString);

            request.getMethod();
            request.addHeader("Content-Type", "application/json");
            request.addHeader("Authorization", prop.getProperty("authHeader"));
            request.setEntity(params);

            HttpResponse response = httpClient.execute(request);

            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                InputStream inStream = response.getEntity().getContent();
                String result = convertStreamToString(inStream);
                JSONArray jsonArr = (JSONArray) parser.parse(result);
                //LOG.info("Result   " +result);
                ArrayList<String> list = new ArrayList<String>();
                for (int i = 0; i < jsonArr.size(); i++) {
                    JSONObject rootObj = (JSONObject) jsonArr.get(i);
                    String patchName = (String) rootObj.get("patchName");
                    list.add(patchName);
                }
                return list;
            } else {
                throw new ServiceException("Error occurred when retrieving UMT patch List. +" +
                        " url:  httpUri statusCode: " + statusCode + ", Please contact admin.");
            }
        } catch (Exception ex) {
            throw new ServiceException("Error occurred when retrieving UMT patch List, Please contact admin.", ex);
        }
    }

}
