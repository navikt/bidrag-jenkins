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
        Integer sleepInterval = 15000
        Integer maxRetries = 20

        while(maxRetries-- > 0) {

            pipelineEnvironment.println "#${maxRetries} Checking POD status for app ${app}"

            /*
            NAME                               READY     STATUS    RESTARTS   AGE
            bidrag-dokument-8668bc66c9-8hcf7   1/1       Running   0          20m
            bidrag-dokument-8668bc66c9-mr5b8   1/1       Running   0          19m
            */
            Integer oldpods = 0
            Integer newpods = 0
            String str = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} get pod -l app=${app}", returnStdout:true)
            str.tokenize("\n").each {
                List line = it.tokenize(" ")
                if(line.size() == 5) {
                    String podId = line.get(0)
                    String status = line.get(2)
                    if(status == "Running") {
                        String desc = pipelineEnvironment.buildScript.sh(script: "kubectl -n ${ns} describe pod ${podId}", returnStdout:true)
                        pipelineEnvironment.println desc
                        desc.tokenize("\n").each {
                            // APP_VERSION:                   1.0.176-SNAPSHOT-q0-16899879708
                            if(it.trim().startsWith("APP_VERSION:")) {
                                String appVersion = it.tokenize(':').get(1).trim()
                                if(appVersion != currentImageVersion) {
                                    oldpods++
                                } else {
                                    pipelineEnvironment.println "Fant ${podId} med forventet APP_VERSION ${appVersion}"
                                    newpods++
                                }
                            }
                        }
                        pipelineEnvironment.println "Gamle PODer: ${oldpods}, Nye PODer: ${newpods}"
                    }
                }
            }
            // Vent til alle gamle poder er stoppet og minst en ny går før retur
            if(oldpods == 0 && newpods > 0) {
                return true
            }
            sleep(sleepInterval)
        }
        return false
    }

    String runCucumberTests() {
        String result = 'SUCCESS'

        // Only throw an exception when cucumber miss json file, else only fail this step
        pipelineEnvironment.println("[INFO] Run cucumber tests")

        pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088',
            'NO_PROXY=localhost,127.0.0.1,10.33.43.41,.local,.adeo.no,.nav.no,.devillo.no,.oera.no,.nais.preprod.local,.nais-iapp.preprod.local,.nais.oera-q.local']) {
            if(!waitForCurrentBuildToDeploy()) {
                throw new IllegalStateException("Timeout waiting for current build to deploy")
            }
        }
        try {
            pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                try {
                    runBidragCucumberWithDocker()
                } catch (Exception e) {
                    pipelineEnvironment.println('Unstable build: ' + e)
                    result = 'UNSTABLE'
                }
            }

            writeCucumberReports()
        } catch (Exception e ) {
            pipelineEnvironment.println("Failed build: " + e)
            result = 'FAIL'
        }

        return result
    }

    private void runBidragCucumberWithDocker() {
        // Checkout bidrag-cucumber -> ./bidrag-cucumber
        // GitHubArtifact will do the magic of matching current branch to bidrag-cucumber branch
        pipelineEnvironment.checkoutCucumberFeatureOrUseMaster()

        // Set 'project' env variable to select features prefixed with project name
        pipelineEnvironment.buildScript.withCredentials([
                [$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD'],
                [$class: 'UsernamePasswordMultiBinding', credentialsId: 'testUser', usernameVariable: 'TEST_USER', passwordVariable: 'TEST_PASS']
            ]) {
            pipelineEnvironment.buildScript.dir('bidrag-cucumber') {
                pipelineEnvironment.execute('npm install')
            }
            pipelineEnvironment.execute(
                    "docker run --rm -e environment=${pipelineEnvironment.fetchEnvironment()} " +
                            "-e NODE_TLS_REJECT_UNAUTHORIZED=0 " +
                            "-e fasit_user=${pipelineEnvironment.buildScript.USERNAME} -e fasit_pass='${pipelineEnvironment.buildScript.PASSWORD}' " +
                            "-e test_user=${pipelineEnvironment.buildScript.TEST_USER} -e test_pass='${pipelineEnvironment.buildScript.TEST_PASS}' " +
                            "-e project=${pipelineEnvironment.gitHubProjectName}. " +
                            "-v ${pipelineEnvironment.workspace}/bidrag-cucumber:/src -w /src node:latest npm start"
            )
        }
    }

    private void writeCucumberReports() {
        pipelineEnvironment.buildScript.cucumber buildStatus: 'UNSTABLE',
                fileIncludePattern: 'bidrag-cucumber/cucumber/*.json',
                trendsLimit: 10,
                classifications: [
                        [
                                'key'  : 'Browser',
                                'value': 'Firefox'
                        ]
                ]
    }
}
