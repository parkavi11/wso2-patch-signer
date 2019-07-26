package org.wso2.patchvalidator.revertor;

import java.util.Properties;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.wso2.patchvalidator.client.UatClient;
import org.wso2.patchvalidator.constants.Constants;
import org.wso2.patchvalidator.exceptions.ServiceException;
import org.wso2.patchvalidator.util.PropertyLoader;

/**
 * <h1>UAT Reverter</h1>
 * Revert WUM UAT database when reverting patch.
 * Delete WUM staging rows related to patch
 * Delete WUM UAT rows related to patch
 *
 * @author Pramodya Mendis
 * @version 1.3
 * @since 2018-07-12
 */

class UatReverter {


    private static Properties prop = PropertyLoader.getInstance().prop;

    boolean revertUat(String patchId) { //1.stg 2.uat => Nishadi, dev:just for testing

        boolean isWumDevReverted;
        boolean isWumStgReverted;
        boolean isWumUatReverted;

//        try {
//            isWumDevReverted = deleteWumDev(patchId);
//        } catch (ServiceException ex) {
//            throw new ServiceException("Exception occurred when reverting WUM DEV, patchId:" + patchId,
//                    ex.getDeveloperMessage(), ex);
//        }
//        return isWumDevReverted;

        try {
            isWumStgReverted = deleteWumUat(patchId);
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred when reverting WUM Stg, patchId:" + patchId,
                    ex.getDeveloperMessage(), ex);
        }

