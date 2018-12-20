import no.nav.bidrag.dokument.DependentVersions
import no.nav.bidrag.dokument.DockerImage
import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.MavenBuilder
import no.nav.bidrag.dokument.PipelineEnvironment

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchMavenNaisPipeline: pipelineParams = ${pipelineParams}"

    PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
            pipelineParams.gitHubProjectName,
            pipelineParams.mvnImage
    )

    DockerImage dockerImage
    GitHubArtifact gitHubArtifact
    MavenBuilder mavenBuilder

    pipeline {
        agent any

        stages {
            stage("init environment") {
                steps {
                    script {
                        sh 'env'
                        pipelineEnvironment.homeFolderJenkins = "$HOME"
                        pipelineEnvironment.multibranchPipeline = this
                        pipelineEnvironment.workspace = "$WORKSPACE"

                        gitHubArtifact = new GitHubArtifact(pipelineEnvironment)

                        if (gitHubArtifact.isLastCommitterFromPipeline()) {
                            pipelineEnvironment.isNotChangeOfCode()
                        } else {
                            gitHubArtifact.checkout("$BRANCH_NAME")
                            dockerImage = new DockerImage(gitHubArtifact)
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
                    expression {
                        pipelineEnvironment.isChangeOfCode && BRANCH_NAME == 'develop' && gitHubArtifact.isSnapshot()
                    }
                }
                steps {
                    script {
                        gitHubArtifact.updateMinorVersion()
                    }
                }
            }

            stage("bump major version") {
                when {
                    expression {
                        pipelineEnvironment.isChangeOfCode && BRANCH_NAME == 'master' && gitHubArtifact.isSnapshot()
                    }
                }
                steps {
                    script {
                        gitHubArtifact.checkout('develop')
                        gitHubArtifact.updateMajorVersion()
                    }
                }
            }
        }

        stage("release docker image") {
            when {
                expression {
                    pipelineEnvironment.isChangeOfCode &&
                            (BRANCH_NAME == 'develop' || BRANCH_NAME == 'master' || pipelineEnvironment.hasDeploymentArea())
                }
            }
            steps {
                script {
                    String releaseVersion = pipelineEnvironment.releaseVersion
                    println("this is release of docker image for versjon: $releaseVersion")
                }
            }
        }

        post {
            always {
                script {
                    gitHubArtifact.resetWorkspace()
                }
            }
        }
    }
}
