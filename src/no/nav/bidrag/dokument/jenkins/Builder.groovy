package no.nav.bidrag.dokument.jenkins

interface Builder {
    void buildAndTest()
    void updateVersion(String nextVersion)
    void verifySnapshotDependencies(def buildDescriptor)
}
