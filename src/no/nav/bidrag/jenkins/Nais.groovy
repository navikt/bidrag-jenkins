package no.nav.bidrag.jenkins

class Nais {
    private PipelineEnvironment pipelineEnvironment

    Nais(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void validateAndUpload() {
        pipelineEnvironment.println("[INFO] display nais: ${pipelineEnvironment.naisBinary}...")
        pipelineEnvironment.println("[INFO] display 'nais version'")
        pipelineEnvironment.execute("${pipelineEnvironment.naisBinary} version")

        pipelineEnvironment.println("[INFO] Run 'nais validate'")
        pipelineEnvironment.execute("${pipelineEnvironment.naisBinary} validate -f ${pipelineEnvironment.appConfig}")

        pipelineEnvironment.println("[INFO] Run 'nais upload' ... to Nexus!")
        pipelineEnvironment.buildScript.withCredentials(
                [[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]
        ) {
            pipelineEnvironment.execute(
                    "${pipelineEnvironment.naisBinary} upload -f ${pipelineEnvironment.appConfig} -a ${pipelineEnvironment.gitHubProjectName} " +
                            "--version '${pipelineEnvironment.fetchImageVersion()}' " +
                            "--username ${pipelineEnvironment.buildScript.USERNAME} --password '${pipelineEnvironment.buildScript.PASSWORD}' "
            )
        }
    }

    void deployApplication() {
        String namespace = pipelineEnvironment.fetchNamespace()
        pipelineEnvironment.println("[INFO] Run 'nais deploy' ... to NAIS using namespace: $namespace!")

        pipelineEnvironment.buildScript.timeout(time: 8, unit: 'MINUTES') {
            pipelineEnvironment.buildScript.withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]) {
                pipelineEnvironment.execute("${pipelineEnvironment.naisBinary} deploy -a ${pipelineEnvironment.gitHubProjectName} " +
                        "-v '${pipelineEnvironment.fetchImageVersion()}' -c ${PipelineEnvironment.naisCluster()} -n $namespace " +
                        "-u ${pipelineEnvironment.buildScript.USERNAME} -p '${pipelineEnvironment.buildScript.PASSWORD}'  " +
                        "-e '${pipelineEnvironment.fetchEnvironment()}' "
                )
            }
        }

        pipelineEnvironment.println("[INFO] Ferdig :)")
    }
}
