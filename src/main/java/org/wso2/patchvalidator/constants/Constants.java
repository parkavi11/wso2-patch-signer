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
package org.wso2.patchvalidator.constants;

/**
 * <h1>Constants</h1>
 * Constants for microservice.
 *
 * @author Kosala Herath
 * @version 1.3
 * @since 2017-12-14
 */

public final class Constants {

    //SVN constants
    public static final String SVN_CONNECTION_FAIL_STATE = "SVN connection failure.";
    public static final String COMMIT_KEYS_FAILURE = "Failure in committing keys to SVN.";
    public static final String SUCCESSFULLY_KEY_COMMITTED = "Keys are successfully generated,committed and locked.";
    public static final String SUCCESSFULLY_VALIDATED = "Patch validation successful.";
    public static final String UPDATE_VALIDATED = "Update validation successful.";
    public static final String PROCESSING = "IN_PROCESS";
    public static final String QUEUE = "IN_QUEUE";
    public static final String CONNECTION_SUCCESSFUL = "Connection Successful.";
    //entry validator constants
    public static final String OVERVIEW_COMPATIBLE_PRODUCTS = "\"Overview compatible products\" validated successfully";
    public static final String OVERVIEW_PRODUCTS = "\"Overview products\" validated successfully";
    public static final String PATCH_INFO_JARS_INVOLVED = "\"Patch information jars involved\" validated successfully";
    public static final String ENTRY_VALIDATION_SUCCESSFUL = "PMT Entry validation finished successfully";
    //PMT update constants
    public static final String PMT_UPDATE_ADMIN_STG_SUCCESSFUL = " PMT LC state updated to \"Admin staging\" state.";
    public static final String PMT_UPDATE_UAT_STG_SUCCESSFUL = " PMT LC state updated to \"UAT staging\" state.";
    public static final String PMT_UPDATE_TESTING_SUCCESSFUL = " PMT LC state updated to \"Testing\" state.";
    public static final String PMT_UPDATE_RELEASED_SUCCESSFUL = " PMT LC state updated to \"Released\" state.";
    public static final String PMT_UPDATE_RELEASED_NOT_AUTOMATED_SUCCESSFUL = " PMT LC state updated to \"Released not"
            + " automated\" state.";
    public static final String PMT_UPDATE_RELEASED_NOT_IN_PUBLIC_SVN_SUCCESSFUL = " PMT LC state updated to \"Released"
            + " not in public svn\" state.";
    public static final String PMT_UPDATE_FAIL = " PMT update failed.";
    //patch types
    public static final int PATCH_ONLY = 1;
    public static final int UPDATE_ONLY = 2;
    public static final int PATCH_AND_UPDATE = 3;
    //developer message
    public static final String CONTACT_ADMIN = "Contact admin. ";
    public static final String INTERNAL_PROBLEM = "Internal problem retrieving patch information.";
    public static final String FAILURE_MESSAGE = "failure";
    public static final String SUCCESS_MESSAGE = "success";
    public static final String WUM_UC_ERROR_MESSAGE = "Validating update :[ERROR]";
    public static final String WUM_UC_SUCCESS_MESSAGE = "Validating update :";

    //Credentials for WUm UAT
    public static final String wumUatGrantType = "";
    public static final String wumUatGrantTypeValue = "";
    public static final String wumUatUsername = "";
    public static final String wumUatPassword = "";
    public static final String wumUatScope = "";
    public static final String wumUatAppKey = "";
    public static final String wumUatAccessTokenUri = "";

    public Constants() {
        // restrict instantiation
    }

}

