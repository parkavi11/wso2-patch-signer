/*
 *  Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
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
package org.wso2.patchvalidator.store;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.interfaces.CommonDatabaseHandler;
import org.wso2.patchvalidator.service.SyncService;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Read data from PMT database about product details.
 */
public class PatchRequestDatabaseHandler implements CommonDatabaseHandler {

    private static final Logger LOG = LoggerFactory.getLogger(PatchRequestDatabaseHandler.class);
    private Properties prop = new Properties();

    {
        try {
            prop.load(SyncService.class.getClassLoader().getResourceAsStream("application.properties"));
        } catch (IOException e) {
            LOG.error("Error occurs while getting properties");
        }
    }

    private Connection getDBConnection() {
        Connection dbConnection = null;
        try {
            String dbURL = prop.getProperty("dbURL");
            String dbUser = prop.getProperty("dbUser");
            String dbPassword = prop.getProperty("dbPassword");
            dbConnection = DriverManager.getConnection(dbURL, dbUser, dbPassword);
        } catch (SQLException e) {
            LOG.error("Database connection failure.", e);
        }
        return dbConnection;
    }

    @Override
    public int getProductType(String product) {

        try (Connection connectDB = getDBConnection(); Statement create = connectDB.createStatement()) {
            String productTypeChooser =
                    "SELECT TYPE FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS WHERE " + "PRODUCT_ABBREVIATION='"
                            + product + "'";
            ResultSet result = create.executeQuery(productTypeChooser);

            int type = 0;
            while (result.next()) {
                type = result.getInt("TYPE");
            }
            return type;
        } catch (SQLException ex) {
            throw new ServiceException("SQL exception occurred when retrieving product type from product " +
                    "abbreviation, productAbbreviation:" + product,
                    "Cannot retrieve corresponding patch-type for the product \"" + product + "\" from database. " +
                            "Please contact admin to insert it.", ex);
        }
    }

    @Override
    public String getProductAbbreviation(String productName, String productVersion) throws SQLException {
        try (Connection connectDB = getDBConnection(); Statement create = connectDB.createStatement()) {
            String productChooser = "SELECT PRODUCT_ABBREVIATION FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS "
                    + "WHERE PRODUCT_NAME='" + productName + "' AND PRODUCT_VERSION='" + productVersion + "'";
            ResultSet result = create.executeQuery(productChooser);

            while (result.next()) {
                productName = result.getString("PRODUCT_ABBREVIATION");
            }
            return productName;
        }
    }

    @Override
    public String getProductURL(String productAbbreviation) throws SQLException {

        try (Connection connectDB = getDBConnection(); Statement create = connectDB.createStatement()) {
            String productUrl = null;
            String productChooser = "SELECT PRODUCT_URL FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS "
                    + "WHERE PRODUCT_ABBREVIATION='" + productAbbreviation + "'";
            ResultSet result = create.executeQuery(productChooser);

            while (result.next()) {
                productUrl = result.getString("PRODUCT_URL");
            }
            return productUrl;
        }
    }

    @Override
    public JsonArray getProductList() {

        JsonArray productList = new JsonArray();
        try (Connection connectDB = getDBConnection(); Statement create = connectDB.createStatement()) {
            String productTypeChooser = "SELECT PRODUCT_NAME , PRODUCT_VERSION FROM " +
                    "WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS ORDER BY PRODUCT_NAME ASC";
            ResultSet result = create.executeQuery(productTypeChooser);
            String productName;
            String productVersion;
            String fullProductName;

            while (result.next()) {
                productName = result.getString("PRODUCT_NAME");
                productVersion = result.getString("PRODUCT_VERSION");

                fullProductName = productName + " " + productVersion;
                JsonObject productDetail = new JsonObject();
                productDetail.addProperty("value", fullProductName);
                productDetail.addProperty("label", fullProductName);
                productList.add(productDetail);
            }
        } catch (SQLException e) {
            LOG.error(e.getSQLState());
            JsonObject productDetail = new JsonObject();
            productDetail.addProperty("value", "No Data");
            productDetail.addProperty("label", "No Data");
            productList.add(productDetail);
            return productList;
        } catch (NullPointerException e) {
            LOG.error("SQL connection failed");
            JsonObject productDetail = new JsonObject();
            productDetail.addProperty("value", "No Data");
            productDetail.addProperty("label", "No Data");
            productList.add(productDetail);
            return productList;
        }
        return productList;
    }

