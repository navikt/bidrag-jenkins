package no.nav.bidrag.jenkins

class DependentVersions {
    static void verify(pom) {
        if ( pom.getProperties().values().toString().contains('SNAPSHOT') ) {
            throw "pom.xml har snapshot dependencies"
        }
    }
}
