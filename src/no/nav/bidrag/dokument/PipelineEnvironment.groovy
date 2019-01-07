package no.nav.bidrag.dokument

class PipelineEnvironment {

    boolean isChangeOfCode = true
    def buildScript

    String appConfig
    String artifactVersion
    String branchName
    String buildImage
    String dockerRepo
    String gitHubProjectName
    String homeFolderJenkins
    String lastCommitter
    String nais
    String workspace

    private String imageVersion

    PipelineEnvironment(String gitHubProjectName, String buildImage) {
        this.gitHubProjectName = gitHubProjectName
        this.buildImage = buildImage
    }

    void isNotChangeOfCode() {
        isChangeOfCode = false
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
        return isMaster() ? "q0" : isDevelop() ? "q0" : "t0"
    }

    private String fetchTagEnvironment() {
        return isMaster() ? 'preprod' : fetchEnvironment() // until first version is released, master goes to preprod
    }

    String naisCluster() {
        return isMaster() ? 'preprod-fss' : 'preprod-fss' // until first version is released, master goes to preprod
    }

    String fetchImageVersion() {
        if (imageVersion == null) {
            String sha = Long.toHexString(System.currentTimeMillis())
            imageVersion = "$artifactVersion-${fetchTagEnvironment()}-$sha"
        }

        return imageVersion
    }

    void println(Object toPrint) {
        buildScript.println(toPrint)
    }

    String createTagName() {
        return "$gitHubProjectName-$artifactVersion-${fetchTagEnvironment()}"
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
        return (isMaster() || isDevelop()) ? "default" : fetchEnvironment()
    }

    boolean isProd() {
        return naisCluster() == 'prod-fss'
    }

    boolean fileExists(String fileInWorkspace) {
        return new File(workspace, fileInWorkspace).exists()
    }
}
