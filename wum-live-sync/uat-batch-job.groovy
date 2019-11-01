import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import jenkins.model.*

// Retrieve the access tokens for WUM.
private def wumAccessToken(appKey, username, password, scope) {

    def accessTokenEndpoint = new URL('https://gateway.api.cloud.wso2.com/token')
    HttpURLConnection connectionLive = (HttpURLConnection) accessTokenEndpoint.openConnection()
    connectionLive.addRequestProperty("Authorization", "Basic " + appKey)

    connectionLive.with {
        doOutput = true
        requestMethod = 'POST'
        outputStream.withWriter { writer ->
            writer << 'grant_type=password&username=' + username + '&password=' + password
        }
        def accessToken = null
        try {
            def response = content.text.toString()
            def jsonSlurper = new JsonSlurper()
            def jsonObject = jsonSlurper.parseText(response)
            accessToken = jsonObject.access_token
        } catch (IOException ioex) {
            println "Request to the access token endpoint failed: " + ioex
        }

        return accessToken
    }
}

// Retrieve the latest timestamp from WUM.
private def latestTimestamp(accessToken) {

    def timestampEndpoint = new URL('https://api.updates.wso2.com/updates/3.0.0/timestamps/latest')
    HttpURLConnection connectionLive = (HttpURLConnection) timestampEndpoint.openConnection()
    connectionLive.addRequestProperty("Accept", "application/json")
    connectionLive.addRequestProperty("Authorization", "Bearer " + accessToken)

    connectionLive.with {
        doOutput = true
        requestMethod = 'GET'
        def timestamp = null
        try {
            def response = content.text.toString()
            def jsonSlurper = new JsonSlurper()
            def jsonObject = jsonSlurper.parseText(response)
            timestamp = jsonObject.timestamp
        } catch (IOException ioex) {
            println "Request to the WUM Live latest timestamp endpoint failed: " + ioex
        }

        return timestamp
    }
}

// Retrieve the list of new updates from WUM UAT.
private def newUpdates(accessToken, timestamp) {

    // Append the timestamp to the end of the URL.
    def updatesEndpoint = new URL('https://gateway.api.cloud.wso2.com/t/wso2umuat/updates/3.0.0/products/' + timestamp)
    HttpURLConnection connectionLive = (HttpURLConnection) updatesEndpoint.openConnection()
    connectionLive.addRequestProperty("Accept", "application/json")
    connectionLive.addRequestProperty("Authorization", "Bearer " + accessToken)

    connectionLive.with {
        doOutput = true
        requestMethod = 'GET'
        def jsonObject = null
        try {
            def response = content.text.toString()
            def jsonSlurper = new JsonSlurper()
            jsonObject = jsonSlurper.parseText(response)
        } catch (IOException ioex) {
            println "Request to the WUM UAT updates endpoint failed: " + ioex
        }

        return jsonObject
    }
}

// Retrieve the list of channels for a particular product version.
private def channels(product, version, accessToken) {

    // Append the product and version to the end of the URL.
    def updatesEndpoint = new URL('https://gateway.api.cloud.wso2.com/t/wso2umuat/channels/3.0.0/user/' + product
            + "/" + version)
    HttpURLConnection connectionLive = (HttpURLConnection) updatesEndpoint.openConnection()
    connectionLive.addRequestProperty("Accept", "application/json")
    connectionLive.addRequestProperty("Authorization", "Bearer " + accessToken)

    connectionLive.with {
        doOutput = true
        requestMethod = 'GET'
        def jsonObject = null
        try {
            def response = content.text.toString()
            def jsonSlurper = new JsonSlurper()
            jsonObject = jsonSlurper.parseText(response)
        } catch (IOException ioex) {
            println "Request to the WUM UAT channels endpoint failed: " + ioex
        }

        return jsonObject.channels
    }
}

// Executing shell commands obtained as the method parameter.
private static executeShellCommand(command) {

    def sout = new StringBuffer(), serr = new StringBuffer()

    def proc = ['bash', '-c', command].execute()
    proc.consumeProcessOutput(sout, serr)
    proc.waitFor()
    return sout + serr
}

