import no.nav.bidrag.jenkins.*

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "naiseratorProdPipeline: pipelineParams = ${pipelineParams}"

    PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
            pipelineParams.gitHubProjectName,
            pipelineParams.buildImage,
            pipelineParams.environmentInDevelopBranch,
            pipelineParams.buildType
    )

    Builder builder = pipelineEnvironment.initBuilder()
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
                        pipelineEnvironment.branchName = "release"
                        pipelineEnvironment.homeFolderJenkins = "$HOME"
                        pipelineEnvironment.buildScript = this
                        pipelineEnvironment.path_jenkins_workspace = "$JENKINS_HOME/workspace"
                        gitHubArtifact.checkout(pipelineEnvironment.branchName)
                        pipelineEnvironment.artifactVersion = gitHubArtifact.fetchProdVersion()
                        pipelineEnvironment.checkoutCucumberBackendFeatureOrUseMaster()
                    }
                }
            }

            stage("Verify dependency versions") {
                steps { script { builder.verifySnapshotDependencies(gitHubArtifact.fetchBuildDescriptor()) } }
            }

            stage("build and test") {
                steps { script { builder.buildAndTest() } }
            }

            stage("release and publish docker image") {
                steps { script { dockerImage.releaseAndPublishForProd() } }
            }

            stage("apply naiserator") {
                steps { script { nais.applyNaiserator() } }
            }

            stage("wait for deploy and termination of old pods") {
                steps { script { nais.waitForNaiseratorDeployAndOldPodsTerminated() } }
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
