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

    pipeline {
        agent any

        stage("init environment") {
            sh 'env'
            workspace = "$HOME/$gitHubProjectName"
            gitHubArtifact = new GitHubArtifact(this, workspace, gitHubProjectName, token, "master")
            gitHubArtifact.checkout()
            pom = gitHubProjectName.fetchPom()
            sh "pom: $pom"
        }

        stage("build and test") {
            new MavenBuild(mvnImage, workspace, pom).buildAndTest()
        }
    }
}
