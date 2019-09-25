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
                pipelineEnvironment.buildScript.sh(script: "git checkout ${branch}")
            }
        }
    }

    void checkoutCucumberFeatureOrUseMaster(String branch) {
        pipelineEnvironment.buildScript.withCredentials(
                [[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkinsPipeline', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {
                pipelineEnvironment.buildScript.sh(script: "${pipelineEnvironment.path_workspace}@libs/bidrag-jenkins/resources/cloneBidragCucumberBranch.sh " +
                        "${pipelineEnvironment.buildScript.USERNAME} ${pipelineEnvironment.buildScript.PASSWORD} ${branch}"
                )
            }
        }
    }

    void checkoutGlobalCucumberFeatureOrUseMaster() {
        pipelineEnvironment.buildScript.sh "echo 'CUCUMBER from github...'"

        pipelineEnvironment.buildScript.withCredentials(
                [[$class: 'UsernamePasswordMultiBinding', credentialsId: 'jenkinsPipeline', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            pipelineEnvironment.buildScript.withEnv(['HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088']) {

                boolean bidragCucumberDoesNotExist = !new File(pipelineEnvironment.path_cucumber).exists()

                if (bidragCucumberDoesNotExist) {
                    pipelineEnvironment.buildScript.sh "ch ${pipelineEnvironment.path_jenkins_workspace}"
                    pipelineEnvironment.buildScript.sh "echo 'CUCUMBER CLONE: ${pipelineEnvironment.path_jenkins_workspace}......'"
                    pipelineEnvironment.buildScript.sh(script: "git clone https://${pipelineEnvironment.buildScript.USERNAME}:${pipelineEnvironment.buildScript.PASSWORD}@github.com/navikt/bidrag-cucumber")
                }

                pipelineEnvironment.buildScript.sh "cd ${pipelineEnvironment.path_cucumber}"
                pipelineEnvironment.buildScript.sh "echo 'CUCUMBER BRANCH CHECKOU (${pipelineEnvironment.branchName})to ${pipelineEnvironment.path_cucumber}...'"
                pipelineEnvironment.buildScript.sh(script: "git pull")
                pipelineEnvironment.buildScript.sh(script: "git checkout ${pipelineEnvironment.branchName}")
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

    String fetchPatchVersion() {
        return fetchPatchVersion(fetchBuildDescriptor())
    }

    String fetchPatchVersion(def buildDescriptor) {
        pipelineEnvironment.println("buildDescriptor: $buildDescriptor")
        String releaseVersion = buildDescriptor.version.tokenize("-")[0]
        def tokens = releaseVersion.tokenize(".")

        pipelineEnvironment.println("fetching patch version from $releaseVersion")

        return "${tokens[2]}"
    }

    boolean isLastCommitterFromPipeline() {
        String commitMessage = pipelineEnvironment.buildScript.sh(script: 'git log -1 --pretty=oneline', returnStdout: true).trim()
        pipelineEnvironment.println("last commit done by ${fetchLastCommitter()}")
        pipelineEnvironment.println(commitMessage)

        return pipelineEnvironment.lastCommitter.contains('navikt-ci')
    }

    boolean isNotLastCommitterFromPipeline() {
        return !isLastCommitterFromPipeline()
    }

    String fetchLastCommitter() {
        if (pipelineEnvironment.lastCommitter == null) {
            pipelineEnvironment.lastCommitter = pipelineEnvironment.buildScript.sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
        }

        return pipelineEnvironment.lastCommitter
    }

    void updatePatchVersion(Builder builder) {
        String majorVersion = fetchMajorVersion()
        String patchVersion = fetchPatchVersion()
        String nextVersion = "${majorVersion}." + (patchVersion.toInteger() + 1) + "-SNAPSHOT"

        builder.updateVersion(nextVersion)
        pipelineEnvironment.buildScript.sh "git commit -a -m \"updated to new patch version ${nextVersion} after release by ${pipelineEnvironment.lastCommitter}\""
        pipelineEnvironment.buildScript.sh "git push"

        pipelineEnvironment.artifactVersion = nextVersion
    }

    void resetWorkspace() {
        pipelineEnvironment.execute("cd ${pipelineEnvironment.path_workspace}")
        pipelineEnvironment.execute("git reset --hard")
    }

    void resetCucumber() {
        pipelineEnvironment.execute("cd ${pipelineEnvironment.path_cucumber}")
        pipelineEnvironment.execute("git reset --hard")
    }
}
