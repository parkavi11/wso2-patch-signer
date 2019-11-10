import groovy.json.JsonSlurper
import groovy.json.JsonSlurperClassic
import jenkins.model.Jenkins

// Executing a shell command obtained as the method parameter.
def shellCommandOutput(command) {
    def sout = new StringBuffer(), serr = new StringBuffer()
    try {
        def proc = ['bash', '-c', command].execute()
        proc.consumeProcessOutput(sout, serr)
        proc.waitFor()
    } catch (Exception e) {
        throw new Exception("Exception while executing the shell command: " + command + "\n" + e)
    }
    return sout + serr

}


// Returns the hyperlink of a link and its display text.
def getHyperLink(link, text) {
    return hudson.console.ModelHyperlinkNote.encodeTo(link, text)

}


// Triggers a build of the UAT Batch Job and does not wait for its completion.
def reTrigger() {
    if (env.TRIGGER) {
        def jobName = "others/uat_batch_wum3_test"
        build(job: jobName, wait: false)
    }
}

// Analyzes the build results to determine the status of the update.
// Whether the builds should be retriggered, update should be reverted or live synced.
def resultsAnalysis() {
    def jsonSlurper = new JsonSlurper()
    def buildStatusString = env.BUILD_RESULTS
    def updatesString = System.properties['UPDATE_LIST']
    def allUpdates = jsonSlurper.parseText(updatesString)
    def buildStatus = evaluate(buildStatusString)
    def remainingUpdates = allUpdates
    def revertedUpdates = []
    def withHeldUpdates = []
    def releasedUpdates = []
    def retriggeringProducts = []
    def updateState = [:]
    def productList = []
    def productUpdates = [:]

    remainingUpdates.each {
        def j = 0
        def updateId = it.get("update_id")
        def products = it.get("product-versions")
        productList += products
        for (product in products) {
            def status = buildStatus.get(product)
            if (status == "FAILURE" || status == "UNSTABLE") {
                updateState.put(updateId, "Revert")
                remainingUpdates = remainingUpdates.minus(it)
                revertedUpdates.add(updateId)
                break
            }
            j++
        }
    }
    productList.toSet()
    productList.each {
        def updateList = []
        def product = it
        allUpdates.each {
            if (it.get("product-versions").contains(product)) {
                updateList.add(it.get("update_id"))
            }
        }
        productUpdates.put(product, updateList)
    }

    remainingUpdates.each {
        def updateId = it.get("update_id")
        def products = it.get("product-versions")
        for (product in products) {
            def updatesAdded = productUpdates.get(product)
            if (updatesAdded.size() > 1) {
                for (update in updatesAdded) {
                    if (revertedUpdates.contains(update)) {
                        retriggeringProducts.add(product)
                    }
                    if (revertedUpdates.contains(update) || withHeldUpdates.contains(update)) {
                        withHeldUpdates.add(updateId)
                    }
                }
            }
        }
    }

    releasedUpdates = remainingUpdates.update_id - withHeldUpdates.toSet()

    def userInput = 'Proceed with Live Sync for Updates: ' + releasedUpdates

    if (retriggeringProducts.isEmpty()) {
        env.trigger = false
    } else {
        env.trigger = true
    }

    jsonSlurper = null

}

// Does the live sync for a given set of updates.
def liveSync() {
    def sout = new StringBuilder(), serr = new StringBuilder()
    println("Release updates passed from main method>>>>>> " + releasedUpdates)

    //path to script file for logging into the instance and start live syncing
    def pathToScript = '/Users/parkavi/Documents/Parkavi/scripts/loginScript.sh'
    def proc = pathToScript.execute()
    proc.waitForOrKill(10000)

}

// Returns the release type of a given update.
def getReleaseType(platformVersion, updateId) {
    def updateName = "WSO2-CARBON-PATCH-" + platformVersion + "-" + updateId
    def endpointUrl = env.PMT_ENDPOINT
    def curlCommand = "curl -s -k -v -X POST " + endpointUrl + "getPatchInfo -F patch=" + updateName

    def result = shellCommandOutput(curlCommand)

    def output = result.toString()
    def jsonSlurper = new JsonSlurper()
    jsonObject = jsonSlurper.parseText(output)
    def properties = jsonObject.pmtResult
    def releasedState = null
    properties.each {
        if (it.name == "wum_status") {
            releasedState = it.value
        }
    }
    return releasedState[0]

}

def updateLifecycleState(platformVersion, updateId, lifecycleState) {
    UPDATE_NAME = "WSO2-CARBON-PATCH-" + platformVersion + "-" + updateId
    LIFECYCLE_STATE = lifecycleState

    try {
        def updateReleasedState = sh returnStdout: true, script: """
            set +x
            curl -k -v -X POST ${env.PMT_ENDPOINT}updatePmtToReleased -F patch=${
                UPDATE_NAME
            } -F releasedState=${LIFECYCLE_STATE}
            """
    } catch (Exception e) {
        println "Exception: " + e
    }
    def successMessage = "Successfully updated the lifecycle state in the PMT."
    if (!updateReleasedState.contains(successMessage)) {
        throw new Exception("Error while updating the lifecycle state in the PMT: " + UPDATE_NAME + "-" + LIFECYCLE_STATE)
    }
}


