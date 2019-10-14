package org.wso2.patchvalidator.interfaces;

import com.google.gson.JsonArray;

import java.sql.SQLException;
import java.util.Map;

/**
 * <h1>Common Database Handler</h1>
 * Interface for Database Handling.
 *
 * @author Kosala Herath, Pramodya Mendis
 * @version 1.3
 * @since 2018-12-14
 */

public interface CommonDatabaseHandler {

    int getProductType(String product) throws SQLException;

    JsonArray getProductList() throws SQLException;

    String getProductAbbreviation(String productName, String productVersion) throws SQLException;

    void insertDataToTrackDatabase(String patchId, String version, int state, int type, String product,
                                   String developedBy, String status) throws SQLException;

    void updatePostRequestStatus(String product, String patchId, String status) throws SQLException;

    void insertProductToTrackDatabase(String productName, String productVersion, String carbonVersion,
                                      String kernelVersion, String productAbbreviation,
                                      int wumSupported, int type, String productUrl) throws SQLException;

    Map<String, String> getProductDetails(String abbreviation) throws SQLException;

    String getProductURL(String productAbbreviation) throws SQLException;
}
