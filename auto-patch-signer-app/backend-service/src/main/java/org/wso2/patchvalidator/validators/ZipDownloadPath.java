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
package org.wso2.patchvalidator.validators;

import java.util.Properties;
import org.slf4j.Logger;
import org.wso2.patchvalidator.util.LogBuilder;
import org.wso2.patchvalidator.util.PropertyLoader;

/**
 * <h1>Zip Download Path</h1>
 * Define zip file download path and file paths
 *
 * @author Kosala Herath, Senthan Prasanth, Thushanthan
 * @version 1.2
 * @since 2017-12-14
 */
class ZipDownloadPath {

    private String type;
    private String version;
    private String id;
    private static Properties prop = PropertyLoader.getInstance().prop;
    private static final Logger LOG = LogBuilder.getInstance().LOG;

    public ZipDownloadPath(String type, String version, String id)  {


        this.type = type;
        this.version = version;
        this.id = id;
    }

    public String getDestFilePath() {

        return prop.getProperty("destFilePath") + type + "/";
    }

    public String getUrl() {

        version = getCarbonVersionWord(version);
        String url;
        if (type.equals("patch")) {
            return url = prop.getProperty("staticURL") + version + "/" + type + "es/" + type + id + "/";
        } else {
            return url = prop.getProperty("staticURL") + version + "/" + type + "s/" + type + id + "/";
        }
    }

    public String getZipDownloadDestination() {

        version = getCarbonVersionWord(version);
        version = prop.getProperty(version);
        return getDestFilePath() + version + "/" + type + id + "/";
    }

    public String getFilepath() {

        version = prop.getProperty(version);
        if (type.equals("patch")) {
            return getZipDownloadDestination() + prop.getProperty("orgPatch") + version + "-" + id + ".zip";
        } else {
            return getZipDownloadDestination() + prop.getProperty("orgUpdate") + version + "-" + id + ".zip";
        }
    }

    public String getUnzippedFolderPath() {

        String version1 = version;
        if (type.equals("patch")) {
            return getZipDownloadDestination() + prop.getProperty("orgPatch") + version1 + "-" + id + "/";
        } else {
            return getZipDownloadDestination() + prop.getProperty("orgUpdate") + version1 + "-" + id + "/";
        }
    }

    private String getCarbonVersionWord(String version) {
        switch (version) {
            case "4.4.0":
                version = "wilkes";
                break;
            case "5.0.0":
                version = "hamming";
                break;
            case "4.2.0":
                version = "turing";
                break;
        }
        return version;
    }
}

