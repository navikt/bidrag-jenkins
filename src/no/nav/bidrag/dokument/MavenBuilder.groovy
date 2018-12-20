package no.nav.bidrag.dokument

class MavenBuilder {

    private PipelineEnvironment pipelineEnvironment

    MavenBuilder(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void buildAndTest() {
        String deployerHomeFolder = pipelineEnvironment.homeFolderJenkins
        String mvnImage = pipelineEnvironment.mvnImage
        pipelineEnvironment.execute("echo", "mvnImage: $mvnImage")

        String workspaceFolder = pipelineEnvironment.workspace
        pipelineEnvironment.execute("echo", "gitHubArtifact: $workspaceFolder")

        if (pipelineEnvironment.isSnapshot()) {
            pipelineEnvironment.execute("echo", "running maven build image.")
            pipelineEnvironment.execute(
                    "docker run --rm -v ${workspaceFolder}:/usr/src/mymaven -w /usr/src/mymaven -v \"" +
                            "${deployerHomeFolder}/.m2\":/root/.m2 ${mvnImage} mvn clean install -B -e"
            )
        } else {
            pipelineEnvironment.execute("echo",
                    "POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. " +
                            "Skipping build and testing of backend"
            )
        }
    }
}