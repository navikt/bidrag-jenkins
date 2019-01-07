package no.nav.bidrag.dokument

abstract class GitHubArtifact {
    protected PipelineEnvironment pipelineEnvironment

    GitHubArtifact(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void checkout(String branch) {
        String gitHubProjectName = pipelineEnvironment.gitHubProjectName

        pipelineEnvironment.buildScript.cleanWs()
        pipelineEnvironment.buildScript.withCredentials([pipelineEnvironment.buildScript.string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
            pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                pipelineEnvironment.buildScript.sh(script: "git clone https://${pipelineEnvironment.buildScript.token}:x-oauth-basic@github.com/navikt/${gitHubProjectName}.git .")
                pipelineEnvironment.buildScript.sh "echo '****** BRANCH ******'"
                pipelineEnvironment.buildScript.sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                pipelineEnvironment.buildScript.sh(script: "git checkout ${branch}")
            }
        }
    }

    abstract def fetchBuildDescriptor()

    abstract def fetchBuildDescriptorFromSourceCode()

    String fetchVersion() {
        return fetchBuildDescriptor().version
    }

    String fetchMajorVersion() {
        return fetchMajorVersion(fetchBuildDescriptor())
    }

    static String fetchMajorVersion(def buildDescriptor) {
        String releaseVersion = buildDescriptor.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        return "${tokens[0]}.${tokens[1]}"
    }

    String fetchMinorVersion() {
        return fetchMinorVersion(fetchBuildDescriptor())
    }

    static String fetchMinorVersion(def buildDescriptor) {
        String releaseVersion = buildDescriptor.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        return "${tokens[2]}"
    }

    boolean isLastCommitterFromPipeline() {
        pipelineEnvironment.lastCommitter = pipelineEnvironment.buildScript.sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
        String commitMessage = pipelineEnvironment.buildScript.sh(script: 'git log -1 --pretty=oneline', returnStdout: true).trim()
        pipelineEnvironment.println("last commit done by ${pipelineEnvironment.lastCommitter}")
        pipelineEnvironment.println(commitMessage)

        return pipelineEnvironment.lastCommitter.contains('navikt-ci')
    }

    void updateMinorVersion(Builder builder) {
        String majorVersion = fetchMajorVersion()
        String minorVersion = fetchMinorVersion()
        String nextVersion = "${majorVersion}." + (minorVersion.toInteger() + 1) + "-SNAPSHOT"

        builder.updateVersion(nextVersion)
        pipelineEnvironment.buildScript.sh "git commit -a -m \"updated to new minor version ${nextVersion} after release by ${pipelineEnvironment.lastCommitter}\""
        pipelineEnvironment.buildScript.sh "git push"

        pipelineEnvironment.artifactVersion = nextVersion
    }

    void updateMajorVersion(Builder builder) {
        String masterMajorVersion = fetchMajorVersion()
        String developMajorVersion = fetchMajorVersion(fetchBuildDescriptorFromSourceCode())

        // only bump major version if not previously bumped...
        if (masterMajorVersion == developMajorVersion) {
            String developMinorVersion = fetchMinorVersion(fetchBuildDescriptorFromSourceCode())
            String nextVersion = (masterMajorVersion.toFloat() + 1) + ".${developMinorVersion}-SNAPSHOT"
            pipelineEnvironment.execute("echo", "[INFO] bumping major version in develop ($developMajorVersion) from version in master ($masterMajorVersion)")

            builder.updateVersion(nextVersion)
            pipelineEnvironment.buildScript.sh "git commit -a -m \"updated to new major version ${nextVersion} after release by ${pipelineEnvironment.lastCommitter}\""
            pipelineEnvironment.buildScript.sh "git push"

            pipelineEnvironment.artifactVersion = nextVersion.replace("-SNAPSHOT", "")
        } else {
            pipelineEnvironment.println("[INFO] do not bump major version in develop ($developMajorVersion) from version in master ($masterMajorVersion)")
        }
    }

    void resetWorkspace() {
        pipelineEnvironment.execute("cd ${pipelineEnvironment.workspace}")
        pipelineEnvironment.execute("git reset --hard")
    }
}
