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
        if (pom == null) {
            pom = readPomFromSourceCode()
        }

        return pom
    }

    private def readPomFromSourceCode() {
        execute("echo", "parsing pom.xml from ${workspace}")
        def pom = multibranchPipeline.readMavenPom file: 'pom.xml'

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
        return fetchMajorVersion(fetchPom())
    }

    static String fetchMajorVersion(def pom) {
        String releaseVersion = pom.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        return "${tokens[0]}.${tokens[1]}"
    }

    String fetchMinorVersion() {
        return fetchMinorVersion(fetchPom())
    }

    static String fetchMinorVersion(def pom) {
        String releaseVersion = pom.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        return "${tokens[2]}"
    }

    boolean isLastCommitterFromPipeline() {
        lastCommitter = multibranchPipeline.sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
        execute("echo", "last committ done by $lastCommitter")

        return lastCommitter.contains('navikt-ci')
    }

    void updateMinorVersion(String homeFolderInJenkins, String mvnImage) {
        String majorVersion = fetchMajorVersion()
        String minorVersion = fetchMinorVersion()
        String nextVersion = "${majorVersion}." + (minorVersion.toInteger() + 1) + "-SNAPSHOT"
        multibranchPipeline.sh "docker run --rm -v ${workspace}:/usr/src/mymaven -w /usr/src/mymaven -v '$homeFolderInJenkins/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
        multibranchPipeline.sh "git commit -a -m \"updated to new minor version ${nextVersion} after release by ${lastCommitter}\""
        multibranchPipeline.sh "git push"
    }

    void updateMajorVersion(String homeFolderInJenkins, String mvnImage) {
        String masterMajorVersion = fetchMajorVersion()
        String developMajorVersion = fetchMajorVersionFromDevelopBranch()

        // only bump major version if not previously bumped...
        if (masterMajorVersion == developMajorVersion) {
            String developMinorVersion = fetchMinorVersion(readPomFromSourceCode())
            String nextVersion = (masterMajorVersion.toFloat() + 1) + ".${developMinorVersion}-SNAPSHOT"
            multibranchPipeline.sh "docker run --rm -v ${workspace}:/usr/src/mymaven -w /usr/src/mymaven -v '$homeFolderInJenkins/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
            multibranchPipeline.sh "git commit -a -m \"updated to new major version ${nextVersion} after release by ${lastCommitter}\""
            multibranchPipeline.sh "git push"
        }
    }

    private String fetchMajorVersionFromDevelopBranch() {
        if (branch != 'develop') {
            throw new IllegalStateException("Expected branch to be develop, was: $branch")
        }

        return fetchMajorVersion(readPomFromSourceCode())
    }
}
