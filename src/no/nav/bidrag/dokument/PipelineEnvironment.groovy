package no.nav.bidrag.dokument

class PipelineEnvironment {

    boolean isChangeOfCode = true
    def buildScript

    String appConfig
    String branchName
    String dockerRepo
    String gitHubProjectName
    String homeFolderJenkins
    String lastCommitter
    String mvnImage
    String mvnVersion
    String nais
    String workspace

    private String imageVersion

    PipelineEnvironment(String gitHubProjectName, String mvnImage) {
        this.gitHubProjectName = gitHubProjectName
        this.mvnImage = mvnImage
    }

    void isNotChangeOfCode() {
        isChangeOfCode = false
    }

    boolean isSnapshot() {
        return mvnVersion.contains("-SNAPSHOT")
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
        return isMaster() ? 'preprod' : fetchEnvironment()
    }

    String naisCluster() {
        return isMaster() ? 'preprod-fss' : 'preprod-fss' // until first version is released, master goes to preprod
    }

    String fetchImageVersion() {
        if (imageVersion == null) {
            String sha = Long.toHexString(System.currentTimeMillis())
            imageVersion = "$mvnVersion-${fetchTagEnvironment()}-$sha"
        }

        return imageVersion
    }

    void println(Object toPrint) {
        buildScript.println(toPrint)
    }

    String createTagName() {
        return "$gitHubProjectName-$mvnVersion-${fetchTagEnvironment()}"
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
