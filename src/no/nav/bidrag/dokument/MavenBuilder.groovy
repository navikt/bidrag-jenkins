package no.nav.bidrag.dokument

class MavenBuilder {

    private GitHubArtifact gitHubArtifact
    private String mvnImage

    MavenBuilder(String mvnImage, GitHubArtifact gitHubArtifact) {
        this.gitHubArtifact = gitHubArtifact
        this.mvnImage = mvnImage
    }

    def buildAndTest(String deployerHomeFolder) {
        gitHubArtifact.debugCommand("echo", "mvnImage: $mvnImage")

        String targetFolder = gitHubArtifact.targetFolder()
        gitHubArtifact.debugCommand("echo", "gitHubArtifact: $targetFolder")

        def pom = gitHubArtifact.fetchPom().version

        if (gitHubArtifact.isSnapshot()) {
            gitHubArtifact.debugCommand("echo", "running maven build image.")
           "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v \"${deployerHomeFolder}/.m2\":/root/.m2 ${mvnImage} mvn clean install -B -e".execute()
        } else {
            gitHubArtifact.debugCommand("echo","POM version is not a SNAPSHOT, it is ${pom}. Skipping build and testing of backend")
        }
    }
}