        if (isWumStgReverted) {
            try {
                isWumUatReverted = deleteWumUat(patchId);

                if (isWumUatReverted) {
                    return true;
                } else {
                    throw new ServiceException("WUM Stg revert successful. WUM UAT revert failed. patchId:" + patchId,
                            "WUM Staging revert successful, WUM UAT revert failed for the update \"" +
                                    patchId + "\", Please contact admin.");
                }
            } catch (ServiceException ex) {
                throw new ServiceException("WUM Stg reverted successfully. Exception occurred when reverting" +
                        " WUM UAT, patchId:" + patchId, ex.getDeveloperMessage(), ex);
            }
        } else {
            throw new ServiceException("WUM Stg revert failed, patchId:" + patchId,
                    "WUM Staging revert failed for the update \"" + patchId + "\", Please contact admin.");
        }
    }

    private static boolean deleteWumStg(String updateId) {

        String uri = prop.getProperty("wumStgDeleteUrl");
        String jwtAssertionValue = getJwtAssertionValue(prop.getProperty("wumJwtAssertValue"));
        String forwardedForValue = prop.getProperty("forwardedForValue");

        JSONObject AccessTokenObj;
        boolean isUatUpdateDeleted;

        try {
            AccessTokenObj = UatClient.getUatAccessToken(Constants.wumUatGrantType,
                    Constants.wumUatUsername, Constants.wumUatPassword,
                    Constants.wumUatScope, Constants.wumUatAppKey, Constants.wumUatAccessTokenUri);
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred, when retrieving access token from WUM Stg. " +
                    " wumStgAccessTokenUri:" + prop.getProperty("wumStgAccessTokenUri") +
                    " wumStgGrantType:" + prop.getProperty("wumStgGrantType") +
                    " wumStgGrantTypeValue:" + prop.getProperty("wumStgGrantTypeValue") +
                    " wumStgAccTokenAuthorization:" + prop.getProperty("wumStgAccTokenAuthorization") +
                    " wumStgAppKey:" + prop.getProperty("wumStgAppKey") +
                    " wumStgUsername:" + prop.getProperty("wumStgUsername") +
                    " wumStgScope:" + prop.getProperty("wumStgScope"),
                    ex.getDeveloperMessage(), ex);
        }
        String authorizationValue = AccessTokenObj.get("token_type") + " " + AccessTokenObj.get("access_token");

        try {
            isUatUpdateDeleted = UatClient.deleteUatUpdate(updateId, uri, jwtAssertionValue, forwardedForValue,
                    authorizationValue);
            if (isUatUpdateDeleted) {
                return true;
            } else {
                throw new ServiceException("Deleting WUM Stg update failed, " + " updateId:" + updateId +
                        " uri:" + uri + " jwtAssertionValue:" + jwtAssertionValue + " forwardedForValue:" +
                        forwardedForValue + " authorizationValue:" + authorizationValue,
                        "Deleting update failed for the update \"" + updateId + "\" in WUM staging, " +
                                "Please contact admin.");
            }
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred when deleting WUM Stg update, " + " updateId:" + updateId +
                    " uri:" + uri + " jwtAssertionValue:" + jwtAssertionValue + " forwardedForValue:" +
                    forwardedForValue + " authorizationValue:" + authorizationValue, ex.getDeveloperMessage(), ex);
        }
    }


    private static boolean deleteWumUat(String updateId) {

        String uri = prop.getProperty("wumUatDeleteUrl");
        String jwtAssertionValue = getJwtAssertionValue(prop.getProperty("wumJwtAssertValue"));
        String forwardedForValue = prop.getProperty("forwardedForValue");

        JSONObject AccessTokenObj;
        boolean isUatUpdateDeleted;

        Constants constants = new Constants();

        try {
//            getUatAccessToken(String grantType, String username, String password, String scope, String key, String uri) {
            AccessTokenObj = UatClient.getUatAccessToken(prop.getProperty(Constants.wumUatGrantType),
                    prop.getProperty(Constants.wumUatUsername), prop.getProperty(Constants.wumUatPassword),
                    prop.getProperty(Constants.wumUatScope), prop.getProperty(Constants.wumUatAppKey),
                    prop.getProperty(Constants.wumUatAccessTokenUri));
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred, when retrieving access token from WUM UAT. " +
                    " wumUatAccessTokenUri:" + constants.wumUatAccessTokenUri +
                    " wumUatGrantType:" + constants.wumUatGrantType +
                    " wumUatGrantTypeValue:" + constants.wumUatGrantTypeValue +
                    " wumUatAccTokenAuthorization:" + prop.getProperty("wumUatAccTokenAuthorization") +
                    " wumUatAppKey:" + constants.wumUatAppKey +
                    " wumUatUsername:" + constants.wumUatUsername +
                    " wumUatScope:" + constants.wumUatScope +
                    " wumUatPassword:" + constants.wumUatPassword,
                    ex.getDeveloperMessage(), ex);
        }
        String authorizationValue = AccessTokenObj.get("token_type") + " " + AccessTokenObj.get("access_token");

        try {
            isUatUpdateDeleted = UatClient.deleteUatUpdate(updateId, uri, jwtAssertionValue, forwardedForValue,
                    authorizationValue);
            if (isUatUpdateDeleted) {
                return true;
            } else {
                throw new ServiceException("Deleting WUM UAT update failed, " + " updateId:" + updateId +
                        " uri:" + uri + " jwtAssertionValue:" + jwtAssertionValue + " forwardedForValue:" +
                        forwardedForValue + " authorizationValue:" + authorizationValue,
                        "Deleting update failed for the update \"" + updateId + "\" in WUM UAT, " +
                                "Please contact admin.");
            }
        } catch (ServiceException ex) {
            throw new ServiceException("Exception occurred when deleting WUM UAT update, " + " updateId:" + updateId +
                    " uri:" + uri + " jwtAssertionValue:" + jwtAssertionValue + " forwardedForValue:" +
                    forwardedForValue + " authorizationValue:" + authorizationValue, ex.getDeveloperMessage(), ex);
        }
    }


    private static String getJwtAssertionValue(String username) {

        String usernameObjStr = "{\"http://wso2.org/claims/emailaddress\":\"" + username + "\"}";
        String jwtAssertStr = usernameObjStr + "." + usernameObjStr + "." + usernameObjStr;
        byte[] bytesEncoded = Base64.encodeBase64(jwtAssertStr.getBytes());
        return new String(bytesEncoded);
    }
}
