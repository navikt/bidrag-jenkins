package no.nav.bidrag.jenkins.maven

import no.nav.bidrag.jenkins.Builder
import no.nav.bidrag.jenkins.DependentVersions
import no.nav.bidrag.jenkins.PipelineEnvironment

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

    String deployArtifact() {
        String stableVersion = pipelineEnvironment.fetchStableVersion()

        try {
            pipelineEnvironment.println("gitHubArtifact: ${pipelineEnvironment.gitHubProjectName}")
            pipelineEnvironment.println("deploying maven artifact ($stableVersion).")
            updateVersion(stableVersion)

            pipelineEnvironment.execute(
                    "docker run --rm -v ${pipelineEnvironment.workspace}:/usr/src/mymaven -w /usr/src/mymaven " +
                            "-v \"${pipelineEnvironment.homeFolderJenkins}/.m2\":/root/.m2 ${pipelineEnvironment.buildImage} " +
                            "mvn clean deploy -B -e"
            )
        } catch(Exception e) {
            pipelineEnvironment.println('unable to deploy artifiact: ' + e)

            return 'UNSTABLE'
        }

        pipelineEnvironment.execute "git tag -a $stableVersion -m $stableVersion"
        pipelineEnvironment.execute "git push --tags"

        return 'SUCCESS'
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
        pipelineEnvironment.println buildDescriptor.getProperties().values().toString()

        DependentVersions.verify(buildDescriptor)
    }
}