package no.nav.bidrag.jenkins

interface Builder {
    void buildAndTest()
    void updateVersion(String nextVersion)
    void verifySnapshotDependencies(def buildDescriptor)
}