// Retrieve the products to be updated either as an input parameter or through the WUM endpoint.
private def productsToBeUpdated(productsInputParameter, wumUatAccessToken, latestWumLiveTimestamp) {

    def jsonSlurper = new JsonSlurper()
    def newUpdateList = []
    if (productsInputParameter == "*") {
        // Getting the list of new updates from WUM UAT.
        try {
            newUpdateList = newUpdates(wumUatAccessToken, latestWumLiveTimestamp)
        } catch (Exception e) {
            println "Error while getting the list of new updates from WUM UAT: " + e
        }
    } else {
        def jsonString = ""
        def products = productsInputParameter.tokenize(',')
        def comma = ","
        products.each {
            if (it == products.last()) {
                comma = ""
            }
            def tokenizedInput = it.tokenize("-")
            def lastToken = tokenizedInput.last()
            tokenizedInput = tokenizedInput - lastToken
            def tokenizedProductName = tokenizedInput.join("-")
            jsonString += '{ "product-name": "' + tokenizedProductName.getAt(0..-7) + '", "product-version": "'
            +tokenizedProductName.getAt(-5..-1) + '", "channel": "' + lastToken + '" }' + comma
        }
        def completeJson = "[" + jsonString + "]"
        newUpdateList = jsonSlurper.parseText(completeJson)
    }

    return newUpdateList
}

// Checks whether there exists updates for a particular product-version-channel.
private static boolean hasUpdates(productVersionChannel, wumBinLocation) {

    def noUpdate = "There are no new updates available for the product"
    def checkUpdateCommand = wumBinLocation + " check-update " + productVersionChannel

    // Checking for WUM product-version-channel updates.
    def checkUpdateOutput = executeShellCommand(checkUpdateCommand).toString()
    return !checkUpdateOutput.contains(noUpdate)
}

