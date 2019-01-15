package no.nav.bidrag.dokument.jenkins.node

import no.nav.bidrag.dokument.jenkins.Builder
import no.nav.bidrag.dokument.jenkins.PipelineEnvironment

class NodeBuilder implements Builder {
    private PipelineEnvironment pipelineEnvironment

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
        def json = pipelineEnvironment.buildScript.readJSON file: 'package.json'
        pipelineEnvironment.execute("echo", "existing json.version = ${json.version}, next json.version = $nextVersion")
        json.version = nextVersion
        pipelineEnvironment.buildScript.writeJSON file: 'package.json', pretty: 4
    }

    @Override
    void verifySnapshotDependencies(def buildDescriptor) {
        def deps = buildDescriptor.dependencies.keySet().iterator()

        while(deps.hasNext()) {
            if (deps.next.contains("-SNAPSHOT")) {
                throw new IllegalStateException("package.json contains snapshot dependencies: " + buildDescriptor.toString())
            }
        }
    }
}
