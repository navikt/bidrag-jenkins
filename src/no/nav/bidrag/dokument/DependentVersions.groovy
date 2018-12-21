package no.nav.bidrag.dokument

class DependentVersions {
    static def verify(pom, PipelineEnvironment pipelineEnvironment) {
        pipelineEnvironment.println "Verifying that no snapshot dependencies is being used."
        pipelineEnvironment.println pom.getProperties().values().toString()

        if ( pom.getProperties().values().toString().contains('SNAPSHOT') ) {
            throw "pom.xml har snapshot dependencies"
        }
    }

    static def verify(pom) {
        if ( pom.getProperties().values().toString().contains('SNAPSHOT') ) {
            throw "pom.xml har snapshot dependencies"
        }
    }
}
