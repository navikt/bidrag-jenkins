package no.nav.bidrag.dokument

class DockerImage {

    private PipelineEnvironment pipelineEnvironment

    DockerImage(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void releaseNew() {
        if (pipelineEnvironment.isSnapshot()) {
            pipelineEnvironment.multibranchPipeline.sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn clean deploy -DskipTests -B -e"
            pipelineEnvironment.multibranchPipeline.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexusCredentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                pipelineEnvironment.multibranchPipeline.sh "docker login -u ${pipelineEnvironment.multibranchPipeline.USERNAME} -p ${pipelineEnvironment.multibranchPipeline.PASSWORD} ${pipelineEnvironment.dockerRepo}"
                pipelineEnvironment.multibranchPipeline.sh "docker push ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:${pipelineEnvironment.fetchImageVersion()}"
            }
        } else {
            println("POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. Skipping publishing!")
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
