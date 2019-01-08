import no.nav.bidrag.dokument.Builder
import no.nav.bidrag.dokument.Cucumber
import no.nav.bidrag.dokument.DependentVersions
import no.nav.bidrag.dokument.DockerImage
import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.GitHubMavenArtifact
import no.nav.bidrag.dokument.MavenBuilder
import no.nav.bidrag.dokument.Nais
import no.nav.bidrag.dokument.PipelineEnvironment

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "naisPipeline: pipelineParams = ${pipelineParams}"

    PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
            pipelineParams.gitHubProjectName,
            pipelineParams.buildImage,
            pipelineParams.environment
    )

    Builder builder = new MavenBuilder(pipelineEnvironment)
    Cucumber cucumber = new Cucumber(pipelineEnvironment)
    DockerImage dockerImage = new DockerImage(pipelineEnvironment)
    GitHubArtifact gitHubArtifact = new GitHubMavenArtifact(pipelineEnvironment)
    Nais nais = new Nais(pipelineEnvironment)

    pipeline {
        agent any

        stages {
            stage("init environment") {
                steps {
                    script {
                        sh 'env'
                        pipelineEnvironment.homeFolderJenkins = "$HOME"
                        pipelineEnvironment.buildScript = this
                        pipelineEnvironment.workspace = "$WORKSPACE"

                        if (gitHubArtifact.isLastCommitterFromPipeline()) {
                            pipelineEnvironment.isNotChangeOfCode()
                        } else {
                            gitHubArtifact.checkout("$BRANCH_NAME")
                            pipelineEnvironment.appConfig = "nais.yaml"
                            pipelineEnvironment.artifactVersion = gitHubArtifact.fetchVersion()
                            pipelineEnvironment.branchName = "$BRANCH_NAME"
                            pipelineEnvironment.dockerRepo = "repo.adeo.no:5443"
                            pipelineEnvironment.nais = "/usr/bin/nais"
                        }
                    }
                }
            }

            stage("Verify maven dependency versions") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps {
                    script { DependentVersions.verify(gitHubArtifact.fetchBuildDescriptor(), pipelineEnvironment) }
                }
            }

            stage("build and test") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { builder.buildAndTest() } }
            }

            stage("bump minor version (when develop)") {
                when { expression { pipelineEnvironment.isChangeOfCodeOnDevelop() } }
                steps { script { gitHubArtifact.updateMinorVersion(builder) } }
            }

            stage("bump major version (when master and prod)") {
                when { expression { pipelineEnvironment.isChangeOfCodeOnMasterAndNaisClusterIsProdFss() } }
                steps {
                    script {
                        gitHubArtifact.checkout('develop')
                        gitHubArtifact.updateMajorVersion(builder)
                        gitHubArtifact.checkout('master')
                    }
                }
            }

            stage("release and publish docker image") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { dockerImage.releaseAndPublish() } }
            }

            stage("validate nais.yaml and upload to nexus") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { nais.validateAndUpload() } }
            }

            stage("deploy nais application") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { nais.deployApplication() } }
            }

            stage("run cucumber integration tests") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { result = cucumber.runCucumberTests() } }
            }
        }

        post {
            always {
                script {
                    gitHubArtifact.resetWorkspace()
                    dockerImage.deleteImagesNotUsed()
                }
            }
        }
    }
}
