package no.nav.bidrag.dokument

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
        if (pipelineEnvironment.isSnapshot()) {
            String workspaceFolder = pipelineEnvironment.workspace
            pipelineEnvironment.execute "docker run --rm -v $workspaceFolder:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn versions:set -B -DnewVersion=${pipelineEnvironment.mvnVersion} -DgenerateBackupPoms=false"
            pipelineEnvironment.execute "docker run --rm -v $workspaceFolder:/usr/src/mymaven -w /usr/src/mymaven -v '${pipelineEnvironment.homeFolderJenkins}/.m2':/root/.m2 ${pipelineEnvironment.mvnImage} mvn clean install -DskipTests -Dhendelse.environments=${pipelineEnvironment.fetchEnvironment()} -B -e"
            pipelineEnvironment.execute "docker build --build-arg version=${pipelineEnvironment.mvnVersion} -t ${pipelineEnvironment.dockerRepo}/${pipelineEnvironment.gitHubProjectName}:${pipelineEnvironment.fetchImageVersion()} ."

            boolean pushNewTag = tagGitHubArtifact()
            publishDockerImage()

            if (pushNewTag) {
                pipelineEnvironment.execute "git push --tags"
            }
        } else {
            pipelineEnvironment.println("POM version is not a SNAPSHOT, it is ${pipelineEnvironment.mvnVersion}. Skipping release and publish")
        }
    }

    private boolean tagGitHubArtifact() {
        String tagName = pipelineEnvironment.createTagName()

        if (pipelineEnvironment.canTagGitHubArtifact()) {
            pipelineEnvironment.execute "git tag -a $tagName -m $tagName"

            return true
        }

        if (pipelineEnvironment.isMaster) {
            pipelineEnvironment.println("Allready tagged dockerimage $tagName")
        } else if (pipelineEnvironment.isDevelop) {
            throw new IllegalStateException("$tagName allready present on develop branch?")
        } else {
            pipelineEnvironment.println("Will not tag $tagName")
        }

        return false
    }
}
