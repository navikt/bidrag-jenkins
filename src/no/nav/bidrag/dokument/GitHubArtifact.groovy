package no.nav.bidrag.dokument

class GitHubArtifact {
    private PipelineEnvironment pipelineEnvironment
    private def pom

    GitHubArtifact(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void checkout(String branch) {
        String gitHubProjectName = pipelineEnvironment.gitHubProjectName

        pipelineEnvironment.multibranchPipeline.cleanWs()
        pipelineEnvironment.multibranchPipeline.withCredentials([pipelineEnvironment.multibranchPipeline.string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
            pipelineEnvironment.multibranchPipeline.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                pipelineEnvironment.multibranchPipeline.sh(script: "git clone https://${pipelineEnvironment.multibranchPipeline.token}:x-oauth-basic@github.com/navikt/${gitHubProjectName}.git .")
                pipelineEnvironment.multibranchPipeline.sh "echo '****** BRANCH ******'"
                pipelineEnvironment.multibranchPipeline.sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                pipelineEnvironment.multibranchPipeline.sh(script: "git checkout ${branch}")
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
        execute("echo", "parsing pom.xml from ${pipelineEnvironment.workspace}")
        def pom = pipelineEnvironment.multibranchPipeline.readMavenPom file: 'pom.xml'

        return pom
    }

    void execute(String command) {
        pipelineEnvironment.multibranchPipeline.sh("$command")
    }

    void execute(String command, String quotedArgs) {
        pipelineEnvironment.multibranchPipeline.sh("$command \"$quotedArgs\"")
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
        pipelineEnvironment.lastCommitter = pipelineEnvironment.multibranchPipeline.sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
        execute("echo", "last commit done by ${pipelineEnvironment.lastCommitter}")

        return pipelineEnvironment.lastCommitter.contains('navikt-ci')
    }

    void updateMinorVersion() {
        String homeFolderInJenkins = pipelineEnvironment.homeFolderJenkins
        String majorVersion = fetchMajorVersion()
        String minorVersion = fetchMinorVersion()
        String mvnImage = pipelineEnvironment.mvnImage
        String nextVersion = "${majorVersion}." + (minorVersion.toInteger() + 1) + "-SNAPSHOT"

        pipelineEnvironment.multibranchPipeline.sh "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven -w /usr/src/mymaven -v '$homeFolderInJenkins/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
        pipelineEnvironment.multibranchPipeline.sh "git commit -a -m \"updated to new minor version ${nextVersion} after release by ${pipelineEnvironment.lastCommitter}\""
        pipelineEnvironment.multibranchPipeline.sh "git push"
    }

    void updateMajorVersion() {
        String homeFolderInJenkins = pipelineEnvironment.homeFolderJenkins
        String masterMajorVersion = fetchMajorVersion()
        String developMajorVersion = readPomFromSourceCode()

        // only bump major version if not previously bumped...
        if (masterMajorVersion == developMajorVersion) {
            String developMinorVersion = fetchMinorVersion(readPomFromSourceCode())
            String mvnImage = pipelineEnvironment.mvnImage
            String nextVersion = (masterMajorVersion.toFloat() + 1) + ".${developMinorVersion}-SNAPSHOT"
            execute("echo", "[INFO] bumping major version in develop ($developMajorVersion) from version in master ($masterMajorVersion)")

            pipelineEnvironment.multibranchPipeline.sh "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven -w /usr/src/mymaven -v '$homeFolderInJenkins/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
            pipelineEnvironment.multibranchPipeline.sh "git commit -a -m \"updated to new major version ${nextVersion} after release by ${pipelineEnvironment.lastCommitter}\""
            pipelineEnvironment.multibranchPipeline.sh "git push"
        } else {
            execute("echo", "[INFO] do not bump major version in develop ($developMajorVersion) from version in master ($masterMajorVersion)")
        }
    }

    void resetWorkspace() {
        execute("cd ${pipelineEnvironment.workspace}")
        execute("git reset --hard")
    }
}
