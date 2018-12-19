package no.nav.bidrag.dokument

class PipelineEnvironment {

    boolean isChangeOfCode = true
    def multibranchPipeline

    String branch
    String deploymentArea
    String gitHubProjectName
    String homeFolderJenkins
    String lastCommitter
    String mvnImage
    String workspace

    PipelineEnvironment(String gitHubProjectName, String mvnImage) {
        this.gitHubProjectName = gitHubProjectName
        this.mvnImage = mvnImage
    }

    void isNotChangeOfCode() {
        isChangeOfCode = false;
    }

    boolean hasDeploymentArea() {
        return deploymentArea != null
    }
}
