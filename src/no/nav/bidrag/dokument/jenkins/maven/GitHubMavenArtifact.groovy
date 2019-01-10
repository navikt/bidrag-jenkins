package no.nav.bidrag.dokument.jenkins.maven

import no.nav.bidrag.dokument.jenkins.GitHubArtifact
import no.nav.bidrag.dokument.jenkins.PipelineEnvironment

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
