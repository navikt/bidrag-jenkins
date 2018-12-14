package no.nav.bidrag.dokument

class MavenBuilder {

    private String mvnImage
    public String workspace
    private def pom
    private def script

    MavenBuilder(script, String mvnImage, String workspace, pom) {
        this.mvnImage = mvnImage
        this.workspace = workspace
        this.pom = pom
        this.script = script
        script.sh "echo mvnImage: $mvnImage"
        script.sh "echo workspace: $workspace"
        script.sh "echo pom: $pom"
    }

    def buildAndTest() {
        script.sh "buildAndTest"
    }

}