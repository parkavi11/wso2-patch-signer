package org.wso2.patchvalidator.service;

import java.util.*;
import java.util.Map.Entry;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;


import org.wso2.patchvalidator.client.PmtClient;
import org.wso2.patchvalidator.client.UmtClient;
import org.wso2.patchvalidator.entryvalidator.PatchInfo;
import org.wso2.patchvalidator.exceptions.ServiceException;

/**
 * Patch list ordering handled by this class.
 */
class Signer {

    static StringBuilder sign() {
        ArrayList<String> patchesList = new ArrayList<>();
        StringBuilder returnMessage = new StringBuilder();
        HashMap<String, String> hMap = new HashMap<>();
        ArrayList<String> list = new ArrayList<>();
        ArrayList<String> orderedList = new ArrayList<>();
        LinkedHashMap<String, String> sortedMap = new LinkedHashMap<>();

        String carbonVersion;
        String patchId;
        JSONArray pmtResultArr;
        JSONObject patchJson;
        PatchInfo patchInfo;

        // call API  and get all the patches in the ReadyToSign state
        try {
            patchesList = UmtClient.getPatchList("ReadyToSign");
            System.out.println("------------------------------------------------------------------");
            System.out.println("                    Patch list from UMT Database                  ");
            System.out.println("------------------------------------------------------------------");
            for (String patch : patchesList){
                System.out.println(patch);
            }
            System.out.println();
        } catch (Exception ex) {
            System.out.println("Exception occurred when searching patches in the governance registry" +
                    " Exception: "+ ex);
        }

        if (patchesList.size() < 1) {
            System.out.println("No patches in the \"Ready to sign\" state");
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
                    System.out.println("Patch name retrieved from greg not in the correct format, patch:" + patch + " Exception: "+ ex);
                    continue;
                }

                //get PMT patch JSON
                try {
                    patchJson = PmtClient.getPatchInfo(carbonVersion, patchId);
                    pmtResultArr = (JSONArray) patchJson.get("pmtResult");
                } catch (ServiceException ex) {
                    System.out.println("Retrieving patch json failed, patch:" + patch+ " Exception: "+ ex);
                    continue;
                }

                try {
                    patchInfo = new PatchInfo(pmtResultArr);
                } catch (Exception ex) {
                    System.out.println("Creating object from pmt patch json failed, patch name: \"" + patch + "\". " +
                            " Exception: "+ ex);
                    continue;
                }

                if (patchInfo.getPatchLifeCycleState().equals("ReadyToSign")) {
                    hMap.put(patch, patchInfo.getWumReleasedTimestamp());
                } else {
                    System.out.println("======================Unable proceed to sign======================");
                    System.out.println("Patch: "+ patch+ " State : "+patchInfo.getPatchLifeCycleState());
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

            System.out.println("------------------------------------------------------------------");
            System.out.println("         Patch Signing order in ACS order by timestamp            ");
            System.out.println("------------------------------------------------------------------");
            for (String patch : orderedList){
                String[] patchReplacedNameArr = patch.replace("WSO2-CARBON-PATCH-", "")
                        .split("-");
                carbonVersion = patchReplacedNameArr[0].trim();//carbon version - 4.2.0/4.4.0/5.0.0
                patchId = patchReplacedNameArr[1].trim();
                patchJson = PmtClient.getPatchInfo(carbonVersion, patchId);
                pmtResultArr = (JSONArray) patchJson.get("pmtResult");
                patchInfo = new PatchInfo(pmtResultArr);
                System.out.println("Patch: "+ patch+ " timestamp: "+ patchInfo.getWumReleasedTimestamp()+
                        " WUM Status : "+patchInfo.getWumStatus());
            }

            System.out.println("------------------------------------------------------------------");
            System.out.println("                   Patches in UAT staging state                   ");
            System.out.println("------------------------------------------------------------------");
            patchesList = UmtClient.getPatchList("UATStaging");
            for (String patch : patchesList){
                System.out.println(patch);
            }
        }
        return returnMessage;
    }


}
