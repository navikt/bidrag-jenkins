package no.nav.bidrag.dokument

class GitHubArtifact {
    private def multibranchPipeline
    private def pom
    private String branch
    private String gitHubProjectName
    private String workspace
    public String lastCommitter

    GitHubArtifact(multibranchPipeline, String gitHubProjectName, String branch, String workspace) {
        this.branch = branch
        this.gitHubProjectName = gitHubProjectName
        this.multibranchPipeline = multibranchPipeline
        this.workspace = workspace
    }

    void checkout() {
        multibranchPipeline.cleanWs()
        multibranchPipeline.withCredentials([multibranchPipeline.string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
            multibranchPipeline.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                multibranchPipeline.sh(script: "git clone https://${multibranchPipeline.token}:x-oauth-basic@github.com/navikt/${gitHubProjectName}.git .")
                multibranchPipeline.sh "echo '****** BRANCH ******'"
                multibranchPipeline.sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                multibranchPipeline.sh(script: "git checkout ${branch}")
            }
        }

        lastCommitter =  multibranchPipeline.sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
    }

    def fetchPom() {
        execute("echo", "parsing pom.xml from ${workspace}")

        if (pom == null) {
            pom = multibranchPipeline.readMavenPom file: 'pom.xml'
        }

        return pom
    }

    void execute(String command) {
        multibranchPipeline.sh("$command")
    }

    void execute(String command, String quotedArgs) {
        multibranchPipeline.sh("$command \"$quotedArgs\"")
    }

    String workspaceFolder() {
        return "${workspace}"
    }

    boolean isSnapshot() {
        return pom.version.contains("-SNAPSHOT")
    }

    String fetchMajorVersion() {
        String releaseVersion = pom.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        return "${tokens[0]}.${tokens[1]}"
    }

    String fetchMinorVersion() {
        String releaseVersion = pom.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        return "${tokens[2]}"
    }

    boolean isLastCommitterFromPipeline() {
        return lastCommitter == 'navikt-ci'
    }
}
