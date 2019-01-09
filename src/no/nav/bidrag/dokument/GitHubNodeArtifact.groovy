package no.nav.bidrag.dokument

class GitHubNodeArtifact extends GitHubArtifact {

    GitHubNodeArtifact(PipelineEnvironment pipelineEnvironment) {
        super(pipelineEnvironment)
    }

    @Override
    def readBuildDescriptorFromSourceCode() {
        pipelineEnvironment.println("reading package.json from ${pipelineEnvironment.workspace}")
        def packageJsonFile = new File(pipelineEnvironment.workspace, 'package.json')

        return new PackageJsonDescriptor(NodeBuilder.readAllLines(new FileReader(packageJsonFile)))
    }

    class PackageJsonDescriptor {
        private List<String> lines

        PackageJsonDescriptor(List<String> lines) {
            this.lines = lines
        }

        String getVersion() {
            return lines.stream()
                    .filter({ s -> s.trim().startsWith("\"version\"") })
                    .dump()
                    .replace("\"", "")
                    .replace("version", "")
                    .replace(":", "")
        }
    }
}
