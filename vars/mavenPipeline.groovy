import no.nav.bidrag.jenkins.DependentVersions
import no.nav.bidrag.jenkins.GitHubArtifact
import no.nav.bidrag.jenkins.PipelineEnvironment
import no.nav.bidrag.jenkins.maven.GitHubMavenArtifact
import no.nav.bidrag.jenkins.maven.MavenBuilder

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "mavenPipeline: pipelineParams = ${pipelineParams}"

    PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
            pipelineParams.gitHubProjectName,
            pipelineParams.mvnImage,
            null, 'maven'
    )

    GitHubArtifact gitHubArtifact = new GitHubMavenArtifact(pipelineEnvironment)
    MavenBuilder mavenBuilder = new MavenBuilder(pipelineEnvironment)

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
                        pipelineEnvironment.workspace = "$WORKSPACE"

                        boolean isAutomatedBuild = PipelineEnvironment.isAutmatedBuild(
                                currentBuild.rawBuild.getCause(hudson.model.Cause$UserIdCause)
                        )

                        if (isAutomatedBuild && gitHubArtifact.isLastCommitterFromPipeline()) {
                            pipelineEnvironment.doNotRunPipeline("$BUILD_ID")
                        } else {
                            gitHubArtifact.checkout()
                            pipelineEnvironment.artifactVersion = gitHubArtifact.fetchVersion()
                            pipelineEnvironment.dockerRepo = "repo.adeo.no:5443"
                        }
                    }
                }
            }

            stage("Verify maven dependency versions") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { DependentVersions.verify(gitHubArtifact.fetchBuildDescriptor()) } }
            }

            stage("build and test") {
                when { expression { pipelineEnvironment.canRunPipeline } }
                steps { script { mavenBuilder.buildAndTest() } }
            }

            stage("deploy new maven artifact (when not pipeline commit)") {
                when { expression { pipelineEnvironment.isMaster() && gitHubArtifact.isNotLastCommitterFromPipeline() } }
                steps { script { result = mavenBuilder.deployArtifact() } }
            }

            // major/minor version is always bumped manual in develop for nexus artifacts
            stage("bump patch version on master (when not pipeline commit)") {
                when { expression { pipelineEnvironment.isMaster() && gitHubArtifact.isNotLastCommitterFromPipeline() } }
                steps { script { gitHubArtifact.updatePatchVersion(mavenBuilder) } }
            }
        }

        post {
            always {
                script {
                    gitHubArtifact.resetWorkspace()
                    pipelineEnvironment.deleteBuildWhenPipelineIsNotExecuted(Jenkins.instance.items)
                }
            }
        }
    }
}
