package no.nav.bidrag.dokument

class MavenBuilder {

    private PipelineEnvironment pipelineEnvironment

    MavenBuilder(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void buildAndTest() {
        String deployerHomeFolder = pipelineEnvironment.homeFolderJenkins
        String mvnImage = pipelineEnvironment.mvnImage
        pipelineEnvironment.execute("echo", "mvnImage: $mvnImage")

        String workspaceFolder = pipelineEnvironment.workspace
        pipelineEnvironment.execute("echo", "gitHubArtifact: $workspaceFolder")

        if (pipelineEnvironment.isSnapshot()) {
            pipelineEnvironment.execute("echo", "running maven build image.")
            pipelineEnvironment.execute(
                    "docker run --rm -v ${workspaceFolder}:/usr/src/mymaven -w /usr/src/mymaven -v \"" +
                            "${deployerHomeFolder}/.m2\":/root/.m2 ${mvnImage} mvn clean install -B -e"
            )
        } else {
            pipelineEnvironment.execute("echo",
                    "POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. " +
                            "Skipping build and testing of backend"
            )
        }
    }

    void releaseArtifact() {
        if (pipelineEnvironment.isSnapshot()) {
            String workspaceFolder = pipelineEnvironment.workspace
            pipelineEnvironment.execute "docker run --rm -v ${workspaceFolder}:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn versions:set -B -DnewVersion=${pipelineEnvironment.mvnVersion} -DgenerateBackupPoms=false"
            pipelineEnvironment.execute "docker run --rm -v ${workspaceFolder}:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn clean install -DskipTests -Dhendelse.environments=${pipelineEnvironment.fetchEnvironment()} -B -e"
            pipelineEnvironment.execute "docker build --build-arg version=${pipelineEnvironment.mvnVersion} -t ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:${pipelineEnvironment.fetchImageVersion()} ."
            pipelineEnvironment.execute "git tag -a ${pipelineEnvironment.gitHubProjectName}-${pipelineEnvironment.mvnVersion}-${pipelineEnvironment.fetchEnvironment()} -m ${pipelineEnvironment.gitHubProjectName}-${pipelineEnvironment.mvnVersion}-${pipelineEnvironment.fetchEnvironment()}"
            pipelineEnvironment.execute "git push --tags"
        } else {
            println("POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. Skipping releasing")
        }
    }
}