package no.nav.bidrag.dokument

interface Builder {
    void buildAndTest()
    void updateVersion(String nextVersion)
    void verifySnapshotDependencies(GitHubArtifact gitHubArtifact)
}
