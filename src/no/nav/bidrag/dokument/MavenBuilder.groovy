package no.nav.bidrag.dokument

class MavenBuilder {

    private GitHubArtifact gitHubArtifact
    private String mvnImage

    MavenBuilder(String mvnImage, GitHubArtifact gitHubArtifact) {
        this.gitHubArtifact = gitHubArtifact
        this.mvnImage = mvnImage
    }

    def buildAndTest() {
        gitHubArtifact.debugCommand("echo", "mvnImage: $mvnImage")

        String targetFolder = gitHubArtifact.targetFolder()
        gitHubArtifact.debugCommand("echo", "gitHubArtifact: $targetFolder")

/*        def pom = gitHubArtifact.fetchPom()
        gitHubArtifact.debugCommand("echo", "pom: `$pom`")
        gitHubArtifact.debugCommand("echo", "buildAndTest")*/
    }
}