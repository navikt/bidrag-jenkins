package no.nav.bidrag.jenkins

class DockerImage {

    private PipelineEnvironment pipelineEnvironment

    DockerImage(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    private void publishDockerImage() {
        pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexusCredentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            pipelineEnvironment.buildScript.sh "docker login -u ${pipelineEnvironment.buildScript.USERNAME} -p ${pipelineEnvironment.buildScript.PASSWORD} ${pipelineEnvironment.dockerRepo}"
            pipelineEnvironment.buildScript.sh "docker push ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:${pipelineEnvironment.fetchImageVersion()}"
        }
    }

    void releaseAndPublish() {
        if (pipelineEnvironment.isRelease()) {
            releaseAndPublishForProd()
        } else {
            String workspaceFolder = pipelineEnvironment.path_workspace

            if (pipelineEnvironment.buildImage != null) {
                pipelineEnvironment.execute "docker run --rm -v $workspaceFolder:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.buildImage} mvn clean install -DskipTests -Dhendelse.environments=${pipelineEnvironment.fetchEnvironment()} -B -e"
            }

            String imgVersion = pipelineEnvironment.fetchImageVersion()

            pipelineEnvironment.execute "docker build --build-arg version=${pipelineEnvironment.artifactVersion} -t ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:$imgVersion ."
        }
    }

    void releaseAndPublishForProd() {
        String workspaceFolder = pipelineEnvironment.path_workspace
        pipelineEnvironment.execute "cd ${pipelineEnvironment.path_workspace}"

        if (pipelineEnvironment.buildImage != null) {
            pipelineEnvironment.execute "docker run --rm -v $workspaceFolder:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.buildImage} mvn clean install -DskipTests -Dhendelse.environments=${pipelineEnvironment.fetchEnvironment()} -B -e"
        }

        String imgVersion = pipelineEnvironment.fetchImageVersionForProd()

        pipelineEnvironment.execute "pwd"
        pipelineEnvironment.execute "ls -la && ls -la target/"
        pipelineEnvironment.execute "docker build --build-arg version=${pipelineEnvironment.artifactVersion} -t ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:$imgVersion ."

        boolean pushNewTag = tagGitHubArtifact()
        publishDockerImage()

        if (pushNewTag) {
            pipelineEnvironment.execute "git push --tags"
        }
    }

    private boolean tagGitHubArtifact() {
        String tagName = pipelineEnvironment.artifactVersion

        if (pipelineEnvironment.canTagGitHubArtifact()) {
            pipelineEnvironment.execute "git tag -a $tagName -m $tagName"

            return true
        }

        return false
    }

    void deleteImagesNotUsed() {
        pipelineEnvironment.execute("docker images -a | grep \"bidrag\" | grep -v \"cucumber\" | awk '{print \$3}' | xargs -r docker rmi")
        pipelineEnvironment.execute("docker ps -a | grep \"Exited\" | awk '{print \$1}' | xargs -r docker rm")
    }
}
