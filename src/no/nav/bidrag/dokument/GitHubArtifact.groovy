package no.nav.bidrag.dokument

class GitHubArtifact {
    def pom
    def script
    private String branch
    private String gitHubProjectName

    GitHubArtifact(script, String gitHubProjectName, String branch) {
        this.branch = branch
        this.gitHubProjectName = gitHubProjectName
        this.script = script
    }

    def checkout() {
        script.cleanWs()
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
        if (pom == null) {
            pom = script.readMavenPom file: './pom.xml'
        }

        return pom
    }

}
