package no.nav.bidrag.dokument

class DependentVersions {
    static def verify(pom) {
        println "Verifying that no snapshot dependencies is being used."
        println pom.getProperties().values().toString()
        if ( pom.getProperties().values().toString().contains('SNAPSHOT') ) {
            throw "pom.xml har snapshot dependencies"
        }
    }
}
