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
        agent any

        stages {
            stage("init environment") {
                steps {
                    sh 'env'
                    String branch = "$BRANCH_NAME"
                    String workspace = "$WORKSPACE"

                    gitHubArtifact = new GitHubArtifact(this, gitHubProjectName, branch, workspace)
                    gitHubArtifact.checkout()

                    mavenBuilder = new MavenBuilder(mvnImage, gitHubArtifact)
                }
            }

            stage("build and test") {
                steps {
                    mavenBuilder.buildAndTest("$HOME")
                }
            }

            stage("bump minor version") {
                when(BRANCH_NAME == 'develop') {
                    if (gitHubArtifact.isSnapshot()) {
                        println("bumping minor version")
                    } else {
                        println("do not bump released artifact")
                    }
                }
            }

            stage("bump major version") {
                when(BRANCH_NAME == 'master') {
                    if (gitHubArtifact.isSnapshot()) {
                        println("bumping major version")
                    } else {
                        println("do not bump released artifact")
                    }
                }
            }

            post {
                println("end of pipeline on $BRANCH_NAME")
            }
        }
    }
}
