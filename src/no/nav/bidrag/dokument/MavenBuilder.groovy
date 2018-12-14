package no.nav.bidrag.dokument

class MavenBuilder {

    private String mvnImage
    private def pom
    private def script

    MavenBuilder(script, String mvnImage, pom) {
        this.mvnImage = mvnImage
        this.pom = pom
        this.script = script
        script.sh "echo mvnImage: $mvnImage"
        script.sh "echo pom: $pom"
    }

    def buildAndTest() {
        script.sh "buildAndTest"
    }
}