pipeline {
    agent any
    environment {
        PMT_ENDPOINT = "https://192.168.56.40:9092/request/"
        BUILD_RESULTS = ""
        RELEASING_UPDATES = ""
        REVERTING_UPDATES = ""
        RETRIGGERING_UPDATES = ""
        JENKINS_URL = "https://supportbuild-wilkes-upgrade.wso2.com/jenkins/job/wum3/job/"
    }
    stages {
        stage("build") {
            steps {
                script {
                    try {
                        def receivedDetails = System.properties['UPDATE_LIST_JSON_TEST']
                        def jsonSlurper = new JsonSlurperClassic()
                        def receivedJson = jsonSlurper.parseText(receivedDetails.toString())

                        jsonSlurper = null

                        def childJobNameList = []
                        Jenkins.instance.getItemByFullName("wum3").allJobs.each {
                            def childJobName = it.name.toString()
                            if (childJobName.startsWith("wum-")) {
                                childJobNameList.add(childJobName)
                            }
                        }

                        env.BUILD_RESULTS = ""
                        def branches = [:]
                        def entry = [:]
                        def jobProductNameMap = [:]
                        def results = []

                        for (int index = 0; index < receivedJson.size(); index++) {
                            def channelDetails = receivedJson[index].ChannelViceDetails
                            def productName = receivedJson[index].Product
                            def productVersion = receivedJson[index].Version

                            channelDetails.each {
                                String jobName = ""
                                String productNameVersionChannel = productName + "-" + productVersion + "-" + it.channel
                                def hasChildJob = false

                                if (childJobNameList.contains(jobName)) {
                                    hasChildJob = true
                                } else {
                                    println "Child job [" + jobName + "] not found."
                                }

                                def packUrl = it.updateFileUrl
                                def packName = it.updateFileName

                                def buildDetails = jobName + " | " + packUrl + " | " + packName

                                if (jobName != null && packUrl != null && packName != null && hasChildJob) {

                                    println buildDetails

                                    def jobNameWithFolder = "wum3/" + jobName

                                    // ******************** Change jobName to jobNameWithFolder
                                    branches["branch:" + jobName] = {
                                        results.add(jobName + ":" + build(job: jobName,
                                                parameters: [[$class: 'StringParameterValue', name: 'UPDATE_FILE_URL', value: packUrl],
                                                             [$class: 'StringParameterValue', name: 'UPDATE_FILE_NAME', value: packName]],
                                                propagate: false).result)

                                    }

                                    jobProductNameMap.put(jobName, productNameVersionChannel)

                                    println "Added Parallel Branch: " + jobName + "\n"
                                } else {
                                    println "Unable to Add Parallel Branch: " + buildDetails + "\n"
                                }
                            }
                        }

                        parallel branches
                        sleep(10)
                        def map = [:]

                        results.each {
                            def jobStatus = it.tokenize(':')
                            def jobName = jobStatus[0]
                            def buildStatus = jobStatus[1]
                            if (buildStatus == "FAILURE") {
                                CURRENT_BUILD_JSON = JENKINS_URL + jobName + "/api/json"
                                LAST_BUILD_JSON = JENKINS_URL + jobName + "/lastBuild/api/json"
                                def output = sh returnStdout: true, script: """
                                    set +x
                                    curl -s -H "Authorization: Basic =" ${CURRENT_BUILD_JSON} | \
                                    python -c "import sys, json; print json.load(sys.stdin)['color']"
                                    """
                                def color = output
                                while (color.contains("anime")) {
                                    sleep(10)
                                    color = sh returnStdout: true, script: """
                                        set +x
                                        curl -s -H "Authorization: Basic =" ${CURRENT_BUILD_JSON} | \
                                        python -c "import sys, json; print json.load(sys.stdin)['color']"
                                        """
                                }
                                def newResult = sh returnStdout: true, script: """
                                    set +x
                                    curl -s -H "Authorization: Basic =" ${LAST_BUILD_JSON} | \
                                    python -c "import sys, json; print json.load(sys.stdin)['result']"
                                    """
                                buildStatus = newResult
                            }
                            entry.put('"' + jobProductNameMap.get(jobName) + '"', '"' + buildStatus.trim() + '"')
                        }
                        env.buildResults = entry.toString()
                        println env.buildResults

                    } catch (Exception e) {
                        println "Exception: " + e
                    }
                }
            }
        }
    }
    post {
        success {
            reTrigger()
        }
        
    }

}
