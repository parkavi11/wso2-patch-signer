package org.wso2.patchvalidator.util;

import java.io.*;

import org.json.simple.JSONArray;
import org.wso2.patchvalidator.exceptions.ServiceException;
import java.util.List;
import java.util.ArrayList;


/**
 * Common helper methods for the service.
 */
public class Util {

    /**
     * Convert http stream to string.
     *
     * @param is input stream
     * @return input stream string
     */
    public static String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException ex) {
            throw new ServiceException("Exception occurred when converting http stream to string",
                    "IO exception occurred when converting http stream, Please contact admin.", ex);
        } finally {
            try {
                is.close();
            } catch (IOException ex) {
                throw new ServiceException("Exception occurred when closing the http input stream",
                        "IO exception occurred when closing http input stream, Please contact admin.", ex);
            }
        }
        return sb.toString();
    }

    /**
     * Create list from json array.
     *
     * @param arr json array
     * @return list of strings
     */
    public static List<String> createListFromJsonArray(JSONArray arr) {

        List<String> list = new ArrayList<>();
        if (arr != null) {
            for (Object anArr : arr) {
                list.add((String) anArr);
            }
        }
        return list;
    }
}