    @Override
    public void insertDataToTrackDatabase(String patchId, String version, int state, int type, String product,
                                          String developedBy,
                                          String status) throws SQLException {

        try (Connection connectDB = getDBConnection(); Statement create = connectDB.createStatement()) {

            version = getCarbonVersion(version);
            String patchType = getPatchType(type);

            String processStatus =
                    "SELECT * FROM WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS " + "WHERE STATUS='"
                            + Constants.PROCESSING + "'";
            ResultSet inProcess = create.executeQuery(processStatus);

            if (inProcess.next()) {
                String postParametersInserter =
                        "INSERT INTO WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS "
                                + "(PATCH_ID,VERSION,STATE,TYPE," + "PRODUCT,DEVELOPED_BY,STATUS) VALUES ('" + patchId
                                + "','" + version + "','" + state + "','" + patchType + "','" + product + "','"
                                + developedBy + "','" + status + "')";
                PreparedStatement proceed = connectDB
                        .prepareStatement(postParametersInserter, Statement.RETURN_GENERATED_KEYS);
                proceed.executeUpdate();
                updatePostRequestStatus(product, patchId, Constants.QUEUE);
            } else {
                String postParametersInserter = "INSERT INTO "
                        + "WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS(PATCH_ID,VERSION,STATE,TYPE,"
                        + "PRODUCT,DEVELOPED_BY,STATUS) VALUES ('" + patchId + "','" + version + "','" + state + "','"
                        + patchType + "','" + product + "','" + developedBy + "','" + status + "')";
                PreparedStatement proceed = connectDB
                        .prepareStatement(postParametersInserter, Statement.RETURN_GENERATED_KEYS);
                proceed.executeUpdate();
                updatePostRequestStatus(product, patchId, Constants.PROCESSING);
            }
        }
    }

    public void insertDataToErrorLog(String patchName, String state, String message, String messageType) {

        try (Connection connectDB = getDBConnection()) {
            message = message.replace("'", "\\'");
            String postParametersInserter = "INSERT INTO WSO2_PATCH_VALIDATION_DATABASE.PATCH_ERROR_LOG " +
                    "(PATCH_NAME,LC_STATE,MESSAGE,MESSAGE_TYPE) VALUES ('" + patchName + "','" + state + "','" + message +
                    "','" + messageType + "')";
            PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                    Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
        } catch (SQLException ex) {
            throw new ServiceException("SQL Exception occurred when trying to insert error message to the database," +
                    " patchName:" + patchName + " state:" + state + " message:" + message + " messageType:" +
                    messageType);
        }
    }

    //add product to track database
    @Override
    public void insertProductToTrackDatabase(String productName, String productVersion, String carbonVersion,
                                             String kernelVersion, String productAbbreviation,
                                             int wumSupported, int type, String productUrl) {

        try (Connection connectDB = getDBConnection()) {
            carbonVersion = getCarbonVersion(carbonVersion);
            //String patchType = getPatchType(type);
            String postParametersInserter = "INSERT INTO " +
                    "WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS(PRODUCT_NAME,PRODUCT_VERSION,CARBON_VERSION," +
                    "KERNEL_VERSION,PRODUCT_ABBREVIATION,WUM_SUPPORTED,TYPE,PRODUCT_URL) VALUES ('" + productName + "','" +
                    productVersion + "','" + carbonVersion + "','" + kernelVersion + "','" + productAbbreviation
                    + "','" + wumSupported + "','" + type + "','" + productUrl + "')";
            PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                    Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
        } catch (SQLException ex) {
            throw new ServiceException("SQL Exception occurred when inserting product in to the " +
                    "WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS table", "Adding product to database failed" +
                    ", Please contact admin", ex);
        }
    }

    //get product details by abbreviation
    @Override
    public Map<String, String> getProductDetails(String abbreviation) throws SQLException {

        try (Connection connectDB = getDBConnection()) {
            Map<String, String> map = new HashMap<>();
            Statement create = connectDB.createStatement();
            String productDetailsChooser =
                    "SELECT * FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS WHERE " + "PRODUCT_ABBREVIATION='"
                            + abbreviation + "'";
            ResultSet result = create.executeQuery(productDetailsChooser);

            while (result.next()) {
                map.put("productName", result.getString("PRODUCT_NAME"));
                map.put("productVersion", result.getString("PRODUCT_VERSION"));
            }
            return map;
        }
    }

