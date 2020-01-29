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

    void releaseAndPublishForProd() {
        releaseAndPublish(true)
    }

    void releaseAndPublish() {
        releaseAndPublish(false)
    }

    void releaseAndPublish(boolean gotoProd) {
        String workspaceFolder = pipelineEnvironment.path_workspace
        pipelineEnvironment.execute "echo version:"
        pipelineEnvironment.execute "mvn help:evaluate -Dexpression=project.version -q -DforceStdout"

        if (pipelineEnvironment.buildImage != null) {
            if (!gotoProd) {
                pipelineEnvironment.execute "docker run --rm -v $workspaceFolder:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.buildImage} mvn versions:set -B -DnewVersion=${pipelineEnvironment.artifactVersion} -DgenerateBackupPoms=false"
            }

            pipelineEnvironment.execute "docker run --rm -v $workspaceFolder:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.buildImage} mvn clean install -DskipTests -Dhendelse.environments=${pipelineEnvironment.fetchEnvironment()} -B -e"
        }

        String imgVersion

        if (gotoProd) {
            imgVersion = pipelineEnvironment.fetchImageVersionForProd()
        } else {
            imgVersion = pipelineEnvironment.fetchImageVersion()
        }

        pipelineEnvironment.execute "ls -la && ls -la target/"
        pipelineEnvironment.execute "docker build --build-arg version=${pipelineEnvironment.artifactVersion} -t ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:$imgVersion ."

        boolean pushNewTag = tagGitHubArtifact(gotoProd)
        publishDockerImage()

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

        if (gotoProd && pipelineEnvironment.isRelease()) {
            throw new IllegalStateException("Unable to tag with $tagName")
        }

        if (pipelineEnvironment.isMaster() || pipelineEnvironment.isDevelop()) {
            pipelineEnvironment.println("Allready tagged git hub artifact: $tagName")
        } else {
            pipelineEnvironment.println("Will not tag $tagName when branch not being master or develop")
        }

        return false
    }

    void deleteImagesNotUsed() {
        pipelineEnvironment.execute("docker images -a | grep \"bidrag\" | grep -v \"cucumber\" | awk '{print \$3}' | xargs -r docker rmi")
        pipelineEnvironment.execute("docker ps -a | grep \"Exited\" | awk '{print \$1}' | xargs -r docker rm")
    }
}
