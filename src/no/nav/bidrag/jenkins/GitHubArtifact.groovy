package no.nav.bidrag.jenkins

abstract class GitHubArtifact {
    protected def buildDescriptor
    protected PipelineEnvironment pipelineEnvironment

    GitHubArtifact(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void checkout() {
        checkout(pipelineEnvironment.branchName)
    }

    void checkout(String branch) {
        String gitHubProjectName = pipelineEnvironment.gitHubProjectName

        pipelineEnvironment.buildScript.cleanWs()
        // pipelineEnvironment.buildScript.withCredentials([pipelineEnvironment.buildScript.string(credentialsId: 'OAUTH_TOKEN', variable: 'token')]) {
        pipelineEnvironment.buildScript.withCredentials(
                [[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkinsPipeline', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                pipelineEnvironment.buildScript.sh(script: "git clone https://${pipelineEnvironment.buildScript.USERNAME}:${pipelineEnvironment.buildScript.PASSWORD}@github.com/navikt/${gitHubProjectName}.git .")
                pipelineEnvironment.buildScript.sh "echo '****** BRANCH ******'"
                pipelineEnvironment.buildScript.sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                pipelineEnvironment.buildScript.sh "echo 'BRANCH CHECKOUT: ${branch}'......"
                pipelineEnvironment.buildScript.sh(script: "git checkout ${branch}")
            }
        }
    }

    void checkoutCucumberFeatureOrUseMaster(String branch) {
        pipelineEnvironment.buildScript.withCredentials(
                [[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkinsPipeline', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                pipelineEnvironment.buildScript.sh(script:
                        "cloneBidragCucumberBranch.sh ${pipelineEnvironment.buildScript.USERNAME} ${pipelineEnvironment.buildScript.PASSWORD} ${branch}"
                )
            }
        }
    }

    def fetchBuildDescriptor() {
        if (buildDescriptor == null) {
            buildDescriptor = readBuildDescriptorFromSourceCode()
        }

        return buildDescriptor
    }

    abstract def readBuildDescriptorFromSourceCode()

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

    String fetchMinorVersion(def buildDescriptor) {
        pipelineEnvironment.println("buildDescriptor: $buildDescriptor")
        String releaseVersion = buildDescriptor.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        pipelineEnvironment.println("fetching minor version from $releaseVersion")

        return "${tokens[2]}"
    }

    boolean isLastCommitterFromPipeline() {
        String commitMessage = pipelineEnvironment.buildScript.sh(script: 'git log -1 --pretty=oneline', returnStdout: true).trim()
        pipelineEnvironment.println("last commit done by ${fetchLastCommitter()}")
        pipelineEnvironment.println(commitMessage)

        return pipelineEnvironment.lastCommitter.contains('navikt-ci')
    }

    String fetchLastCommitter() {
        if (pipelineEnvironment.lastCommitter == null) {
            pipelineEnvironment.lastCommitter = pipelineEnvironment.buildScript.sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
        }

        return pipelineEnvironment.lastCommitter
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
        String developMajorVersion = fetchMajorVersion(readBuildDescriptorFromSourceCode())

        // only bump major version if not previously bumped...
        if (masterMajorVersion == developMajorVersion) {
            String nextVersion = (masterMajorVersion.toFloat() + 1) + ".0-SNAPSHOT"
            pipelineEnvironment.execute("echo", "[INFO] bumping major version in develop ($developMajorVersion) from version in master ($masterMajorVersion)")

            builder.updateVersion(nextVersion)
            pipelineEnvironment.buildScript.sh "git commit -a -m \"updated to new major version ${nextVersion} after release by ${fetchLastCommitter()}\""
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
