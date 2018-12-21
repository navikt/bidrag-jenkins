package no.nav.bidrag.dokument

class DockerImage {

    private PipelineEnvironment pipelineEnvironment

    DockerImage(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void publishDockerImage() {
        if (pipelineEnvironment.isSnapshot()) {
            pipelineEnvironment.buildScript.sh "docker run --rm -v `pwd`:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn clean deploy -DskipTests -B -e"
            pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexusCredentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                pipelineEnvironment.buildScript.sh "docker login -u ${pipelineEnvironment.buildScript.USERNAME} -p ${pipelineEnvironment.buildScript.PASSWORD} ${pipelineEnvironment.dockerRepo}"
                pipelineEnvironment.buildScript.sh "docker push ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:${pipelineEnvironment.fetchImageVersion()}"
            }
        } else {
            pipelineEnvironment.println("POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. Skipping publishing!")
        }
    }

    void releaseArtifact() {
        if (pipelineEnvironment.isSnapshot()) {
            String workspaceFolder = pipelineEnvironment.workspace
            pipelineEnvironment.execute "docker run --rm -v ${workspaceFolder}:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn versions:set -B -DnewVersion=${pipelineEnvironment.mvnVersion} -DgenerateBackupPoms=false"
            pipelineEnvironment.execute "docker run --rm -v ${workspaceFolder}:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn clean install -DskipTests -Dhendelse.environments=${pipelineEnvironment.fetchEnvironment()} -B -e"

            String tmpTag = pipelineEnvironment.fetchEnvironment()

            if (pipelineEnvironment.isMaster && tmpTag == 'q0') {
                tmpTag = 'prod'
            }

            pipelineEnvironment.execute "docker build --build-arg version=${pipelineEnvironment.mvnVersion} -t ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:${pipelineEnvironment.fetchImageVersion()} ."
            pipelineEnvironment.execute "git tag -a ${pipelineEnvironment.gitHubProjectName}-${pipelineEnvironment.mvnVersion}-${tmpTag} -m ${pipelineEnvironment.gitHubProjectName}-${pipelineEnvironment.mvnVersion}-${tmpTag}"
            pipelineEnvironment.execute "git push --tags"
        } else {
            pipelineEnvironment.println("POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. Skipping releasing")
        }
    }
}
