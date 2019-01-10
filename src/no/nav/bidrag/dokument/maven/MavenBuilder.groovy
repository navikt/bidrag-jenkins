package no.nav.bidrag.dokument.maven

import no.nav.bidrag.dokument.Builder
import no.nav.bidrag.dokument.DependentVersions
import no.nav.bidrag.dokument.PipelineEnvironment

class MavenBuilder implements Builder {

    private PipelineEnvironment pipelineEnvironment

    MavenBuilder(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    @Override
    void buildAndTest() {
        pipelineEnvironment.println("gitHubArtifact: ${pipelineEnvironment.gitHubProjectName}")
        pipelineEnvironment.println("workspace: ${pipelineEnvironment.workspace}")

        if (pipelineEnvironment.isSnapshot()) {
            pipelineEnvironment.println("running maven build image.")
            pipelineEnvironment.execute(
                    "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven -w /usr/src/mymaven " +
                            "-v \"${pipelineEnvironment.homeFolderJenkins}/.m2\":/root/.m2 ${pipelineEnvironment.buildImage} " +
                            "mvn clean install -B -e"
            )
        } else {
            pipelineEnvironment.println(
                    "POM version is not a SNAPSHOT, it is ${pipelineEnvironment.artifactVersion}. " +
                            "Skipping build and testing of backend"
            )
        }
    }

    void deployArtifact() {
        pipelineEnvironment.println("gitHubArtifact: ${pipelineEnvironment.gitHubProjectName}")
        pipelineEnvironment.println("deploying maven artifact.")
        updateVersion(pipelineEnvironment.artifactVersion)

        pipelineEnvironment.execute(
                "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven -w /usr/src/mymaven " +
                        "-v \"${pipelineEnvironment.homeFolderJenkins}/.m2\":/root/.m2 ${pipelineEnvironment.buildImage} " +
                        "mvn clean deploy -B -e"
        )
    }

    @Override
    void updateVersion(String version) {
        pipelineEnvironment.execute(
                "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven " +
                        "-w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 " +
                        "${pipelineEnvironment.buildImage} mvn versions:set -B -DnewVersion=${version} -DgenerateBackupPoms=false"
        )
    }

    @Override
    void verifySnapshotDependencies(def buildDescriptor) {
        pipelineEnvironment.println "Verifying that no snapshot dependencies is being used."
        pipelineEnvironment.println buildDescriptor().getProperties().values().toString()

        DependentVersions.verify(buildDescriptor)
    }
}