import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.MavenBuilder

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchPipeline: pipelineParams = ${pipelineParams}"

    String mvnImage = pipelineParams.mvnImage
    String gitHubProjectName = pipelineParams.gitHubProjectName
    MavenBuilder mavenBuilder

    node {
        stage("init environment") {
            sh 'env'
            String branch = "$BRANCH_NAME"
            String workspace = "$WORKSPACE"

            gitHubArtifact = new GitHubArtifact(this, gitHubProjectName, branch, workspace)
            gitHubArtifact.checkout()

            def pom = gitHubArtifact.parseMavenPom()
            sh "pom: $pom"

            mavenBuilder = new MavenBuilder(this, mvnImage, pom)
        }

        stage("build and test") {
            mavenBuilder.buildAndTest()
        }
    }
}
