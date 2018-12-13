package no.nav.bidrag.dokument

class MavenBuild {

    private String mvnImage
    public String workspace
    private File pom

    MavenBuild(String mvnImage, String workspace, File pom) {
        this.mvnImage = mvnImage
        this.workspace = workspace
        this.pom = pom
        "echo mvnInage: $mvnImage".execute()
        "echo workspace: $workspace".execute()
        "echo pom: $pom".execute()
    }

    def buildAndTest() {
        "echo buildAndTest".execute()
    }

}