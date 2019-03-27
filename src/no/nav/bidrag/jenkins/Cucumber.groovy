package no.nav.bidrag.jenkins

class Cucumber {
    private PipelineEnvironment pipelineEnvironment

    Cucumber(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    String runCucumberTests() {
        String result = 'SUCCESS'

        // Only throw an exception when cucumber miss json file, else only fail this step
        pipelineEnvironment.println("[INFO] Run cucumber tests")
        sleep(10000)

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
                            "-v ${pipelineEnvironment.workspace}/bidrag-cucumber:/src -w src node:latest npm start"
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
