package no.nav.bidrag.jenkins

import no.nav.bidrag.jenkins.maven.GitHubMavenArtifact
import no.nav.bidrag.jenkins.maven.MavenBuilder
import no.nav.bidrag.jenkins.node.GitHubNodeArtifact
import no.nav.bidrag.jenkins.node.NodeBuilder

class PipelineEnvironment {

    boolean canRunPipeline = true
    def buildScript

    final String appConfig = "nais.yaml"
    final String dockerRepo = "repo.adeo.no:5443"
    final String naisBinary = "/usr/bin/nais"

    String artifactVersion
    String branchName
    String buildImage
    String buildType
    String environmentInDevelopBranch
    String gitHubProjectName
    String homeFolderJenkins
    String imageVersionForProd
    String lastCommitter
    String path_cucumber
    String path_jenkins_workspace
    String path_workspace

    private GitHubArtifact githubArtifact
    private String buildId
    private String imageVersion
    private def build

    PipelineEnvironment(String gitHubProjectName, String buildImage, String environmentInDevelopBranch, String buildType) {
        this.gitHubProjectName = gitHubProjectName
        this.buildImage = buildImage
        this.environmentInDevelopBranch = environmentInDevelopBranch
        this.buildType = buildType
    }

    PipelineEnvironment(String buildImage, String environmentInDevelopBranch, String buildType) {
        this.buildImage = buildImage
        this.environmentInDevelopBranch = environmentInDevelopBranch
        this.buildType = buildType
    }

    void doNotRunPipeline(String buildId) {
        canRunPipeline = false
        this.buildId = buildId
        println("will delete build $buildId on $gitHubProjectName/$branchName")
    }

    void deleteBuildWhenPipelineIsNotExecuted(def items) {
        if (!canRunPipeline && buildId != null) {
            fetchBuild(items)
            build.delete()
        }
    }

    void fetchBuild(def items) {
        items.each {
            println("Item: $it")

            if (it.name == gitHubProjectName) {
                def buildFolder = it.getItemByBranchName(branchName)
                println("branch: $buildFolder")
                fetchBuildById(buildFolder, buildId)
            }
        }

        if (build == null) {
            throw new IllegalStateException("unable to find $buildId on $gitHubProjectName/$branchName")
        }
    }

    private void fetchBuildById(def buildFolder, String buildId) {
        buildFolder.getBuilds().each {
            if (it.getId() == buildId) {
                println("build: $it, result=$it.result, id=$it.id")
                build = it
            }
        }
    }

    boolean isSnapshot() {
        return artifactVersion.contains("-SNAPSHOT")
    }

    void execute(String command) {
        buildScript.sh("$command")
    }

    void execute(String command, String quotedArgs) {
        buildScript.sh("$command \"$quotedArgs\"")
    }

    String fetchEnvironment() {
        if (environmentInDevelopBranch != null && isDevelop()) {
            return environmentInDevelopBranch
        }

        return isMaster() ? "q4" : isDevelop() ? "q0" : "q1"
    }

    static String naisCluster() {
        return 'preprod-fss'
    }

    String fetchImageVersion() {
        if (imageVersion == null) {
            String sha = Long.toHexString(System.currentTimeMillis())
            imageVersion = "$artifactVersion-${fetchEnvironment()}-$sha"
        } else {
            println("Using version: $imageVersion")
        }


        return imageVersion
    }

    String fetchImageVersionForProd() {
        if (imageVersionForProd == null) {
            String sha = Long.toHexString(System.currentTimeMillis())
            imageVersionForProd = "${fetchStableVersion()}-$sha"
        }

        return imageVersionForProd
    }

    String fetchStableVersion() {
        return artifactVersion.replace("-SNAPSHOT", "")
    }

    void println(Object toPrint) {
        buildScript.println(toPrint)
    }

    String createTagName(boolean gotoProd) {
        if (gotoProd) {
            return fetchImageVersionForProd()
        }

        return "$gitHubProjectName-$artifactVersion-${fetchEnvironment()}"
    }

    boolean canTagGitHubArtifact(boolean gotoProd) {
        String existingTag = buildScript.sh(script: "git tag -l ${createTagName(gotoProd)}", returnStdout: true).trim()

        return (isMaster() || isDevelop() || (gotoProd && isRelease())) && existingTag == ""
    }

    boolean isDevelop() {
        return branchName == "develop"
    }

    boolean isMaster() {
        return branchName == "master"
    }

    boolean isRelease() {
        return branchName == "release"
    }

    String fetchNamespace() {
        return isDevelop() || isRelease() ? "default" : fetchEnvironment()
    }

    boolean canRunPipelineOnDevelop(boolean isLastCommitFromPipeline) {
        return canRunPipeline && branchName == 'develop' && !isLastCommitFromPipeline
    }

    Builder initBuilder() {
        if (buildType == null || buildType == 'maven') {
            return new MavenBuilder(this)
        }

        if (buildType == 'node') {
            return new NodeBuilder(this)
        }

        throw new IllegalStateException("unknown build type: " + buildType)
    }

    GitHubArtifact initGitHubArtifact() {
        if (githubArtifact == null) {
            if (buildType == null || buildType == 'maven') {
                githubArtifact = new GitHubMavenArtifact(this)
            }

            if (buildType == 'node') {
                githubArtifact = new GitHubNodeArtifact(this)
            }
        }

        if (githubArtifact != null) {
            return githubArtifact
        }

        throw new IllegalStateException("unknown build type: " + buildType)
    }

    void checkoutCucumberFeatureOrUseMaster() {
        initGitHubArtifact().checkoutCucumberFeatureOrUseMaster(branchName)
    }

    def checkoutCucumberBackendFeatureOrUseMaster() {
        new GitHubMavenArtifact(this).checkoutCucumberBackendFeatureOrUseMaster()
    }

    boolean canRunPipelineWithMaven() {
        if (isRelease()) {
            return false
        }

        return canRunPipeline && githubArtifact instanceof GitHubMavenArtifact
    }

    def executeMavenTest() {
        println "[INFO] Run bidrag-cucumber-backend tests"

        buildScript.withCredentials([
                buildScript.usernamePassword(credentialsId: 'j104364', usernameVariable: 'USERNAME', passwordVariable: 'USER_AUTH'),
                buildScript.usernamePassword(credentialsId: '889b0a78-e462-41c5-a49d-3686af79e0b4', usernameVariable: 'TEST_USER', passwordVariable: 'TEST_PASS'),
                buildScript.usernamePassword(credentialsId: 'ddfdef43-4bdc-469d-b389-62412a6ff431', usernameVariable: 'PIP_USER', passwordVariable: 'PIP_AUTH')
        ]) {
            new MavenBuilder(this).executeMavenTest(
                    path_cucumber,
                    "${buildScript.USERNAME}", "${buildScript.USER_AUTH}",
                    "${buildScript.TEST_USER}", "${buildScript.TEST_PASS}",
                    "${buildScript.PIP_USER}", "${buildScript.PIP_AUTH}"
            )
        }
    }
}
