package no.nav.bidrag.dokument

class GitHubArtifact {
    private def multibranchPipeline
    private def pom
    private String branch
    private String gitHubProjectName
    private String workspace

    GitHubArtifact(multibranchPipeline, String gitHubProjectName, String branch, String workspace) {
        this.branch = branch
        this.gitHubProjectName = gitHubProjectName
        this.multibranchPipeline = multibranchPipeline
        this.workspace = workspace
    }

    def checkout() {
        multibranchPipeline.cleanWs()
        multibranchPipeline.withCredentials([multibranchPipeline.string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
            multibranchPipeline.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                multibranchPipeline.sh(script: "git clone https://${multibranchPipeline.token}:x-oauth-basic@github.com/navikt/${gitHubProjectName}.git .")
                multibranchPipeline.sh "echo '****** BRANCH ******'"
                multibranchPipeline.sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                multibranchPipeline.sh(script: "git checkout ${branch}")
            }
        }
    }

    def fetchPom() {
        multibranchPipeline.sh "parsing pom.xml from ${workspace}"

        if (pom == null) {
            pom = multibranchPipeline.readMavenPom file: 'pom.xml'
        }

        return pom
    }

    def debugCommand(String command, String message) {
        multibranchPipeline.sh(command + " $message")
    }

    String toString() {
       multibranchPipeline.sh("cloned to: `pwd`")
    }

}
