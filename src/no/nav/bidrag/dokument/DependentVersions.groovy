package no.nav.bidrag.dokument

class DependentVersions {
    static void verify(pom) {
        if ( pom.getProperties().values().toString().contains('SNAPSHOT') ) {
            throw "pom.xml har snapshot dependencies"
        }
    }
}
