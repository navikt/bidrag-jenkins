package no.nav.bidrag.dokument

class Cucumber {
    private PipelineEnvironment pipelineEnvironment

    Cucumber(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    String runCucumberTests() {
        String result = 'SUCCESS'

        // Only throw an exception when cucumber miss json file, else only fail this step
        try {
            if (pipelineEnvironment.fileExists('cucumber')) {
                pipelineEnvironment.println("[INFO] Run cucumber tests")
                sleep(10)

                try {
                    pipelineEnvironment.buildScript.sh "docker run -e environment=${pipelineEnvironment.fetchEnvironment()} -v ${pipelineEnvironment.workspace}/cucumber:/cucumber bidrag-dokument-cucumber"
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
