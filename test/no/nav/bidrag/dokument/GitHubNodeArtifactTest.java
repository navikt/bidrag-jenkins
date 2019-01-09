package no.nav.bidrag.dokument;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * in order to use this unit test, the following jars must be added to the classpath:
 * - assertj-core
 * - jackson-annotations
 * - jackson-core
 * - jackson-databind
 * - jaxb-api (only if jdk > 1.8)
 * - junit-jupiter-api
 */
@DisplayName("GitHubNodeArtifact")
class GitHubNodeArtifactTest {

    @DisplayName("should read version from package json")
    @Test
    void shouldUpdateVersion() {
        GitHubNodeArtifact gitHubNodeArtifact = initGitHubNodeArtifactWithPipelineEnvironment();
        GitHubNodeArtifact.PackageJsonDescriptor packageJsonDescriptor = (GitHubNodeArtifact.PackageJsonDescriptor) gitHubNodeArtifact.readBuildDescriptorFromSourceCode();

        String version = packageJsonDescriptor.getVersion();
        System.out.println(version);

        assertThat(version).isNotNull();
    }

    private GitHubNodeArtifact initGitHubNodeArtifactWithPipelineEnvironment() {
        PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
                null, null, null, "node"
        );

        String file = this.getClass().getResource("package.json").getFile();
        pipelineEnvironment.setWorkspace(file.substring(0, file.lastIndexOf('/')));

        return (GitHubNodeArtifact) pipelineEnvironment.initGitHubArtifact();
    }

}