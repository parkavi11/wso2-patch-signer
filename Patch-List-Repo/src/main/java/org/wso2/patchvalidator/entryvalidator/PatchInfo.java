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

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.Util;

/**
 * Create patch information object from PMT Patch Json.
 */
public class PatchInfo {

    private String patchLifeCycleState;
    private String wumStatus;
    private String wumReleasedTimestamp;

    //constructor
    public PatchInfo(JSONArray jsonArray) {

        for (Object aJsonArray : jsonArray) {

            JSONObject element = (JSONObject) aJsonArray;

            if (element.get("name").equals("registry.lifecycle.PatchLifeCycle.state")) {
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
            } else if (element.get("name").equals("wum_releasedTimestamp")) {
                this.setWumReleasedTimestamp(Util.createListFromJsonArray((JSONArray) element.get("value")).get(0));
            } else if (element.get("name").equals("wum_status")) {
                try {
                    this.setWumStatus(Util.createListFromJsonArray((JSONArray) element.get("value")).get(0));
                } catch (Exception ex) {
                    throw new ServiceException("wum_status is not valid in the pmt patch json, wum_status:" +
                            Util.createListFromJsonArray((JSONArray) element.get("value")),
                            "Wum status is not valid, Please amend and re-submit.");
                }
            }
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

    public String getPatchLifeCycleState() {
        return patchLifeCycleState;
    }


    public String getWumReleasedTimestamp() {
        return wumReleasedTimestamp;
    }

    public String getWumStatus() {
        return wumStatus;
    }

    private void setWumStatus(String wumStatus) {
        this.wumStatus = wumStatus;
    }

    private void setWumReleasedTimestamp(String wumReleasedTimestamp) {
        this.wumReleasedTimestamp = wumReleasedTimestamp;
    }
}
