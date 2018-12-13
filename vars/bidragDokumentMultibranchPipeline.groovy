import no.nav.bidrag.dokument.MavenBuild

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchPipeline: pipelineParams = ${pipelineParams}"

    String mvnImage = pipelineParams.mvnImage
    File pom = null

    node {
        stage("list environment") {
            sh 'env'
            pom = readMavenPom file: 'pom.xml'
        }

        stage("build and test") {
            new MavenBuild(mvnImage, "$WORKSPACE", pom).buildAndTest()
        }
    }
}
