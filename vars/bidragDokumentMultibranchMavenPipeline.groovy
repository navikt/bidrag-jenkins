import no.nav.bidrag.dokument.DependentVersions
import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.MavenBuilder

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchMavenPipeline: pipelineParams = ${pipelineParams}"

    String mvnImage = pipelineParams.mvnImage
    String gitHubProjectName = pipelineParams.gitHubProjectName
    boolean isChangeOfCode = true
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

                        if (gitHubArtifact.isLastCommitterFromPipeline()) {
                            isChangeOfCode = false
                        } else {
                            gitHubArtifact.checkout()
                            mavenBuilder = new MavenBuilder(mvnImage, gitHubArtifact)
                        }
                    }
                }
            }

            stage("Verify maven dependency versions") {
                when { expression { isChangeOfCode } }
                steps { script { DependentVersions.verify(gitHubArtifact.fetchPom()) } }
            }

            stage("build and test") {
                when { expression { isChangeOfCode } }
                steps {
                    script {
                        mavenBuilder.buildAndTest("$HOME")
                    }
                }
            }

            stage("bump minor version") {
                when {
                    expression { isChangeOfCode && BRANCH_NAME == 'develop' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script {
                        gitHubArtifact.updateMinorVersion("$HOME", mvnImage)
                    }
                }
            }

            stage("bump major version") {
                when {
                    expression { isChangeOfCode && BRANCH_NAME == 'master' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script {
                        gitHubArtifact = new GitHubArtifact(gitHubArtifact, "develop")
                        gitHubArtifact.checkout()
                        gitHubArtifact.updateMajorVersion("$HOME", mvnImage)
                    }
                }
            }
        }
    }
}
