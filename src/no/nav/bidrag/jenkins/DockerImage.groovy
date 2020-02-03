package no.nav.bidrag.jenkins

class DockerImage {

    private PipelineEnvironment pipelineEnvironment

    DockerImage(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    private void publishDockerImage(boolean gotoProd) {
        String version

        if (gotoProd) {
            version = pipelineEnvironment.fetchImageVersionForProd()
        } else {
            version = pipelineEnvironment.fetchImageVersion()
        }

        pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'nexusCredentials', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
            pipelineEnvironment.buildScript.sh "docker login -u ${pipelineEnvironment.buildScript.USERNAME} -p ${pipelineEnvironment.buildScript.PASSWORD} ${pipelineEnvironment.dockerRepo}"
            pipelineEnvironment.buildScript.sh "docker push ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:$version"
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

            boolean pushNewTag = tagGitHubArtifact(false)
            publishDockerImage(false)

            if (pushNewTag) {
                pipelineEnvironment.execute "git push --tags"
            }
        }
    }

    void releaseAndPublishForProd() {
        String workspaceFolder = pipelineEnvironment.path_workspace
        pipelineEnvironment.execute "cd ${pipelineEnvironment.path_workspace}"

        if (pipelineEnvironment.buildImage != null) {
            pipelineEnvironment.execute "docker run --rm -v $workspaceFolder:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.buildImage} mvn clean install -DskipTests -Dhendelse.environments=${pipelineEnvironment.fetchEnvironment()} -B -e"
        }

        String imgVersion = pipelineEnvironment.fetchImageVersionForProd()

        pipelineEnvironment.execute "docker build --build-arg version=${pipelineEnvironment.artifactVersion} -t ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:$imgVersion ."

        boolean pushNewTag = tagGitHubArtifact(true)
        publishDockerImage(true)

        if (pushNewTag) {
            pipelineEnvironment.execute "git push --tags"
        }
    }

    private boolean tagGitHubArtifact(boolean gotoProd) {
        String tagName = pipelineEnvironment.createTagName(gotoProd)

        if (pipelineEnvironment.canTagGitHubArtifact(gotoProd)) {
            pipelineEnvironment.execute "git tag -a $tagName -m $tagName"

            return true
        }

        if (pipelineEnvironment.isMaster() || pipelineEnvironment.isDevelop() || pipelineEnvironment.isRelease()) {
            pipelineEnvironment.println("Allready tagged git hub artifact: $tagName")
        } else {
            pipelineEnvironment.println("Will not tag $tagName when branch not being master, develop or release")
        }

        return false
    }

    void deleteImagesNotUsed() {
        pipelineEnvironment.execute("docker images -a | grep \"bidrag\" | grep -v \"cucumber\" | awk '{print \$3}' | xargs -r docker rmi")
        pipelineEnvironment.execute("docker ps -a | grep \"Exited\" | awk '{print \$1}' | xargs -r docker rm")
    }
}
