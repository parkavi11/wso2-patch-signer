/*
 *  Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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

package org.wso2.patchvalidator.client;

import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.context.ConfigurationContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.base.ServerConfiguration;
import org.wso2.carbon.governance.api.generic.GenericArtifactManager;
import org.wso2.carbon.governance.api.generic.dataobjects.GenericArtifact;
import org.wso2.carbon.governance.api.util.GovernanceUtils;
import org.wso2.carbon.governance.client.WSRegistrySearchClient;
import org.wso2.carbon.registry.core.Registry;
import org.wso2.carbon.registry.core.pagination.PaginationContext;
import org.wso2.carbon.registry.core.session.UserRegistry;
import org.wso2.carbon.registry.ws.client.registry.WSRegistryServiceClient;
import org.wso2.patchvalidator.service.SyncService;

import java.io.File;
import java.util.*;

import org.wso2.patchvalidator.util.PropertyLoader;

/**
 * Client for accessing PMT governance registry.
 */
public class GregClient {

    private static final Logger LOG = LoggerFactory.getLogger(SyncService.class);
    private static ConfigurationContext configContext = null;

    private static Properties prop = PropertyLoader.getInstance().prop;

    private static String CARBON_HOME = "";
    private static String username = "";
    private static String password = "";
    private static String serverURL = "";

    private static WSRegistryServiceClient initialize() throws Exception {

        CARBON_HOME = prop.getProperty("gregCarbonHome");
        final String axis2Repo =
                CARBON_HOME + File.separator + "repository" + File.separator + "deployment" + File.separator + "client";
        final String axis2Conf = ServerConfiguration.getInstance()
                .getFirstProperty("Axis2Config.clientAxis2XmlLocation");
        username = prop.getProperty("gregUserName");
        password = prop.getProperty("gregPassword");
        serverURL = prop.getProperty("gregServerUri");

        System.setProperty("javax.net.ssl.trustStore", prop.getProperty("gregTruststore"));
        System.setProperty("javax.net.ssl.trustStorePassword", prop.getProperty("gregTruststorePassword"));
        System.setProperty("javax.net.ssl.trustStoreType", "JKS");
        System.setProperty("carbon.repo.write.mode", "true");

        configContext = ConfigurationContextFactory.createConfigurationContextFromFileSystem(axis2Repo, axis2Conf);
        return new WSRegistryServiceClient(serverURL, username, password, configContext);

    }

    public static ArrayList<String> search() throws Exception {

        ArrayList<String> list = new ArrayList<>();
        try {

            final Registry registry = initialize();
            Registry gov = GovernanceUtils.getGovernanceUserRegistry(registry, "admin");

            // Should be load the governance artifact.
            GovernanceUtils.loadGovernanceArtifacts((UserRegistry) gov,
                    GovernanceUtils.findGovernanceArtifactConfigurations(gov));

            //Initialize the pagination context.
            //Top five services, sortBy name , and sort order descending.
            PaginationContext.init(0, 10, "ASC",  "wum_releasedTimestamp", 100);
            WSRegistrySearchClient wsRegistrySearchClient = new WSRegistrySearchClient(serverURL, username, password,
                    configContext);

            //This should be execute to initialize the AttributeSearchService.
            wsRegistrySearchClient.init();

            //Initialize the GenericArtifactManager
            GenericArtifactManager artifactManager = new GenericArtifactManager(gov, "patch");
            Map<String, List<String>> listMap = new HashMap<>();

            //Create the search attribute map
//            listMap.put("lcName", new ArrayList<String>() {
//                {
//                    //add("PatchLifeCycle");
//                    add("Security_PatchLifeCycle");
//                }
//            });
            listMap.put("lcState", new ArrayList<String>() {
                {
                    add("ReadyToSign");
                }
            });

            //Find the results.
            GenericArtifact[] genericArtifacts = artifactManager.findGenericArtifacts(listMap);

            for (GenericArtifact artifact : genericArtifacts) {
                list.add(artifact.getPath());
            }
            if (genericArtifacts.length != 0) {
                LOG.info("Patches to be signed:" + list);
            }

        } finally {
            PaginationContext.destroy();
        }
        return list;
    }

}