boolean parent_job() {

    // Location to the WUM 3.0 bin folder.
    def wumBinLocation = "/build/jenkins-home/workspace/wumInstall/wum3/bin/wum"
    // Location to where the WSO2 products are downloaded by WUM 3.0.
    def wumProductLocation = "/home/ubuntu/.wum3/products/"
    // Name of the folder for storing WUM updated packs in Jenkins workspace.
    def updatesFolderName = "wum3-updates"
    // Access token for the WUM UAT environment.
    def wumUatAccessToken = ""
    // Access token for the WUM Live environment.
    def wumLiveAccessToken = ""
    // Latest timestamp of the WUM Live environment.
    def latestWumLiveTimestamp = ""
    // List of new updates available in the WUM UAT when compared with the latest timestamp of the WUM Live.
    def newUpdateList = []
    // Variable to store the list of child jobs in WUM_Release view.
    def childJobNameList = []
    // String for collecting information to make a json object.
    def updateDetails = "["
    // Json object to pass information to the Parallel Trigger job.
    def detailsForParallelTrigger
    // Folder where the child jobs are present
    def childJobFolder = "wum3"

    // Get the environment variables.
    def build = this.getProperty('binding').getVariable('build')
    def listener = this.getProperty('binding').getVariable('listener')
    def env = build.getEnvironment(listener)

    // Setting the environment properties to variables.
    def jenkinsUsername = env.UAT_JENKINS_USER
    def jenkinsPassword = env.UAT_JENKINS_PASS
    def jobUrl = env.JOB_URL
    def jobWorkspace = env.WORKSPACE
    def productsInputParameter = env.PRODUCTS

    // Define the WUM UAT AppKey and the WUM Live AppKey.
    def uatAppKey = env.WUM_UAT_APPKEY
    def liveAppKey = env.WUM_LIVE_APPKEY

    List<String> scopeList = new ArrayList<String>()
    scopeList.add("updates_get_list")
    scopeList.add("updates_get_products")
    scopeList.add("updates_get_latest_timestamp")
    scopeList.add("channels_get_channel_for_product")

    // Scope for wum-live
    def scopeLive = "updates_get_latest_ timestamp"

    def jsonSlurper = new JsonSlurper()

    // Retrieve the child job names from "wum3" folder.
    Jenkins.instance.getItemByFullName(childJobFolder).allJobs.each {
        def childJobName = it.name.toString()
        if (childJobName.startsWith("wum-")) {
            childJobNameList.add(childJobName)
        }
    }

    println "\n" + "Child Jobs Available: " + childJobNameList + "\n"

    // Retrieve the access token for WUM UAT.
    for (String item : scopeList) {
        try {
            wumUatAccessToken = wumAccessToken(uatAppKey, jenkinsUsername, jenkinsPassword, item)
            println "WUM UAT access token: " + wumUatAccessToken + "\n"
        } catch (Exception e) {
            throw new Exception("Error while fetching the WUM UAT access token: " + e)
        }
    }

    // Retrieve the access token for WUM Live.
    try {
        wumLiveAccessToken = wumAccessToken(liveAppKey, jenkinsUsername, jenkinsPassword, scopeLive)
        println "WUM Live access token: " + wumLiveAccessToken + "\n"
    } catch (Exception e) {
        throw new Exception("Error while fetching the WUM Live access token: " + e)
    }

    // Retrieve the latest timestamp from WUM Live.
    try {
        latestWumLiveTimestamp = latestTimestamp(wumLiveAccessToken)
    } catch (Exception e) {
        println "Error while fetching the latest timestamp from WUM Live: " + e + "\n"
    }

    // Retrieve the products to be updated, either as an input parameter or from the WUM endpoint.
    try {
        newUpdateList = productsToBeUpdated(productsInputParameter, wumUatAccessToken, latestWumLiveTimestamp)
    } catch (Exception e) {
        println "Error while processing the products to be updated: " + e
    }

    // Clearing the existing workspace folder.
    def deleteFolder = "rm -rf " + jobWorkspace + "/" + updatesFolderName + "/"
    def deleteFolderOutput = executeShellCommand(deleteFolder)
    if (deleteFolderOutput != "" && deleteFolderOutput != null) {
        println "Deleting the existing pack folder in the workspace: " + deleteFolderOutput + "\n"
    }

    // Command to create a new folder in Jenkins workspace to store WUM 3.0 updated packs.
    def newFolderCommand = "mkdir -p " + jobWorkspace + "/" + updatesFolderName + "/"
    def newFolderOutput = executeShellCommand(newFolderCommand)
    if (newFolderOutput != "" && newFolderOutput != null) {
        println "Creating a new pack folder in the workspace: " + newFolderOutput + "\n"
    }

    def comma = ","

    newUpdateList.each {

        def productName = it.getAt("product-name")
        println "Product Name: " + productName
        def productNameWithoutWso2

        if (productName.startsWith("wso2-")) {
            productNameWithoutWso2 = productName.drop(5)
        } else {
            productNameWithoutWso2 = productName.drop(4)
        }

        def productVersion = it.getAt("product-version")
        println "Product-Version: " + productName + "-" + productVersion
        def productVersionName = productNameWithoutWso2 + "-" + productVersion
        def channelList = []
        def channelName = it.getAt("channel")

        if (channelName == null) {
            // Getting the list of channels for a particular product version.
            try {
                channelList = channels(productName, productVersion, wumUatAccessToken)
            } catch (Exception e) {
                println "Error while fetching the list of available channels: " + e
            }
        } else {
            channelList.add(channelName)
        }

        // Variable to store the channel details of each product versions.
        def channelViceDetails = "["

        channelList.each {

            // Stores "true" if the channel has updates, "false" if it doesn't.
            def channelStatus = true
            def channel = it.toString()
            def productVersionChannel = productName + "-" + productVersion + " " + channel
            def packLocation = wumProductLocation + productName + "/" + productVersion + "/" + channel + "/"
            def jobName = ""

            childJobNameList.each {
                def childName = "wum-" + productVersionName + "-uat"
                if (channel == "security") {
                    childName += "_security"
                }
                if (it == childName) {
                    jobName = it.toString()
                }
            }

            if (jobName != "") {

                def channelUpdate = hasUpdates(productVersionChannel, wumBinLocation)
                if (channelUpdate) {
                    println "Channel [" + channel + "] has updates."
                } else {
                    channelStatus = false
                    println "Channel [" + channel + "] has no updates."
                }

                def deletePacks = "rm -f `ls -t " + packLocation + "wso2*.zip|awk 'NR >0'`"
                def deleteOutput = executeShellCommand(deletePacks)
                if (deleteOutput != "" && deleteOutput != null) {
                    println "Deleting Packs in WUM 3.0 Product Base: " + deleteOutput
                }

                // Doing a WUM update for product-version-channel.
                def updateCommand = wumBinLocation + " update " + productVersionChannel
                def updateOutput = executeShellCommand(updateCommand)
                println "$updateOutput"

                // Get the name of the latest product pack with the timestamp.
                def command = "ls -t " + packLocation + " | head -n 1"
                def productZipName = executeShellCommand(command).trim()
                println "Updated Pack: " + productZipName.trim()

                if (productZipName.startsWith("wso2")) {
                    // Removes the previous product packs created, leaving the latest 2.
                    def deletePreviousPacks = "rm -f `ls -t " + packLocation + "wso2*.zip|awk 'NR >2'`"
                    def deletePacksOutput = executeShellCommand(deletePreviousPacks)
                    if (deletePacksOutput != "" && deletePacksOutput != null) {
                        println "Deleting Past Packs in WUM 3.0 Product Base: " + deletePacksOutput
                    }

                    // Moves the product pack to the Jenkins workspace.
                    def packMovingOutput = executeShellCommand("cp " + packLocation + productZipName + " "
                            + jobWorkspace + "/" + updatesFolderName + "/")
                    if (packMovingOutput != "" && packMovingOutput != null) {
                        println "Moving the Pack to Jenkins Workspace: " + packMovingOutput
                    }

                    // Renaming the pack name
                    def zipLocation = jobWorkspace + "/" + updatesFolderName + "/"
                    def rename = "mv " + zipLocation + productZipName + " " + zipLocation + productName + "-"
                    +productVersion + "-" + channel + ".zip"
                    def renamingOutput = executeShellCommand(rename)
                    if (renamingOutput != "" && renamingOutput != null) {
                        println "Renaming the pack: " + renamingOutput
                    }
                    println "================ ZIP Location: $zipLocation =================="

                    def packName = productName + "-" + productVersion + "-" + channel + ".zip"

                    // URL of the product pack in the Jenkins workspace.
                    def packUrl = jobUrl + "ws/" + updatesFolderName + "/" + packName
                    println "================ PACK Url: $packUrl =================="

                    channelViceDetails += '{ "channel": "' + channel + '", "channelStatus": "' + channelStatus
                    +'", "jobName": "' + jobName + '", "updateFileUrl": "' + packUrl + '", "updateFileName": "'
                    +packName + '" }' + comma
                }
            } else {
                println "Could not find a child job for: " + productVersionName + "-" + channel
            }
        }

        // Removing the additional comma at the end of the last element.
        if (channelViceDetails.length() > 1) {
            channelViceDetails = channelViceDetails.getAt(0..-2)
        }

        channelViceDetails += "]"
        updateDetails += '{ "Product": "' + productName + '", "Version": "' + productVersion
        +'", "ChannelViceDetails": ' + channelViceDetails + '}' + comma
        println "\n"
    }

    // Removing the additional comma at the end of the last element.
    if (updateDetails.length() > 1) {
        updateDetails = updateDetails.getAt(0..-2)
    }

    updateDetails += "]"
    detailsForParallelTrigger = jsonSlurper.parseText(updateDetails)
    println "JSON for the Parallel Trigger Job: " + detailsForParallelTrigger + "\n"
    System.properties['UPDATE_LIST_JSON_TEST'] = new JsonBuilder(detailsForParallelTrigger).toString()
    return true
}

parent_job()
