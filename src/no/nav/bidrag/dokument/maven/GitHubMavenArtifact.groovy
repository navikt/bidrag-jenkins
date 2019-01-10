package no.nav.bidrag.dokument.maven

import no.nav.bidrag.dokument.GitHubArtifact
import no.nav.bidrag.dokument.PipelineEnvironment

class GitHubMavenArtifact extends GitHubArtifact {

    GitHubMavenArtifact(PipelineEnvironment pipelineEnvironment) {
        super(pipelineEnvironment)
    }

    @Override
    def readBuildDescriptorFromSourceCode() {
        pipelineEnvironment.println("parsing pom.xml from ${pipelineEnvironment.workspace}")
        def pom = pipelineEnvironment.buildScript.readMavenPom file: 'pom.xml'

        return pom
    }
}
