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

            // major version is always bumbed manual in develop for nexus artifacts
            stage("bump minor version (when last commit is not pipeline)") {
                when { expression { pipelineEnvironment.isMaster() && gitHubArtifact.isLastCommitterFromPipeline() } }
                steps { script { gitHubArtifact.updateMinorVersion(mavenBuilder) } }
            }

            stage("deploy new maven artifact") {
                when { expression { pipelineEnvironment.isMaster() } }
                steps { script { result = mavenBuilder.deployArtifact() } }
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
