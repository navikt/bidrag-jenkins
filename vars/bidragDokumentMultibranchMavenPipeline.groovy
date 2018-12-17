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

    node {
        stage("init environment") {
            sh 'env'
            String branch = "$BRANCH_NAME"
            String workspace = "$WORKSPACE"

            gitHubArtifact = new GitHubArtifact(this, gitHubProjectName, branch, workspace)
            gitHubArtifact.checkout()

            mavenBuilder = new MavenBuilder(mvnImage, gitHubArtifact)
        }

        stage("build and test") {
            mavenBuilder.buildAndTest("$HOME")
        }

        stage("bump snapshot version") {
            if (gitHubArtifact.isSnapshot()) {
                if (gitHubArtifact.isNotFeatureBranch()) {
                    println("bumping version")
                } else {
                    println("feature branch is not bumped")
                }
            } else {
                pom = gitHubArtifact.fetchPom()
                println("do not bump: ${pom}")
            }
        }
    }
}
