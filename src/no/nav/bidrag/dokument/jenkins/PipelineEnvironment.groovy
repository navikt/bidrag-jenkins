package no.nav.bidrag.dokument.jenkins

import no.nav.bidrag.dokument.jenkins.maven.GitHubMavenArtifact
import no.nav.bidrag.dokument.jenkins.maven.MavenBuilder

import no.nav.bidrag.dokument.jenkins.node.GitHubNodeArtifact
import no.nav.bidrag.dokument.jenkins.node.NodeBuilder

class PipelineEnvironment {

    boolean canRunPipeline = true
    def buildScript

    String appConfig
    String artifactVersion
    String branchName
    String buildImage
    String buildType
    String dockerRepo
    String environment
    String gitHubProjectName
    String homeFolderJenkins
    String lastCommitter
    String nais
    String workspace

    private String imageVersion

    PipelineEnvironment(String gitHubProjectName, String buildImage, String environment, String buildType) {
        this.gitHubProjectName = gitHubProjectName
        this.buildImage = buildImage
        this.environment = environment
        this.buildType = buildType
    }

    void doNotRunPipeline() {
        canRunPipeline = false
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
        if (environment != null) {
            return environment
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
        return fetchEnvironment() == "q0" ? "default" : fetchEnvironment()
    }

    private boolean isProd() {
        return naisCluster() == 'prod-fss'
    }

    boolean fileExists(String fileInWorkspace) {
        return new File(workspace, fileInWorkspace).exists()
    }

    boolean canRunPipelineOnDevelop() {
        return canRunPipeline && branchName == 'develop'
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
        if (buildType == null || buildType == 'maven') {
            return new GitHubMavenArtifact(this)
        }

        if (buildType == 'node') {
            return new GitHubNodeArtifact(this)
        }

        throw new IllegalStateException("unknown build type: " + buildType)
    }

    static boolean isAutmatedBuild(def userId) {
        return userId == null
    }
}
