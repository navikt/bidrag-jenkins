package no.nav.bidrag.dokument

class MavenBuilder {

    private GitHubArtifact gitHubArtifact
    private String mvnImage

    MavenBuilder(String mvnImage, GitHubArtifact gitHubArtifact) {
        this.gitHubArtifact = gitHubArtifact
        this.mvnImage = mvnImage }

    def buildAndTest() {
        gitHubArtifact.debug("echo mvnImage: $mvnImage")
        gitHubArtifact.debug("echo gitHubArtifact: $gitHubArtifact")
        def pom = gitHubArtifact.fetchPom()
        gitHubArtifact.debug( "pom: $pom")
        gitHubArtifact.debug("buildAndTest")
    }
}