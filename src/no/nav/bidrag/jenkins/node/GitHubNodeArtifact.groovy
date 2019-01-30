package no.nav.bidrag.jenkins.node

import no.nav.bidrag.jenkins.GitHubArtifact
import no.nav.bidrag.jenkins.PipelineEnvironment

class GitHubNodeArtifact extends GitHubArtifact {

    GitHubNodeArtifact(PipelineEnvironment pipelineEnvironment) {
        super(pipelineEnvironment)
    }

    @Override
    def readBuildDescriptorFromSourceCode() {
        pipelineEnvironment.println("reading package.json from ${pipelineEnvironment.workspace}")
        def packageJson = pipelineEnvironment.buildScript.readJSON file: 'package.json'
        pipelineEnvironment.println('package.json:\n' + packageJson)

        return packageJson
    }
}