    //get products for each carbon kernel version
    public ArrayList<String> getProductsByKernalVersion(String productKernal) throws SQLException {

        try (Connection connectDB = getDBConnection()) {
            Statement create = connectDB.createStatement();
            ArrayList<String> productName = new ArrayList<>();
            String productChooser = "SELECT PRODUCT_ABBREVIATION FROM WSO2_PATCH_VALIDATION_DATABASE.PRODUCT_DETAILS "
                    + "WHERE KERNEL_VERSION='" + productKernal + "'";
            ResultSet result = create.executeQuery(productChooser);

            while (result.next()) {
                productName.add(result.getString("PRODUCT_ABBREVIATION"));
            }
            return productName;
        }
    }

    public void updatePostRequestStatus(String product, String patchId, String status) throws SQLException {

        try (Connection connectDB = getDBConnection()) {
            String changeStatus =
                    "UPDATE WSO2_PATCH_VALIDATION_DATABASE.TRACK_PATCH_VALIDATE_RESULTS SET status='" + status
                            + "' WHERE PRODUCT='" + product + "' && PATCH_ID='" + patchId + "'";
            PreparedStatement proceed = connectDB.prepareStatement(changeStatus, Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
        }

    }


    /**
     * Get timestamp for a given jar from JAR_TIMESTAMP table.
     */
    public String getJarTimestamp(String jarName) {

        try (Connection connectDB = getDBConnection()) {
            Statement create = connectDB.createStatement();
            String jarTimestampChooser = "SELECT BUILD_TIMESTAMP FROM WSO2_PATCH_VALIDATION_DATABASE.JAR_TIMESTAMP " +
                    "WHERE FILE_NAME='" + jarName + "'";
            ResultSet result = create.executeQuery(jarTimestampChooser);

            String timestamp = "";
            while (result.next()) {
                timestamp = result.getString("BUILD_TIMESTAMP");
            }
            return timestamp;
        } catch (SQLException ex) {
            throw new ServiceException("SQL exception occurred when retrieving timestamp for the jar \"" + jarName +
                    "\"", "Cannot retrieve timestamp for the jar \"" + jarName + "\" from database, " +
                    "Please contact admin.", ex);
        }
    }


    /**
     * Insert timestamp of a `new jar file` to the TEMP_JAR_TIMESTAMP table.
     */
    public void insertJarTimestamp(String jarName, String timestamp) {

        try (Connection connectDB = getDBConnection()) {
            String postParametersInserter = "INSERT INTO WSO2_PATCH_VALIDATION_DATABASE.JAR_TIMESTAMP " +
                    "(FILE_NAME,BUILD_TIMESTAMP) VALUES ('" + jarName + "','" + timestamp + "')";
            PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                    Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
        } catch (SQLException ex) {
            throw new ServiceException("SQL exception occurred when inserting timestamp for the jar \"" + jarName +
                    "\" to the Master table", "Cannot insert timestamp for the jar \"" + jarName + "\" to the " +
                    "Master table, Please contact admin.", ex);
        }
    }


    /**
     * Insert updated timestamp of a jar file to the TEMP_JAR_TIMESTAMP table.
     * If there is a row for same jar file in the table, timestamp will be updated to latest one.
     */
    public void insertTempJarTimestamp(String jarName, String jarTimestamp, String updateId) {

        try (Connection connectDB = getDBConnection()) {
            String postParametersInserter = "INSERT INTO WSO2_PATCH_VALIDATION_DATABASE.TEMP_JAR_TIMESTAMP " +
                    "(FILE_NAME,BUILD_TIMESTAMP,UPDATE_ID) VALUES ('" + jarName + "','" + jarTimestamp + "'," +
                    "'" + updateId + "')";
            PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                    Statement.RETURN_GENERATED_KEYS);
            proceed.executeUpdate();
        } catch (SQLException ex) {
            throw new ServiceException("SQL exception occurred when inserting jarTimestamp for the jar \"" + jarName +
                    "\" to the Temp table", "Cannot insert jarTimestamp for the jar \"" + jarName + "\" to the " +
                    "Temp table, Please contact admin.", ex);
        }
    }


