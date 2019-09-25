package no.nav.bidrag.jenkins.maven

import no.nav.bidrag.jenkins.GitHubArtifact
import no.nav.bidrag.jenkins.PipelineEnvironment

class GitHubMavenArtifact extends GitHubArtifact {

    GitHubMavenArtifact(PipelineEnvironment pipelineEnvironment) {
        super(pipelineEnvironment)
    }

    @Override
    def readBuildDescriptorFromSourceCode() {
        pipelineEnvironment.println("parsing pom.xml from ${pipelineEnvironment.path_workspace}")
        def pom = pipelineEnvironment.buildScript.readMavenPom file: 'pom.xml'

        return pom
    }
}
