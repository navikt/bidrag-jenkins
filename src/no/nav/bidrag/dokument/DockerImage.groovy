package no.nav.bidrag.dokument

class DockerImage {

    private PipelineEnvironment pipelineEnvironment

    DockerImage(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void releaseNew() {
        String mvnVersion = pipelineEnvironment.mvnVersion
        pipelineEnvironment.multibranchPipeline.sh("echo 'this is release of docker image for versjon: $mvnVersion'")
    }
}
