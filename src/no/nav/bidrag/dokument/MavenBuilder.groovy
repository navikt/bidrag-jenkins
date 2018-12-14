package no.nav.bidrag.dokument

class MavenBuilder {

    private String mvnImage
    public String workspace
    private File pom

    MavenBuilder(String mvnImage, String workspace, File pom) {
        this.mvnImage = mvnImage
        this.workspace = workspace
        this.pom = pom
        sh "echo mvnImage: $mvnImage"
        sh "echo workspace: $workspace"
        sh "echo pom: $pom"
    }

    def buildAndTest() {
        sh "buildAndTest"
    }

}