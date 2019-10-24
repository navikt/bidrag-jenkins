import no.nav.bidrag.jenkins.*

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "naisPipeline: pipelineParams = ${pipelineParams}"

    PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
            pipelineParams.gitHubProjectName,
            pipelineParams.buildImage,
            pipelineParams.environmentInDevelopBranch,
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
                        pipelineEnvironment.branchName = "$BRANCH_NAME"
                        pipelineEnvironment.homeFolderJenkins = "$HOME"
                        pipelineEnvironment.buildScript = this
                        pipelineEnvironment.path_jenkins_workspace = "$JENKINS_HOME" + "/workspace"
                        pipelineEnvironment.path_cucumber = pipelineEnvironment.path_jenkins_workspace + "/bidrag-cucumber"
                        pipelineEnvironment.path_workspace = "$WORKSPACE"

                        boolean isAutomatedBuild = currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause) == null

                        if (isAutomatedBuild && gitHubArtifact.isLastCommitterFromPipeline()) {
                            pipelineEnvironment.doNotRunPipeline("$BUILD_ID")
                        } else {
                            gitHubArtifact.checkout("$BRANCH_NAME")

                            pipelineEnvironment.appConfig = "nais.yaml"
                            pipelineEnvironment.artifactVersion = gitHubArtifact.fetchVersion()
                            pipelineEnvironment.dockerRepo = "repo.adeo.no:5443"
                            pipelineEnvironment.naisBinary = "/usr/bin/nais"

                            gitHubArtifact.checkoutGlobalCucumberFeatureOrUseMaster()
                        }
                    }
                }
            }

            stage("Verify dependency versions") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { builder.verifySnapshotDependencies(gitHubArtifact.fetchBuildDescriptor()) } }
            }

            stage("build and test") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { builder.buildAndTest() } }
            }

            // major/minor versions are always edited manually: todo: tag final versions when master...
            stage("bump patch version on develop") {
                when { expression { pipelineEnvironment.canRunPipelineOnDevelop(gitHubArtifact.isLastCommitterFromPipeline()) } }
                steps { script { gitHubArtifact.updatePatchVersion(builder) } }
            }

            stage("release and publish docker image") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { dockerImage.releaseAndPublish() } }
            }

            stage("apply naiserator") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { nais.applyNaiserator() } }
            }

            stage("wait for deploy and termination of old pods") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { nais.waitForDeploAndOldPodsTerminated() } }
            }

            stage("run cucumber tests") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { result = cucumber.runCucumberTests() } }
            }

//            stage("run cucumber tests for backend") {
//                when { expression { pipelineEnvironment.canRunPipelineWithMaven() } }
//                steps { script { result = cucumber.runCucumberKotlinTests() } }
//            }
        }

        post {
            always {
                script {
                    gitHubArtifact.resetWorkspace()
                    gitHubArtifact.resetCucumber()
                    dockerImage.deleteImagesNotUsed()
                    pipelineEnvironment.deleteBuildWhenPipelineIsNotExecuted(Jenkins.instance.items)
                }
            }
        }
    }
}
