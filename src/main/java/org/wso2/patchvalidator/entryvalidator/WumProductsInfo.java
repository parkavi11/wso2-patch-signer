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

package org.wso2.patchvalidator.entryvalidator;

import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.ArrayList;
import java.util.Base64;
import org.wso2.patchvalidator.exceptions.ServiceException;

/**
 * Create object for Wum products info from products json of patch info (base64 string).
 */
class WumProductsInfo {

    private List<WumProduct> compatibleProducts;
    private List<WumProduct> partiallyApplicableProducts;

    static WumProductsInfo createWumProductsInfoObjectFromBase64(String base64Str) {
        try {
            //decode base64 string
            String decodedString = getDecodedString(base64Str);
            //convert decoded string to JSON object
            JSONObject convertedJson = getJsonFromString(decodedString);
            //create WumProductsInfo class object
            return createWumProductsInfoObject(convertedJson);
        } catch (Exception ex){
            throw new ServiceException("Exception occurred when creating WumProductsInfo from base64 string," +
                    "base64string:" + base64Str, "Encoded products json cannot converted to a object, " +
                    "Please contact admin.", ex);
        }
    }

    private static String getDecodedString(String base64String) {
        // Getting decoder
        Base64.Decoder decoder = Base64.getDecoder();
        // Decoding string
        return new String(decoder.decode(base64String));
    }

    //convert decoded string in to json
    private static JSONObject getJsonFromString(String decodedString) {
        JSONParser parser = new JSONParser();
        try {
            return (JSONObject) parser.parse(decodedString);
        } catch (ParseException ex) {
            throw new ServiceException("Exception occurred when parsing decoded string to json, decodedString:"
                    + decodedString,"Decoded string cannot convert in to JSON, Please contact admin.", ex);
        }
    }

    //create WumProductsInfo object from converted json
    private static WumProductsInfo createWumProductsInfoObject(JSONObject convertedJson) {
        try {
            WumProductsInfo productJson = new WumProductsInfo();
            //create array lists for products
            productJson.setCompatibleProducts(createProductArrayList(convertedJson, "compatible-products"));
            productJson.setPartiallyApplicableProducts
                    (createProductArrayList(convertedJson, "partially-applicable-products"));
            return productJson;
        } catch (ServiceException ex){
            throw new ServiceException("Exception occurred when creating WumProductsInfo, convertedJson:"
                    + convertedJson, ex.getDeveloperMessage(), ex);
        }
    }

    //create Wum Product for each product list in the json
    private static ArrayList<WumProduct> createProductArrayList(JSONObject obj, String key) {
        try {
            ArrayList<WumProduct> list = new ArrayList<>();
            JSONArray arr = (JSONArray) obj.get(key);
            if (arr != null) {
                for (Object anArr : arr) {
                    list.add(createWumProduct((JSONObject) anArr));
                }
            }
            return list;
        } catch (ServiceException ex){
            throw new ServiceException("Exception occurred when creating WumProduct for each product list json, " +
                    "listType:" + key, ex.getDeveloperMessage(), ex);
        }
    }

    //create WumProduct class object
    private static WumProduct createWumProduct(JSONObject jsonObj) {
        try {
            WumProduct product = new WumProduct();
            product.setProductName((String) jsonObj.get("product-name"));
            product.setBaseVersion((String) jsonObj.get("base-version"));
            product.setAddedFiles(createArrayListByFileType(jsonObj, "added-files"));
            product.setModifiedFiles(createArrayListByFileType(jsonObj, "modified-files"));
            product.setRemovedFiles(createArrayListByFileType(jsonObj, "removed-files"));
            String productAbbr = product.getProductName().replace("wso2", "").trim() + "-" +
                    product.getBaseVersion().trim();
            product.setProductAbbreviation(productAbbr);
            return product;
        } catch (ServiceException ex){
            throw new ServiceException("Service Exception occurred when creating the WumProduct object",
                    ex.getDeveloperMessage(), ex);
        } catch (Exception ex){
            throw new ServiceException("Exception occurred when creating the WumProduct object",
                    "Exception occurred when creating WumProduct object, Please contact admin.", ex);
        }
    }

    //create array list for given file type from json object
    private static List<String> createArrayListByFileType(JSONObject obj, String fileType) {
        try {
            List<String> list = new ArrayList<>();
            JSONArray arr = (JSONArray) obj.get(fileType);
            if (arr != null) {
                for (Object anArr : arr) {
                    list.add((String) anArr);
                }
            }
            return list;
        } catch (Exception ex){
            throw new ServiceException("Exception occurred when creating a array list for given file type" +
                    "fileType:" + fileType + " productObject:" + obj,
                    "Exception occurred when creating file array list, Please contact admin.", ex);
        }
    }

    //getters and setters

    List<WumProduct> getCompatibleProducts() {
        return compatibleProducts;
    }

    private void setCompatibleProducts(List<WumProduct> compatibleProducts) {
        this.compatibleProducts = compatibleProducts;
    }

    List<WumProduct> getPartiallyApplicableProducts() {
        return partiallyApplicableProducts;
    }

    private void setPartiallyApplicableProducts(List<WumProduct> partiallyApplicableProducts) {
        this.partiallyApplicableProducts = partiallyApplicableProducts;
    }
}
