package no.nav.bidrag.dokument

class GitHubArtifact {
    private def multibranchPipeline
    private def pom
    private String branch
    private String gitHubProjectName
    private String lastCommitter
    private String workspace

    GitHubArtifact(multibranchPipeline, String gitHubProjectName, String branch, String workspace) {
        this.branch = branch
        this.gitHubProjectName = gitHubProjectName
        this.multibranchPipeline = multibranchPipeline
        this.workspace = workspace
    }

    GitHubArtifact(GitHubArtifact gitHubArtifact, String branch) {
        this.branch = branch
        this.gitHubProjectName = gitHubArtifact.gitHubProjectName
        this.lastCommitter = gitHubArtifact.lastCommitter
        this.multibranchPipeline = gitHubArtifact.multibranchPipeline
        this.pom = gitHubArtifact.pom
        this.workspace = gitHubArtifact.workspace
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
        lastCommitter =  multibranchPipeline.sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
        execute( "echo", "last committ done by $lastCommitter")

        return lastCommitter.contains('navikt-ci')
    }

    void updateMinorVersion(String homeFolderInJenkins, String mvnImage) {
        String majorVersion = fetchMajorVersion()
        String minorVersion = fetchMinorVersion()
        String nextVersion = "${majorVersion}." + (minorVersion.toInteger() + 1) + "-SNAPSHOT"
        updateVersion(homeFolderInJenkins, mvnImage, nextVersion)
   }

    void updateMajorVersion(String homeFolderInJenkins, String mvnImage) {
        String majorVersion = fetchMajorVersion()
        String minorVersion = fetchMinorVersion()
        String nextVersion = (majorVersion.toFloat() + 1) + ".${minorVersion}-SNAPSHOT"
        updateVersion(homeFolderInJenkins, mvnImage, nextVersion)
    }

    private void updateVersion(String homeFolderInJenkins, String mvnImage, String nextVersion) {
        multibranchPipeline.sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$homeFolderInJenkins/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
        multibranchPipeline.sh "git commit -a -m \"updated to new dev-major-version ${nextVersion} after release by ${lastCommitter}\""
        multibranchPipeline.sh "git push"
    }
}
