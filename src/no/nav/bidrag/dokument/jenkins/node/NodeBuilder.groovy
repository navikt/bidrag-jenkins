package no.nav.bidrag.dokument.jenkins.node

import no.nav.bidrag.dokument.jenkins.Builder
import no.nav.bidrag.dokument.jenkins.PipelineEnvironment

class NodeBuilder implements Builder {
    private PipelineEnvironment pipelineEnvironment

    FileLineReaderWriter fileLineReaderWriter

    NodeBuilder(PipelineEnvironment pipelineEnvironment) {
        this.pipelineEnvironment = pipelineEnvironment
    }

    @Override
    void buildAndTest() {
        pipelineEnvironment.buildScript.withEnv([
                'HTTPS_PROXY=http://webproxy-utvikler.nav.no:8088',
                'HTTP_PROXY=http://webproxy-utvikler.nav.no:8088',
                'NO_PROXY=localhost,127.0.0.1,maven.adeo.no',
                'NODE_TLS_REJECT_UNAUTHORIZED=0',
                'PORT=8081'
        ]) {
            pipelineEnvironment.buildScript.sh "npm install"
            pipelineEnvironment.buildScript.sh "npm run build"
        }
    }

    @Override
    void updateVersion(String nextVersion) {
        List<String> allLinesInPackageJson = fileLineReaderWriter.readAllLines('package.json')
        ArrayList<String> linesWithModifiedVersion = modifyVersion(allLinesInPackageJson, nextVersion)
        fileLineReaderWriter.update(linesWithModifiedVersion, 'package.json')
    }

    private static ArrayList<String> modifyVersion(List<String> allLinesInPackageJson, String nextVersion) {
        List<String> linesWithModifiedVersion = new ArrayList<>()

        for (String line : allLinesInPackageJson) {
            if (line.trim().startsWith("\"version\"")) {
                String nextVersionLine = "   \"version\": \"$nextVersion\","
                linesWithModifiedVersion.add(nextVersionLine)
            } else {
                linesWithModifiedVersion.add(line)
            }
        }

        linesWithModifiedVersion.forEach({ s -> println(s) })

        return linesWithModifiedVersion
    }

    @Override
    void verifySnapshotDependencies(def buildDescriptor) {
        PackageJsonDescriptor packageJsonDescriptor = (PackageJsonDescriptor) buildDescriptor

        if (packageJsonDescriptor.hasSnapshotDependencies()) {
            throw new IllegalStateException('package.json contains snapshot dependencies: ' + packageJsonDescriptor)
        }
    }
}
