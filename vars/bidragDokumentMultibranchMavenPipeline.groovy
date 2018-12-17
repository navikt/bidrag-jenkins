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
                    script {
                        sh 'env'
                        String branch = "$BRANCH_NAME"
                        String workspace = "$WORKSPACE"

                        gitHubArtifact = new GitHubArtifact(this, gitHubProjectName, branch, workspace)
                        gitHubArtifact.checkout()

                        mavenBuilder = new MavenBuilder(mvnImage, gitHubArtifact)
                    }
                }
            }

            stage("build and test") {
                steps {
                    script {
                        mavenBuilder.buildAndTest("$HOME")
                    }
                }
            }

            stage("bump minor version") {
                when {
                    expression { BRANCH_NAME == 'develop' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script { println("bumping minor version") }
                }
            }

            stage("bump major version") {
                when {
                    expression { BRANCH_NAME == 'master' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script { println("bumping major version") }
                }
            }

            stage("complete pipeline") {
                steps {
                    script { println("end of pipeline on $BRANCH_NAME") }
                }
            }
        }
    }
}
