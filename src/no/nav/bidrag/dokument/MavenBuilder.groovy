package no.nav.bidrag.dokument

class MavenBuilder {

    private GitHubArtifact gitHubArtifact
    private PipelineEnvironment pipelineEnvironment

    MavenBuilder(PipelineEnvironment pipelineEnvironment, GitHubArtifact gitHubArtifact) {
        this.gitHubArtifact = gitHubArtifact
        this.pipelineEnvironment = pipelineEnvironment
    }

    void buildAndTest() {
        String deployerHomeFolder = pipelineEnvironment.homeFolderJenkins
        String mvnImage = pipelineEnvironment.mvnImage
        gitHubArtifact.execute("echo", "mvnImage: $mvnImage")

        String workspaceFolder = pipelineEnvironment.workspace
        gitHubArtifact.execute("echo", "gitHubArtifact: $workspaceFolder")

        def pom = gitHubArtifact.fetchPom()

        if (gitHubArtifact.isSnapshot()) {
            gitHubArtifact.execute("echo", "running maven build image.")
            gitHubArtifact.execute(
                    "docker run --rm -v ${workspaceFolder}:/usr/src/mymaven -w /usr/src/mymaven -v \"" +
                            "${deployerHomeFolder}/.m2\":/root/.m2 ${mvnImage} mvn clean install -B -e"
            )
        } else {
            gitHubArtifact.execute("echo",
                    "POM version is not a SNAPSHOT, it is ${pom}. " +
                            "Skipping build and testing of backend"
            )
        }
    }
}