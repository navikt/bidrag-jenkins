package no.nav.bidrag.jenkins

class Cucumber {
    private PipelineEnvironment pipelineEnvironment

    Cucumber(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    String runCucumberTests() {
        String result = 'SUCCESS'

        // Only throw an exception when cucumber miss json file, else only fail this step
        try {
            pipelineEnvironment.println("[INFO] Run cucumber tests")
            sleep(10000)

            pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {

                try {
                    runBidragCucumberWithDocker()
                } catch (Exception e) {
                    pipelineEnvironment.println('Unstable build: ' + e)
                    result = 'UNSTABLE'
                }

                writeCucumberReports()
            } catch (Exception e ) {
                pipelineEnvironment.println("Failed build: " + e)
                result = 'FAIL'
            }

            return result
        }
    }

    private void runBidragCucumberWithDocker() {
        // Checkout bidrag-cucumber -> ./bidrag-cucumber
        // GitHubArtifact will do the magic of matching current branch to bidrag-cucumber branch
        pipelineEnvironment.checkoutCucumberFeatureOrUseMaster()

        // Instead of linking ./cucumber to the container link the project specific sub-dir to run test for current project (-v param)
        pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            pipelineEnvironment.execute(
                    "docker run --rm -e environment=${pipelineEnvironment.fetchEnvironment()} " +
                            "-e fasit_user=${pipelineEnvironment.buildScript.USERNAME} -e fasit_pass='${pipelineEnvironment.buildScript.PASSWORD}' " +
                            "-v ${pipelineEnvironment.workspace}/bidrag-cucumber/cucumber/${pipelineEnvironment.gitHubProjectName}:/cucumber bidrag-cucumber"
            )
        }
    }

    private void writeCucumberReports() {
        pipelineEnvironment.buildScript.cucumber buildStatus: 'UNSTABLE',
                fileIncludePattern: 'cucumber/*.json',
                trendsLimit: 10,
                classifications: [
                        [
                                'key'  : 'Browser',
                                'value': 'Firefox'
                        ]
                ]
    }
