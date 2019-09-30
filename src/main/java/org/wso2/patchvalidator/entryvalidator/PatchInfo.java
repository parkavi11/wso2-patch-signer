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
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.Util;

/**
 * Create patch information object from PMT Patch Json.
 */
public class PatchInfo {

    private List<String> overviewProducts;
    private List<String> overviewCompatibleProducts;
    private List<String> patchInformationJarsInvolved;
    private List<String> peopleInvolvedDevelopedBy;
    private String wumProductsJSON;
    private String patchLifeCycleState;
    private String wumStatus;

    //constructor
    public PatchInfo(JSONArray jsonArray) {

        for (Object aJsonArray : jsonArray) {

            JSONObject element = (JSONObject) aJsonArray;

            if (element.get("name").equals("overview_products")) {
                this.setOverviewProducts(Util.createListFromJsonArray((JSONArray) element.get("value")));
            } else if (element.get("name").equals("wum_productsJSON")) {
                try {
                    this.setWumProductsJSON
                            (Util.createListFromJsonArray((JSONArray) element.get("value")).get(0));
                } catch (Exception ex) {
                    throw new ServiceException("wum_productsJSON property is not valid in the pmt patch json,",
                            "Products json field is not valid. Please amend and re-submit.", ex);
                }
            } else if (element.get("name").equals("overview_compatibleProducts")) {
                this.setOverviewCompatibleProducts(Util.createListFromJsonArray((JSONArray) element.get("value")));
            } else if (element.get("name").equals("patchInformation_jarsInvolved")) {
                this.setPatchInformationJarsInvolved(Util.createListFromJsonArray((JSONArray) element.get("value")));
            } else if (element.get("name").equals("peopleInvolved_developedby")) {
                this.setPeopleInvolvedDevelopedBy(Util.createListFromJsonArray((JSONArray) element.get("value")));
            } else if (element.get("name").equals("registry.lifecycle.PatchLifeCycle.state")) {
                try {
                    this.setPatchLifeCycleState
                            (Util.createListFromJsonArray((JSONArray) element.get("value")).get(0));
                } catch (Exception ex) {
                    throw new ServiceException("registry.lifecycle.PatchLifeCycle.state property is not valid in" +
                            " the pmt patch json, registry.lifecycle.PatchLifeCycle.state:" +
                            Util.createListFromJsonArray((JSONArray) element.get("value")),
                            "Patch life cycle state is not valid. Please amend and re-submit.", ex);
                }
            } else if (element.get("name").equals("registry.lifecycle.Security_PatchLifeCycle.state")) {
                try {
                    this.setPatchLifeCycleState
                            (Util.createListFromJsonArray((JSONArray) element.get("value")).get(0));
                } catch (Exception ex) {
                    throw new ServiceException("registry.lifecycle.Security_PatchLifeCycle.state property is not valid in" +
                            " the pmt patch json, registry.lifecycle.Security_PatchLifeCycle.state:" +
                            Util.createListFromJsonArray((JSONArray) element.get("value")),
                            "Patch life cycle state is not valid. Please amend and re-submit.", ex);
                }
            } else if (element.get("name").equals("wum_status")) {
                try {
                    this.setWumStatus(Util.createListFromJsonArray((JSONArray) element.get("value")).get(0));
                } catch (Exception ex){
                    throw new ServiceException("wum_status is not valid in the pmt patch json, wum_status:" +
                            Util.createListFromJsonArray((JSONArray) element.get("value")),
                            "Wum status is not valid, Please amend and re-submit.");
                }
            }
        }
    }

    public List<String> getOverviewProducts() {

        return overviewProducts;
    }

    private void setOverviewProducts(List<String> overviewProducts) {

        if (overviewProducts.size() < 1) {
            throw new ServiceException("overview products list cannot be empty, overviewProducts:" + overviewProducts,
                    "Products list cannot be empty, Please amend and re-submit.");
        } else if (overviewProducts.get(0).equals("") || overviewProducts.get(0) == null) {
            throw new ServiceException("overview products list cannot have empty strings or null values," +
                    "overviewProducts:" + overviewProducts,
                    "products list cannot have empty strings or null values, Please amend and re-submit.");
        } else {
            this.overviewProducts = overviewProducts;
        }
    }

    String getWumProductsJSON() {

        return wumProductsJSON;
    }

    private void setWumProductsJSON(String wumProductsJSON) {

        this.wumProductsJSON = wumProductsJSON;
    }

    List<String> getOverviewCompatibleProducts() {

        return overviewCompatibleProducts;
    }

    private void setOverviewCompatibleProducts(List<String> overviewCompatibleProducts) {

        this.overviewCompatibleProducts = overviewCompatibleProducts;
    }

    List<String> getPatchInformationJarsInvolved() {

        return patchInformationJarsInvolved;
    }

    private void setPatchInformationJarsInvolved(List<String> patchInformationJarsInvolved) {

        this.patchInformationJarsInvolved = patchInformationJarsInvolved;
    }

    public List<String> getPeopleInvolvedDevelopedBy() {

        return peopleInvolvedDevelopedBy;
    }

    private void setPeopleInvolvedDevelopedBy(List<String> peopleInvolvedDevelopedBy) {

        if (peopleInvolvedDevelopedBy.size() < 1) {
            throw new ServiceException("peopleInvolvedDevelopedBy list cannot be empty," +
                    " peopleInvolvedDevelopedBy:" + peopleInvolvedDevelopedBy,
                    "Developed by list cannot be empty, Please amend and re-submit.");
        } else if (peopleInvolvedDevelopedBy.get(0).equals("") || peopleInvolvedDevelopedBy.get(0) == null) {
            throw new ServiceException("peopleInvolvedDevelopedBy list cannot have empty strings or null values," +
                    " peopleInvolvedDevelopedBy:" + peopleInvolvedDevelopedBy,
                    "Developed by list cannot have empty or null values, Please amend and re-submit.");
        } else if (!(peopleInvolvedDevelopedBy.get(0).contains("@wso2.com"))) {
            throw new ServiceException("developer email is not valid, developerEmail:"
                    + peopleInvolvedDevelopedBy.get(0),
                    "Developed by email is not valid, Please amend and re-submit.");
        } else {
            this.peopleInvolvedDevelopedBy = peopleInvolvedDevelopedBy;
        }
    }

    private void setPatchLifeCycleState(String patchLifeCycleState) {
        if (patchLifeCycleState.equals("")) {
            throw new ServiceException("patchLifeCycleState cannot have empty strings," +
                    " patchLifeCycleState:" + patchLifeCycleState);
        } else {
            this.patchLifeCycleState = patchLifeCycleState;
        }
    }

    public String getPatchLifeCycleState(){
        return patchLifeCycleState;
    }

    public String getWumStatus() {
        return wumStatus;
    }

    private void setWumStatus(String wumStatus) {
        this.wumStatus = wumStatus;
    }
}
