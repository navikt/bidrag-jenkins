package no.nav.bidrag.dokument

class GitHubArtifact {
    def script
    def token
    private String branch
    private String gitHubProjectName
    private String workspace

    GitHubArtifact(script, String workspace, String gitHubProjectName, token, String branch) {
        this.branch = branch;
        this.gitHubProjectName = gitHubProjectName
        this.script = script
        this.token = token
        this.workspace = workspace
    }

    def checkout() {
        script.withCredentials([script.string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
            script.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                script.sh(script: "git clone https://${script.token}:x-oauth-basic@github.com/navikt/${gitHubProjectName}.git .")
                script.sh "echo '****** BRANCH ******'"
                script.sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                script.sh(script: "git checkout ${branch}")
            }
        }
    }

    def fetchPom() {
         script.readMavenPom file: "$workspace/pom.xml"
    }

}
