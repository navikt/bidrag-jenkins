package no.nav.bidrag.dokument

class PipelineEnvironment {

    boolean isChangeOfCode = true
    def multibranchPipeline

    String deploymentArea
    String gitHubProjectName
    String homeFolderJenkins
    String lastCommitter
    String mvnImage
    String mvnVersion
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
        multibranchPipeline.sh("$command")
    }

    void execute(String command, String quotedArgs) {
        multibranchPipeline.sh("$command \"$quotedArgs\"")
    }
}
