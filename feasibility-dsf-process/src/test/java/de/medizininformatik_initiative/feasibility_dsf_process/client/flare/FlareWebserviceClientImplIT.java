package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("client")
@Tag("flare")
@SpringBootTest(classes = FlareWebserviceClientSpringConfig.class)
@Testcontainers
public class FlareWebserviceClientImplIT {

    @Autowired
    private FlareWebserviceClient flareClient;

    private static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();

    @Container
    public static GenericContainer<?> fhirServer = new GenericContainer<>(DockerImageName.parse("ghcr.io/samply/blaze:0.16.5"))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("fhir-server")
            .withEnv("LOG_LEVEL", "debug");

    @Container
    public static GenericContainer<?> flare = new GenericContainer<>(DockerImageName.parse("ghcr.io/rwth-imi/flare-query:1.0.0"))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("flare")
            .withEnv(Map.of(
                    "FLARE_FHIR_SERVER_URL", "http://fhir-server:8080/fhir/"
            ))
            .withStartupTimeout(Duration.ofMinutes(5))
            .dependsOn(fhirServer);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        var flareHost = flare.getHost();
        var flarePort = flare.getFirstMappedPort();

        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                () -> String.format("http://%s:%s/", flareHost, flarePort));
    }

    @Test
    public void testRequestToFlareWithEmptyFhirServer() throws IOException {
        var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json")
                .openStream().readAllBytes();

        var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
        assertEquals(0, feasibility);
    }
}
