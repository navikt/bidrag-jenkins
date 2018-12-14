package no.nav.bidrag.dokument

class GitHubArtifact {
    def multibranchPipeline
    def pom
    private String branch
    private String gitHubProjectName

    GitHubArtifact(multibranchPipeline, String gitHubProjectName, String branch) {
        this.branch = branch
        this.gitHubProjectName = gitHubProjectName
        this.multibranchPipeline = multibranchPipeline
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

}
