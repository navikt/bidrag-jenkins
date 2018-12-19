import no.nav.bidrag.dokument.DependentVersions
import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.MavenBuilder
import no.nav.bidrag.dokument.PipelineEnvironment

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchMavenPipeline: pipelineParams = ${pipelineParams}"

    PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
            pipelineParams.gitHubProjectName,
            pipelineParams.mvnImage
    )

    GitHubArtifact gitHubArtifact
    MavenBuilder mavenBuilder

    pipeline {
        agent any

        stages {
            stage("init environment") {
                steps {
                    script {
                        sh 'env'
                        pipelineEnvironment.branch = "$BRANCH_NAME"
                        pipelineEnvironment.homeFolderJenkins = "$HOME"
                        pipelineEnvironment.multibranchPipeline = this
                        pipelineEnvironment.workspace = "$WORKSPACE"

                        gitHubArtifact = new GitHubArtifact(pipelineEnvironment)

                        if (gitHubArtifact.isLastCommitterFromPipeline()) {
                            pipelineEnvironment.isNotChangeOfCode()
                        } else {
                            gitHubArtifact.checkout()
                            mavenBuilder = new MavenBuilder(pipelineEnvironment, gitHubArtifact)
                        }
                    }
                }
            }

            stage("Verify maven dependency versions") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { DependentVersions.verify(gitHubArtifact.fetchPom()) } }
            }

            stage("build and test") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps {
                    script {
                        mavenBuilder.buildAndTest()
                    }
                }
            }

            stage("bump minor version") {
                when {
                    expression { pipelineEnvironment.isChangeOfCode && BRANCH_NAME == 'develop' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script {
                        gitHubArtifact.updateMinorVersion()
                    }
                }
            }

            stage("bump major version") {
                when {
                    expression { pipelineEnvironment.isChangeOfCode && BRANCH_NAME == 'master' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script {
                        gitHubArtifact = new GitHubArtifact(gitHubArtifact, "develop")
                        gitHubArtifact.checkout()
                        gitHubArtifact.updateMajorVersion()
                    }
                }
            }
        }

        post {
            always {
                if (pipelineEnvironment.branch == 'master') {
                    gitHubArtifact.resetWorkspace()
                }
            }
        }
    }
}