    /**
     * Clear TEMP_JAR_TIMESTAMP table.
     * Update each jar in the JAR_TIMESTAMP with latest timestamp in TEMP_JAR_TIMESTAMP.
     * Invoked if WUM build process completed successfully.
     */
    public void clearTempJarTimestampTable() {

        try (Connection connectDB = getDBConnection()) {
            Statement create = connectDB.createStatement();
            String query = "SELECT * FROM WSO2_PATCH_VALIDATION_DATABASE.TEMP_JAR_TIMESTAMP " +
                    "ORDER BY FILE_NAME,BUILD_TIMESTAMP";
            ResultSet resultSet = create.executeQuery(query);

            while (resultSet.next()) {
                String jarName = resultSet.getString("FILE_NAME");
                String timestamp = resultSet.getString("BUILD_TIMESTAMP");

                //update master table from temp table
                String postParametersInserter = "UPDATE WSO2_PATCH_VALIDATION_DATABASE.JAR_TIMESTAMP SET " +
                        "BUILD_TIMESTAMP='" + timestamp + "' WHERE FILE_NAME='" + jarName + "'";
                PreparedStatement proceed = connectDB.prepareStatement(postParametersInserter,
                        Statement.RETURN_GENERATED_KEYS);
                int updated = proceed.executeUpdate();

                if (updated == 1) {

                    //delete updated jar from Temp
                    String postParamDeleter = "DELETE FROM WSO2_PATCH_VALIDATION_DATABASE.TEMP_JAR_TIMESTAMP WHERE" +
                            " FILE_NAME='" + jarName + "' AND BUILD_TIMESTAMP='" + timestamp + "'";
                    PreparedStatement proceedDelete = connectDB.prepareStatement(postParamDeleter,
                            Statement.RETURN_GENERATED_KEYS);
                    int deleted = proceedDelete.executeUpdate();

                    if (deleted != 1) {
                        throw new ServiceException("Delete JAR_TIMESTAMP Temp row failed, deleted:" + deleted +
                                " jarName:" + jarName + " timestamp:" + timestamp,
                                "Delete JAR_TIMESTAMP Temp row failed for the \"" + jarName + "\"," +
                                        " Please contact admin");
                    }
                } else {
                    throw new ServiceException("Update JAR_TIMESTAMP Master from Temp failed, updated:" + updated +
                            " jarName:" + jarName + " timestamp:" + timestamp,
                            "Update JAR_TIMESTAMP Master from Temp failed for the \"" + jarName + "\"," +
                                    " Please contact admin");
                }
            }
        } catch (SQLException ex) {
            throw new ServiceException("SQL Exception occurred when updating Master table from Temp table.",
                    "SQL Exception occurred when updating Master table from Temp table. Please contact admin.", ex);
        }
    }


    /**
     * Delete row from TEMP_JAR_TIMESTAMP when update is reverting.
     *
     * @param updateId reverting update id, format: WSO2-CARBON-UPDATE-4.4.0-2243
     */
    public void deleteJarFromTemp(String updateId) {

        try (Connection connectDB = getDBConnection()) {
            String query = "DELETE FROM WSO2_PATCH_VALIDATION_DATABASE.TEMP_JAR_TIMESTAMP WHERE UPDATE_ID='" +
                    updateId + "'";
            PreparedStatement ps = connectDB.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            int deleted = ps.executeUpdate();
            if (deleted != 1) {
                throw new ServiceException("Deleting jar from TEMP_JAR_TIMESTAMP failed for update \"" + updateId +
                        "\"," + "Deleting jar from TEMP_JAR_TIMESTAMP failed for update \"" + updateId + "\", " +
                        "Please contact admin.");
            }
        } catch (SQLException ex) {
            throw new ServiceException("SQL exception occurred when deleting jar from TEMP_JAR_TIMESTAMP table," +
                    " updateId:" + updateId,
                    "Deleting jar from TEMP_JAR_TIMESTAMP failed for update \"" + updateId + "\", " +
                            "Please contact admin.", ex);
        }
    }

    private String getCarbonVersion(String carbonVersion) {

        LOG.info(carbonVersion);
        switch (carbonVersion) {
            case "wilkes":
                carbonVersion = "4.4.0";
                break;
            case "hamming":
                carbonVersion = "5.2.0";
                break;
            case "turing":
                carbonVersion = "4.2.0";
                break;
            default:
                LOG.info("Error in carbon version: " + carbonVersion);
                break;
        }
        return carbonVersion;
    }

    private String getPatchType(int type) {

        String patchType = null;
        switch (type) {
            case 1:
                patchType = "patch";
                break;
            case 2:
                patchType = "update";
                break;
            case 3:
                patchType = "PatchAndUpdate";
                break;
            default:
                LOG.info("Error in patch type" + type);
        }
        return patchType;
    }
}


