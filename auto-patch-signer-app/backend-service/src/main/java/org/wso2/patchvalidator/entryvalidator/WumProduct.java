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

/**
 * <h1>Wum Product</h1>
 * Create object for each product, of wumProducts info products lists (compatible-products etc).
 *
 * @author Pramodya Mendis
 * @version 1.3
 * @since 2018-07-12
 */

class WumProduct {

    private String productName;
    private String baseVersion;
    private List<String> addedFiles;
    private List<String> modifiedFiles;
    private List<String> removedFiles;
    private String productAbbreviation;

    String getProductName() {
        return productName;
    }

    void setProductName(String productName) {
        this.productName = productName;
    }

    String getBaseVersion() {
        return baseVersion;
    }

    void setBaseVersion(String baseVersion) {
        this.baseVersion = baseVersion;
    }

    List<String> getAddedFiles() {
        return addedFiles;
    }

    void setAddedFiles(List<String> addedFiles) {
        this.addedFiles = addedFiles;
    }

    List<String> getModifiedFiles() {
        return modifiedFiles;
    }

    void setModifiedFiles(List<String> modifiedFiles) {
        this.modifiedFiles = modifiedFiles;
    }

    List<String> getRemovedFiles() {
        return removedFiles;
    }

    void setRemovedFiles(List<String> removedFiles) {
        this.removedFiles = removedFiles;
    }

    String getProductAbbreviation() {
        return productAbbreviation;
    }

    void setProductAbbreviation(String productAbbreviation) {
        this.productAbbreviation = productAbbreviation;
    }
}
