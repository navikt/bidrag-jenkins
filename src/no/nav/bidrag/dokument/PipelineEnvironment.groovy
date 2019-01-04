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

    String naisCluster() {
        return isMaster() ? 'preprod-fss' : 'preprod-fss'
    }

    String fetchImageVersion() {
        if (imageVersion == null) {
            String sha = buildScript.sh(script: "git --no-pager log -1 --pretty=%h", returnStdout: true).trim()
            imageVersion = "$mvnVersion-${fetchEnvironment()}-$sha"
        }

        return imageVersion
    }

    void println(Object toPrint) {
        buildScript.println(toPrint)
    }

    String createTagName() {
        return "$gitHubProjectName-$mvnVersion-${fetchEnvironment()}"
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
}
