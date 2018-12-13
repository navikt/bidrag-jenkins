import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.MavenBuild

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

            gitHubArtifact = new GitHubArtifact(this, workspace, gitHubProjectName, "${token}", branch)
            gitHubArtifact.checkout()

            pom = gitHubProjectName.fetchPom()
            sh "pom: $pom"
        }

        stage("build and test") {
            new MavenBuild(mvnImage, workspace, pom).buildAndTest()
        }
    }
}
