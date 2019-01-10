package no.nav.bidrag.dokument.jenkins.node

import no.nav.bidrag.dokument.jenkins.GitHubArtifact
import no.nav.bidrag.dokument.jenkins.PipelineEnvironment

class GitHubNodeArtifact extends GitHubArtifact {

    private FileLineReaderWriter fileLineReaderWriter

    GitHubNodeArtifact(PipelineEnvironment pipelineEnvironment, FileLineReaderWriter fileLineReaderWriter) {
        super(pipelineEnvironment)
        this.fileLineReaderWriter = fileLineReaderWriter
    }

    @Override
    def readBuildDescriptorFromSourceCode() {
        pipelineEnvironment.println("reading package.json from ${pipelineEnvironment.workspace}")

        return new PackageJsonDescriptor(fileLineReaderWriter.readAllLines('package.json'))
    }
}
