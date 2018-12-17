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

    GitHubArtifact gitHubArtifact
    MavenBuilder mavenBuilder

    pipeline {
        stage("init environment") {
            agent any
            sh 'env'
            String branch = "$BRANCH_NAME"
            String workspace = "$WORKSPACE"

            gitHubArtifact = new GitHubArtifact(this, gitHubProjectName, branch, workspace)
            gitHubArtifact.checkout()

            mavenBuilder = new MavenBuilder(mvnImage, gitHubArtifact)
        }

        stage("build and test") {
            agent any
            mavenBuilder.buildAndTest("$HOME")
        }

        stage("bump minor version") {
            agent any
            when(BRANCH_NAME == 'develop') {
                if (gitHubArtifact.isSnapshot()) {
                    println("bumping minor version")
                }
            }
        }

        stage("bump major version") {
            agent any
            when(BRANCH_NAME == 'master') {
                if (gitHubArtifact.isSnapshot()) {
                    println("bumping major version")
                }
            }
        }

        post {
            println("end of pipeline on $BRANCH_NAME")
        }
    }
}
