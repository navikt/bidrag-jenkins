package no.nav.bidrag.dokument

class Cucumber {
    static def runCucumberTests(script) {
        if (fileExists('cucumber')) {
            println("[INFO] Run cucumber tests")
            sleep(20)
            try {
                script.sh "docker run --rm -v ${env.WORKSPACE}/cucumber:/cucumber bidrag-dokument-cucumber"
            } catch (e) {
                result = 'UNSTABLE'
            }
            if (fileExists('cucumber/cucumber.json')) {
                cucumber buildStatus: 'UNSTABLE',
                        fileIncludePattern: 'cucumber/*.json',
                        trendsLimit: 10,
                        classifications: [
                                [
                                        'key'  : 'Browser',
                                        'value': 'Firefox'
                                ]
                        ]
            } else {
                throw e
            }
        } else {
            println("[WARN] No cucumber directory - no tests to run!")
        }
    }
}
