package org.wso2.patchvalidator.service;

import java.util.*;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


import org.slf4j.Logger;
import org.wso2.patchvalidator.client.PmtClient;
import org.wso2.patchvalidator.client.UmtClient;
import org.wso2.patchvalidator.entryvalidator.PatchInfo;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.LogBuilder;

/**
 * Patch list ordering handled by this class.
 */
public class Signer {
    private static final Logger LOG = LogBuilder.getInstance().LOG;

    static StringBuilder sign() {
        List<String> patchesList = new ArrayList<>();
        StringBuilder returnMessage = new StringBuilder();
        HashMap<String, String> hMap = new HashMap<>();
        List<String> list = new ArrayList<>();
        ArrayList<String> orderedList = new ArrayList<>();
        LinkedHashMap<String, String> sortedMap = new LinkedHashMap<>();

        String carbonVersion;
        String patchId;
        JSONArray pmtResultArr;
        JSONObject patchJson;
        PatchInfo patchInfo;
        String dash = "------------------------------------------------------------------";

        // call API  and get all the patches in the ReadyToSign state
        try {
            patchesList = UmtClient.getPatchList("ReadyToSign");
            LOG.info(dash);
            LOG.info("                    Patch list from UMT Database                  ");
            LOG.info(dash);
            for (String patch : patchesList) {
                LOG.info(patch);
            }
        } catch (Exception ex) {
            LOG.error("Exception occurred when searching patches in the governance registry, %s", ex);
        }

        if (patchesList.isEmpty()) {
            LOG.info("No patches in the "Ready to sign" state");
        } else {
            /**
             * iterate the patches list and check the state in registry
             */

            for (String patch : patchesList) {
                //get version and patch id from patch name string
                try {
                    //WSO2-CARBON-PATCH-4.4.0-2912
                    String[] patchReplacedNameArr = patch.replace("WSO2-CARBON-PATCH-", "")
                            .split("-");
                    carbonVersion = patchReplacedNameArr[0].trim();//carbon version - 4.2.0/4.4.0/5.0.0
                    patchId = patchReplacedNameArr[1].trim(); //2912
                } catch (Exception ex) {
                    LOG.error("Patch name retrieved from greg not in the correct format, %s", ex);
                    continue;
                }

                //get PMT patch JSON
                try {
                    patchJson = PmtClient.getPatchInfo(carbonVersion, patchId);
                    pmtResultArr = (JSONArray) patchJson.get("pmtResult");
                } catch (ServiceException ex) {
                    LOG.error("Retrieving patch json failed,%s", ex);
                    continue;
                }

                try {
                    patchInfo = new PatchInfo(pmtResultArr);
                } catch (Exception ex) {
                    LOG.error("Creating object from pmt patch json failed, patch name: \"" + patch + "\". %s", ex);
                    continue;
                }

                if (patchInfo.getPatchLifeCycleState().equals("ReadyToSign")) {
                    hMap.put(patch, patchInfo.getWumReleasedTimestamp());
                } else {
                    LOG.info("======================Unable proceed to sign======================");
                    LOG.info(String.format("Patch: %s State : %s", patch, patchInfo.getPatchLifeCycleState()));
                }
            }

            /**
             * After validating the patch status with registry : use hashmap and create new patch list by timestamp order
             */
            for (Map.Entry<String, String> entry : hMap.entrySet()) {
                list.add(entry.getValue());
            }
            Collections.sort(list, new Comparator<String>() {
                public int compare(String str, String str1) {
                    return (str).compareTo(str1);
                }
            });
            for (String str : list) {
                for (Entry<String, String> entry : hMap.entrySet()) {
                    if (entry.getValue().equals(str)) {
                        sortedMap.put(entry.getKey(), str);
                        orderedList.add(entry.getKey());
                    }
                }
            }

            LOG.info(dash);
            LOG.info("         Patch Signing order in ACS order by timestamp            ");
            LOG.info(dash);
            for (String patch : orderedList) {
                String[] patchReplacedNameArr = patch.replace("WSO2-CARBON-PATCH-", "")
                        .split("-");
                carbonVersion = patchReplacedNameArr[0].trim();//carbon version - 4.2.0/4.4.0/5.0.0
                patchId = patchReplacedNameArr[1].trim();
                patchJson = PmtClient.getPatchInfo(carbonVersion, patchId);
                pmtResultArr = (JSONArray) patchJson.get("pmtResult");
                patchInfo = new PatchInfo(pmtResultArr);
                LOG.info(String.format("Patch: %s timestamp: %s WUM Status :%s "
                        , patch, patchInfo.getWumReleasedTimestamp(), patchInfo.getWumStatus()));
            }

            LOG.info(dash);
            LOG.info("                   Patches in UAT staging state                   ");
            LOG.info(dash);
            patchesList = UmtClient.getPatchList("UATStaging");
            for (String patch : patchesList) {
                LOG.info(patch);
            }
        }
        return returnMessage;
    }


}
