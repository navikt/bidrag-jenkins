import no.nav.bidrag.dokument.DependentVersions
import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.maven.GitHubMavenArtifact
import no.nav.bidrag.dokument.maven.MavenBuilder
import no.nav.bidrag.dokument.PipelineEnvironment

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
                        pipelineEnvironment.homeFolderJenkins = "$HOME"
                        pipelineEnvironment.buildScript = this
                        pipelineEnvironment.workspace = "$WORKSPACE"

                        if (gitHubArtifact.isLastCommitterFromPipeline()) {
                            pipelineEnvironment.isNotChangeOfCode()
                        } else {
                            gitHubArtifact.checkout("$BRANCH_NAME")
                            pipelineEnvironment.artifactVersion = gitHubArtifact.fetchVersion()
                            pipelineEnvironment.dockerRepo = "repo.adeo.no:5443"
                        }
                    }
                }
            }

            stage("Verify maven dependency versions") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { DependentVersions.verify(gitHubArtifact.fetchBuildDescriptor()) } }
            }

            stage("build and test") {
                when { expression { pipelineEnvironment.isChangeOfCode } }
                steps { script { mavenBuilder.buildAndTest() } }
            }

            // major version is always bumbed manual in develop for nexus artifacts
            stage("bump minor version") {
                when { expression { pipelineEnvironment.isChangeOfCodeOnDevelop() } }
                steps { script { gitHubArtifact.updateMinorVersion(mavenBuilder) } }
            }

            stage("deploy new maven artifact") {
                when { expression { pipelineEnvironment.isChangeOfCodeOnMaster() } }
                steps { script { mavenBuilder.deployArtifact() } }
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
