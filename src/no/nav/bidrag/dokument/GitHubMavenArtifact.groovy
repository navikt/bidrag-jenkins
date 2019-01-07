package no.nav.bidrag.dokument;

class GitHubMavenArtifact extends GitHubArtifact {
    private def pom

    GitHubMavenArtifact(PipelineEnvironment pipelineEnvironment) {
        super(pipelineEnvironment)
    }

    @Override
    def fetchBuildDescriptor() {
        if (pom == null) {
            pom = fetchBuildDescriptorFromSourceCode()
        }

        return pom
    }

    @Override
    def fetchBuildDescriptorFromSourceCode() {
        pipelineEnvironment.println("parsing pom.xml from ${pipelineEnvironment.workspace}")
        def pom = pipelineEnvironment.buildScript.readMavenPom file: 'pom.xml'

        return pom
    }
}
