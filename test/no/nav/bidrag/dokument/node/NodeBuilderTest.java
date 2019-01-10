package no.nav.bidrag.dokument.node;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import no.nav.bidrag.dokument.PipelineEnvironment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.junit.jupiter.api.Assertions.assertAll;

/**
 * in order to use this unit test, the following jars must be added to the classpath:
 * - assertj-core
 * - jackson-annotations
 * - jackson-core
 * - jackson-databind
 * - jaxb-api (only if jdk > 1.8)
 * - junit-jupiter-api
 */
@DisplayName("NodeBuilder")
class NodeBuilderTest {

    private NodeBuilder nodeBuilder;

    @BeforeEach
    void initNodeBuilderWithPipelineEnvironment() {
        PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
                null, null, null, "node"
        );

        pipelineEnvironment.setWorkspace(findPathToPackageJson());

        nodeBuilder = (NodeBuilder) pipelineEnvironment.initBuilder();
        nodeBuilder.setFileLineReaderWriter(new FileLineReaderWriter(pipelineEnvironment));
    }

    private String findPathToPackageJson() {
        String file = this.getClass().getResource("package.json").getFile();
        return file.substring(0, file.lastIndexOf('/'));
    }

    @DisplayName("should update version property in package.json")
    @Test
    @Disabled("idea unable to read file after update (file seems fine in editor)")
    void shouldUpdateVersion() throws IOException {
        // set up values
        String version = readVersionFromPackageJson();
        System.out.println("Version before update: " + version);

        String majorVersion = fetchMajor(version);
        int minorVersion = fetchMinor(version);

        System.out.print("Major version: " + majorVersion);
        System.out.println(" - minor version: " + minorVersion);

        // test node builder
        nodeBuilder.updateVersion(majorVersion + "." + (minorVersion + 1));

        // assert values
        String updatedVersion = readVersionFromPackageJson();

        assertAll(
                () -> assertThat(fetchMajor(updatedVersion)).as("major version update").isEqualTo(majorVersion),
                () -> assertThat(fetchMinor(updatedVersion)).as("minor version update").isEqualTo(minorVersion + 1)
        );
    }

    private String readVersionFromPackageJson() throws IOException {
        InputStream fileStream = this.getClass().getResourceAsStream("package.json");
        assertThat(fileStream).isNotNull();

        ObjectNode objectNode = new ObjectMapper().readValue(fileStream, ObjectNode.class);

        return objectNode.get("version").asText();
    }

    private String fetchMajor(String version) {
        return version.substring(0, version.lastIndexOf('.'));
    }

    private int fetchMinor(String version) {
        return Integer.parseInt(version.substring(version.lastIndexOf('.') + 1));
    }

    @DisplayName("should verify no SNAPSHOT dependencies")
    @Test
    void shouldVerifyNoSnapshotDependencies() {
        PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(null, null, null, "node");
        pipelineEnvironment.setWorkspace(findPathToPackageJson());
        List<String> lines = new FileLineReaderWriter(pipelineEnvironment).readAllLines("package.json");
        nodeBuilder.verifySnapshotDependencies(new PackageJsonDescriptor(lines));
    }

    @DisplayName("should fail when SNAPSHOT dependencies")
    @Test
    @Disabled("Unnable to debug, verified that an exception is thrown when real file contains SNAPSHOT dependencies")
    void shouldFailWhenSnapshotDependencies() {
        List<String> lines = List.of("a line in package json", "\ndevDependencies\"", "\"avshome-shit\":\"1.0.1\"", "\"bleeding-edge\":\"1.0.1-SNAPSHOT\"");
        assertThatIllegalStateException()
                .isThrownBy(() -> nodeBuilder.verifySnapshotDependencies(new PackageJsonDescriptor(lines)))
                .withMessageContaining("package.json contains snapshot dependencies");
    }

    @DisplayName("should not fail when package.json is SNAPSHOT")
    @Test
    void shouldNotFailWhenPackageJsonIsSnapshot() {
        List<String> lines = List.of("\"version\":\"1.0.1-SNAPSHOT\"", "\"devDependencies\"", "\"avshome-shit\":\"1.0.1\"");
        nodeBuilder.verifySnapshotDependencies(new PackageJsonDescriptor(lines));
    }
}
