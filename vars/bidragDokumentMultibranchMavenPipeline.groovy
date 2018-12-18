import hudson.model.Result
import jenkins.model.CauseOfInterruption
import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.MavenBuilder

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchPipeline: pipelineParams = ${pipelineParams}"

    String mvnImage = pipelineParams.mvnImage
    String gitHubProjectName = pipelineParams.gitHubProjectName

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
                            println("not a real change")
                            result = 'UNSTABLE'
                        } else {
                            gitHubArtifact.checkout()
                            mavenBuilder = new MavenBuilder(mvnImage, gitHubArtifact)
                        }
                    }
                }
            }

          stage("build and test") {
                steps {
                    script {
                        mavenBuilder.buildAndTest("$HOME")
                    }
                }
            }

            stage("bump minor version") {
                when {
                    expression { BRANCH_NAME == 'develop' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script {
                        String majorVersion = gitHubArtifact.fetchMajorVersion()
                        String minorVersion = gitHubArtifact.fetchMinorVersion()
                        String lastCommitter = gitHubArtifact.lastCommitter
                        gitHubArtifact = new GitHubArtifact(gitHubArtifact, "develop")
                        nextVersion = "${majorVersion}." + (minorVersion.toInteger() + 1) + "-SNAPSHOT"
                        sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
                        sh "git commit -a -m \"updated to new dev-minor-version ${nextVersion} after release by ${lastCommitter}\""
                        sh "git push"
                    }
                }
            }

            stage("bump major version") {
                when {
                    expression { BRANCH_NAME == 'master' && gitHubArtifact.isSnapshot() }
                }
                steps {
                    script {
                        String majorVersion = gitHubArtifact.fetchMajorVersion()
                        String minorVersion = gitHubArtifact.fetchMinorVersion()
                        String lastCommitter = gitHubArtifact.lastCommitter
                        nextVersion = (majorVersion.toFloat() + 1) + ".${minorVersion}-SNAPSHOT"
                        sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
                        sh "git commit -a -m \"updated to new dev-major-version ${nextVersion} after release by ${lastCommitter}\""
                        sh "git push"
                    }
                }
            }
        }
    }
}
