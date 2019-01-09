package no.nav.bidrag.dokument;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.assertj.core.api.Assertions.assertThat;
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

    @DisplayName("should update version property in package.json")
    @Test
    void shouldUpdateVersion() throws IOException {
        // set up values
        String version = readVersionFromPackageJson();
        System.out.println("Version before update: " + version);

        String majorVersion = fetchMajor(version);
        int minorVersion = fetchMinor(version);

        System.out.print("Major version: " + majorVersion);
        System.out.println(" - minor version: " + minorVersion);

        NodeBuilder nodeBuilder = initNodeBuilderWithPipelineEnvironment();

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

    private NodeBuilder initNodeBuilderWithPipelineEnvironment() {
        PipelineEnvironment pipelineEnvironment = new PipelineEnvironment(
                null, null, null, "node"
        );

        String file = this.getClass().getResource("package.json").getFile();
        pipelineEnvironment.setWorkspace(file.substring(0, file.lastIndexOf('/')));

        return (NodeBuilder) pipelineEnvironment.initBuilder();
    }
}