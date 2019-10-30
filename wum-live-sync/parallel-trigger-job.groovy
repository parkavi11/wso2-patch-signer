import groovy.json.JsonSlurperClassic

pipeline {

    agent { label 'Jenkins-AWS01' }
    stages {
        stage('Build') {
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

                        def branches = [:]
                        def results = []

                        for (int index = 0; index < receivedJson.size(); index++) {

                            def channelDetails = receivedJson[index].ChannelViceDetails

                            channelDetails.each {
                                String jobName = it.jobName
                                def hasChildJob = false

                                if (jobName != null) {
                                    hasChildJob = true
                                } else {
                                    println "Child job [" + jobName + "] not found."
                                }

                                def packUrl = it.updateFileUrl
                                def packName = it.updateFileName
                                def buildDetails = jobName + " | " + packUrl + " | " + packName

                                if (jobName != null && packUrl != null && packName != null && hasChildJob) {

                                    println buildDetails

                                    branches["branch:" + jobName] = {
                                        results.add(jobName + ":" + build(job: "wum3/" + jobName,
                                            parameters: [[$class: 'StringParameterValue', name: 'UPDATE_FILE_URL', value: packUrl],
                                                         [$class: 'StringParameterValue', name: 'UPDATE_FILE_NAME', value: packName]],
                                            propagate: false).result)
                                    };
                                    println "Added Parallel Branch: " + jobName + "\n"

                                } else {
                                    println "Unable to Add Parallel Branch: " + buildDetails + "\n"
                                }
                            }
                        }
                        parallel branches
                        println results

                    } catch (Exception e) {
                        println "Exception: " + e
                    }
                }
            }
        }
    }
}
