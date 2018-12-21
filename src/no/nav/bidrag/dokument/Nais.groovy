package no.nav.bidrag.dokument

class Nais {
    private PipelineEnvironment pipelineEnvironment

    Nais(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    void validateAndUpload() {
        pipelineEnvironment.println("[INFO] display nais: ${pipelineEnvironment.nais}...")
        pipelineEnvironment.println("[INFO] display 'nais version'")
        pipelineEnvironment.execute("${pipelineEnvironment.nais} version")

        pipelineEnvironment.println("[INFO] Run 'nais validate'")
        pipelineEnvironment.execute("${pipelineEnvironment.nais} validate -f ${pipelineEnvironment.appConfig}")

        pipelineEnvironment.println("[INFO] Run 'nais upload' ... to Nexus!")
        pipelineEnvironment.buildScript.withCredentials(
                [[$class: 'UsernamePasswordMultiBinding', credentialsId: 'naisUploader', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD']]
        ) {
            pipelineEnvironment.execute(
                    "${pipelineEnvironment.nais} upload -f ${pipelineEnvironment.appConfig} -a ${pipelineEnvironment.gitHubProjectName} " +
                            "--version '${pipelineEnvironment.fetchImageVersion()}' " +
                            "--username ${pipelineEnvironment.buildScript.USERNAME} --password '${pipelineEnvironment.buildScript.PASSWORD}' "
            )
        }
    }
}
