import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.MavenBuilder

def call(body) {

    def pipelineParams = [:]
    body.resolveStrategy = Closure.DELEGATE_FIRST
    body.delegate = pipelineParams
    body()

    println "bidragDokumentmMultibranchPipeline: pipelineParams = ${pipelineParams}"

    String committer
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
                        committer = sh(script: 'git log -1 --pretty=format:"%an (%ae)"', returnStdout: true).trim()
                        println("svada lada " + committer)

                        gitHubArtifact = new GitHubArtifact(this, gitHubProjectName, branch, workspace)
                        gitHubArtifact.checkout()

                        mavenBuilder = new MavenBuilder(mvnImage, gitHubArtifact)
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
                        nextVersion = "${majorVersion}." + (minorVersion.toInteger() + 1) + "-SNAPSHOT"
                        sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
                        sh "git commit -a -m \"updated to new dev-minor-version ${nextVersion} after release by ${committer}\""
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
                        nextVersion = (majorVersion.toFloat() + 1) + ".${minorVersion}.-SNAPSHOT"
                        sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '$HOME/.m2':/root/.m2 ${mvnImage} mvn versions:set -B -DnewVersion=${nextVersion} -DgenerateBackupPoms=false"
                        sh "git commit -a -m \"updated to new dev-major-version ${nextVersion} after release by ${committer}\""
                        sh "git push"
                    }
                }
            }

            stage("complete pipeline") {
                steps {
                    script {
                        pom = gitHubArtifact.fetchPom()
                        println("end of pipeline on $BRANCH_NAME for artifact $pom")
                    }
                }
            }
        }
    }
}
