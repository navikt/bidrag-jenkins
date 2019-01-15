import no.nav.bidrag.dokument.jenkins.Builder
import no.nav.bidrag.dokument.jenkins.Cucumber
import no.nav.bidrag.dokument.jenkins.DockerImage
import no.nav.bidrag.dokument.jenkins.GitHubArtifact
import no.nav.bidrag.dokument.jenkins.Nais
import no.nav.bidrag.dokument.jenkins.PipelineEnvironment

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "naisPipeline: pipelineParams = ${pipelineParams}"

    PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
            pipelineParams.gitHubProjectName,
            pipelineParams.buildImage,
            pipelineParams.environment,
            pipelineParams.buildType
    )

    Builder builder = pipelineEnvironment.initBuilder()
    Cucumber cucumber = new Cucumber(pipelineEnvironment)
    DockerImage dockerImage = new DockerImage(pipelineEnvironment)
    GitHubArtifact gitHubArtifact = pipelineEnvironment.initGitHubArtifact()
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

                        boolean isAutomatedBuild = PipelineEnvironment.isAutmatedBuild(
                                currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
                        )

                        if (isAutomatedBuild && gitHubArtifact.isLastCommitterFromPipeline()) {
                            pipelineEnvironment.doNotRunPipeline()
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
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { builder.verifySnapshotDependencies(gitHubArtifact.fetchBuildDescriptor()) } }
            }

            stage("build and test") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { builder.buildAndTest() } }
            }

            stage("bump minor version (when develop)") {
                when { expression { pipelineEnvironment.canRunPipelineOnDevelop() } }
                steps { script { gitHubArtifact.updateMinorVersion(builder) } }
            }

            stage("bump major version (when master and prod)") {
                when { expression { pipelineEnvironment.canRunPipelineOnMasterAndNaisClusterIsProdFss() } }
                steps {
                    script {
                        gitHubArtifact.checkout('develop')
                        gitHubArtifact.updateMajorVersion(builder)
                        gitHubArtifact.checkout('master')
                    }
                }
            }

            stage("release and publish docker image") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { dockerImage.releaseAndPublish() } }
            }

            stage("validate nais.yaml and upload to nexus") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { nais.validateAndUpload() } }
            }

            stage("deploy nais application") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { nais.deployApplication() } }
            }

            stage("run cucumber integration tests") {
                when { expression { pipelineEnvironment.canRunPipeline } }
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
