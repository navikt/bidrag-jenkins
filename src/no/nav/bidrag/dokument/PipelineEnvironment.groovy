package no.nav.bidrag.dokument

class PipelineEnvironment {

    boolean isChangeOfCode = true
    boolean isMaster = false
    def buildScript

    String appConfig
    String deploymentArea
    String dockerRepo
    String gitHubProjectName
    String homeFolderJenkins
    String lastCommitter
    String mvnImage
    String mvnVersion
    String nais
    String workspace

    PipelineEnvironment(String gitHubProjectName, String mvnImage) {
        this.gitHubProjectName = gitHubProjectName
        this.mvnImage = mvnImage
    }

    void isNotChangeOfCode() {
        isChangeOfCode = false
    }

    boolean hasDeploymentArea() {
        return deploymentArea != null
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
        return deploymentArea || ( isMaster ? "q0" : "q0" )
    }

    String fetchImageVersion() {
        return "$mvnVersion-${fetchEnvironment()}"
    }

    void println(Object toPrint) {
        buildScript.println(toPrint)
    }
}
