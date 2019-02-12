package no.nav.bidrag.jenkins

import no.nav.bidrag.jenkins.maven.GitHubMavenArtifact
import no.nav.bidrag.jenkins.maven.MavenBuilder

import no.nav.bidrag.jenkins.node.GitHubNodeArtifact
import no.nav.bidrag.jenkins.node.NodeBuilder

class PipelineEnvironment {

    boolean canRunPipeline = true
    def buildScript

    String appConfig
    String artifactVersion
    String branchName
    String buildImage
    String buildType
    String dockerRepo
    String environmentInDevelopBranch
    String gitHubProjectName
    String homeFolderJenkins
    String lastCommitter
    String naisBinary
    String workspace

    private GitHubArtifact githubArtifact
    private String buildId
    private String imageVersion
    private def build

    PipelineEnvironment(String gitHubProjectName, String buildImage, String environmentInDevelopBranch, String buildType) {
        this.gitHubProjectName = gitHubProjectName
        this.buildImage = buildImage
        this.environmentInDevelopBranch = environmentInDevelopBranch
        this.buildType = buildType
    }

    void doNotRunPipeline(String buildId) {
        canRunPipeline = false
        this.buildId = buildId
        println("will delete build $buildId on $gitHubProjectName/$branchName")
    }

    void deleteBuildWhenPipelineIsNotExecuted(def items) {
        if (!canRunPipeline && buildId != null) {
            fetchBuild(items)
            build.delete()
        }
    }

    void fetchBuild(def items) {
        items.each {
            println("Item: $it")

            if (it.name == gitHubProjectName) {
                def buildFolder = it.getItemByBranchName(branchName)
                println("branch: $buildFolder")
                fetchBuildById(buildFolder, buildId)
            }
        }

        if (build == null) {
            throw new IllegalStateException("unable to find $buildId on $gitHubProjectName/$branchName")
        }
    }

    private void fetchBuildById(def buildFolder, String buildId) {
        buildFolder.getBuilds().each {
            if (it.getId() == buildId) {
                println("build: $it, result=$it.result, id=$it.id")
                build = it
            }
        }
    }

    boolean isSnapshot() {
        return artifactVersion.contains("-SNAPSHOT")
    }

    void execute(String command) {
        buildScript.sh("$command")
    }

    void execute(String command, String quotedArgs) {
        buildScript.sh("$command \"$quotedArgs\"")
    }

    String fetchEnvironment() {
        if (environmentInDevelopBranch != null && isDevelop()) {
            return environmentInDevelopBranch
        }

        return isMaster() ? "q4" : isDevelop() ? "q0" : "q1"
    }

    String naisCluster() {
        return isMaster() ? 'preprod-fss' : 'preprod-fss' // until first version is released, master goes to preprod
    }

    String fetchImageVersion() {
        if (imageVersion == null) {
            String sha = Long.toHexString(System.currentTimeMillis())
            imageVersion = "$artifactVersion-${fetchEnvironment()}-$sha"
        }

        return imageVersion
    }

    String fetchStableVersion() {
        return artifactVersion.replace("-SNAPSHOT", "")
    }

    void println(Object toPrint) {
        buildScript.println(toPrint)
    }

    String createTagName() {
        return "$gitHubProjectName-$artifactVersion-${fetchEnvironment()}"
    }

    boolean canTagGitHubArtifact() {
        String existingTag = buildScript.sh(script: "git tag -l ${createTagName()}", returnStdout: true).trim()

        return (isMaster() || isDevelop()) && existingTag == ""
    }

    boolean isDevelop() {
        return branchName == "develop"
    }

    boolean isMaster() {
        return branchName == "master"
    }

    String fetchNamespace() {
        return isDevelop() ? "default" : fetchEnvironment()
    }

    private boolean isProd() {
        return naisCluster() == 'prod-fss'
    }

    boolean canRunPipelineOnDevelop(boolean isLastCommitFromPipeline) {
        return canRunPipeline && branchName == 'develop' && !isLastCommitFromPipeline
    }

    boolean canRunPipelineOnMaster() {
        return canRunPipeline && isMaster()
    }

    boolean canRunPipelineOnMasterAndNaisClusterIsProdFss() {
        return canRunPipelineOnMaster() && isProd()
    }

    Builder initBuilder() {
        if (buildType == null || buildType == 'maven') {
            return new MavenBuilder(this)
        }

        if (buildType == 'node') {
            return new NodeBuilder(this)
        }

        throw new IllegalStateException("unknown build type: " + buildType)
    }

    GitHubArtifact initGitHubArtifact() {
        if (githubArtifact == null) {
            if (buildType == null || buildType == 'maven') {
                githubArtifact = new GitHubMavenArtifact(this)
            }

            if (buildType == 'node') {
                githubArtifact = new GitHubNodeArtifact(this)
            }
        }

        if (githubArtifact != null) {
            return githubArtifact
        }

        throw new IllegalStateException("unknown build type: " + buildType)
    }

    static boolean isAutmatedBuild(def userId) {
        return userId == null
    }

    void checkoutCucumberFeatureOrUseMaster() {
        initGitHubArtifact().checkoutCucumberFeatureOrUseMaster(branchName)
    }
}
