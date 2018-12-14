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

    node {
        stage("init environment") {
            sh 'env'
            String workspace = "$HOME/$gitHubProjectName"
            String branch = "$BRANCH_NAME"

            gitHubArtifact = new GitHubArtifact(this, workspace, gitHubProjectName, branch)
            gitHubArtifact.checkout()

            File pom = gitHubProjectName.fetchPom()
            sh "pom: $pom"

            mavenBuilder = new MavenBuilder(mvnImage, workspace, pom)
        }

        stage("build and test") {
            mavenBuilder.buildAndTest()
        }
    }
}
