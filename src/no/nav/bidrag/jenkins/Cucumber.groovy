package no.nav.bidrag.jenkins

class Cucumber {
    private PipelineEnvironment pipelineEnvironment

    Cucumber(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    boolean waitForCurrentBuildToDeploy() {
        String app = pipelineEnvironment.gitHubProjectName
        String currentImageVersion = pipelineEnvironment.fetchImageVersion()
        String ns = pipelineEnvironment.fetchNamespace()
        Integer sleepInterval = 5000
        Integer maxRetries = 20

        while(maxRetries-- > 0) {

            println "#${maxRetries} Running ${kubectl}"

            /*
            NAME                               READY     STATUS    RESTARTS   AGE
            bidrag-dokument-8668bc66c9-8hcf7   1/1       Running   0          20m
            bidrag-dokument-8668bc66c9-mr5b8   1/1       Running   0          19m
            */
            Integer tainted = 0
            Integer running = 0
            String str = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} get pod -l app=${app}", returnStdout:true)
            str.tokenize("\n").each {
                List line = it.tokenize(" ")
                if(line.size() == 5) {
                    String podId = line.get(0)
                    String status = line.get(2)
                    if(status == "Running") {
                        String desc = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} describe pod ${podId}", returnStdout:true)
                        desc.tokenize("\n").each {
                            // APP_VERSION:                   1.0.176-SNAPSHOT-q0-16899879708
                            if(it.trim().startsWith("APP_VERSION:")) {
                                String appVersion = it.tokenize(':').get(1).trim()
                                if(appVersion != currentImageVersion) {
                                    println "Fant ${podId} med gammel APP_VERSION ${appVersion}"
                                    tainted++
                                } else {
                                    println "Fant ${podId} med forventet APP_VERSION ${appVersion}"
                                    running++
                                }
                            }
                        }
                    }
                }
            }
            // Vent til alle gamle poder er stoppet og minst en ny går før retur
            if(tainted == 0 && running > 0) {
                return true
            }
            println "Sleeping ${sleepInterval}"
            sleep(sleepInterval)
        }
        return false
    }

    String runCucumberTests() {
        String result = 'SUCCESS'

        // Only throw an exception when cucumber miss json file, else only fail this step
        try {
            if (pipelineEnvironment.fileExists('cucumber')) {
                pipelineEnvironment.println("[INFO] Run cucumber tests")

                pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088',
                    'NO_PROXY=localhost,127.0.0.1,10.33.43.41,.local,.adeo.no,.nav.no,.devillo.no,.oera.no,.nais.preprod.local,.nais-iapp.preprod.local,.nais.oera-q.local']) {
                    if(!waitForCurrentBuildToDeploy()) {
                        throw new IllegalStateException("Timeout waiting for current build to deploy")
                    }
                }

                try {
                    pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                        pipelineEnvironment.execute(
                            "docker run --rm -e environment=${pipelineEnvironment.fetchEnvironment()} " + 
                            "-e fasit_user=${pipelineEnvironment.buildScript.USERNAME} -e fasit_pass='${pipelineEnvironment.buildScript.PASSWORD}' " +
                            "-v ${pipelineEnvironment.workspace}/cucumber:/cucumber bidrag-dokument-cucumber"
                        )
                    }
                } catch (Exception e) {
                    pipelineEnvironment.println('Unstable build: ' + e)
                    result = 'UNSTABLE'
                }

                if (pipelineEnvironment.fileExists('cucumber/cucumber.json')) {
                    pipelineEnvironment.buildScript.cucumber buildStatus: 'UNSTABLE',
                            fileIncludePattern: 'cucumber/*.json',
                            trendsLimit: 10,
                            classifications: [
                                    [
                                            'key'  : 'Browser',
                                            'value': 'Firefox'
                                    ]
                            ]
                } else {
                    throw new IllegalStateException("No cucumber.json in cucumber folder")
                }
            } else {
                pipelineEnvironment.println("[INFO] No cucumber directory - no tests to run!")
            }
        } catch (Exception e) {
            pipelineEnvironment.println("Failed build: " + e)
            result = 'FAIL'
        }

        return result
    }
}
