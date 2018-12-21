package no.nav.bidrag.dokument

class MavenBuilder {

    private PipelineEnvironment pipelineEnvironment

    MavenBuilder(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void buildAndTest() {
        pipelineEnvironment.println("gitHubArtifact: ${pipelineEnvironment.gitHubProjectName}")
        pipelineEnvironment.println("workspace: ${pipelineEnvironment.workspace}")

        if (pipelineEnvironment.isSnapshot()) {
            pipelineEnvironment.println("running maven build image.")
            pipelineEnvironment.execute(
                    "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven -w /usr/src/mymaven " +
                            "-v \"${pipelineEnvironment.homeFolderJenkins}/.m2\":/root/.m2 ${pipelineEnvironment.mvnImage} " +
                            "mvn clean install -B -e"
            )
        } else {
            pipelineEnvironment.println(
                    "POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. " +
                            "Skipping build and testing of backend"
            )
        }
    }

    void deployArtifact() {
        pipelineEnvironment.println("gitHubArtifact: ${pipelineEnvironment.gitHubProjectName}")
        pipelineEnvironment.println("deploying maven artifact.")
        updateVersion(pipelineEnvironment.mvnVersion)

        pipelineEnvironment.execute(
                "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven -w /usr/src/mymaven " +
                        "-v \"${pipelineEnvironment.homeFolderJenkins}/.m2\":/root/.m2 ${pipelineEnvironment.mvnImage} " +
                        "mvn clean deploy -B -e"
        )
    }

    void updateVersion(String version) {
        pipelineEnvironment.execute(
                "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven " +
                        "-w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 " +
                        "${pipelineEnvironment.mvnImage} mvn versions:set -B -DnewVersion=${version} -DgenerateBackupPoms=false"
        )
    }
}