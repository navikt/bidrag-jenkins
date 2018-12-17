package no.nav.bidrag.dokument

class MavenBuilder {

    private GitHubArtifact gitHubArtifact
    private String mvnImage

    MavenBuilder(String mvnImage, GitHubArtifact gitHubArtifact) {
        this.gitHubArtifact = gitHubArtifact
        this.mvnImage = mvnImage
    }

    def buildAndTest(String deployerHomeFolder) {
        gitHubArtifact.execute("echo", "mvnImage: $mvnImage")

        String workspaceFolder = gitHubArtifact.workspaceFolder()
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