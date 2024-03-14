package de.medizininformatik_initiative.process.feasibility.client.flare;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.time.Duration;
import java.util.Map;

public abstract class FlareWebserviceClientImplBaseIT {

    protected static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();

    public static GenericContainer<?> fhirServer = new GenericContainer<>(DockerImageName.parse("samply/blaze:0.23.0"))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("fhir-server")
            .withEnv("LOG_LEVEL", "debug");

    public static GenericContainer<?> flare = new GenericContainer<>(DockerImageName.parse("ghcr.io/medizininformatik-initiative/flare:2.1.0"))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("flare")
            .withEnv(Map.of("FLARE_FHIR_SERVER", "http://fhir-server:8080/fhir/"))
            .withStartupTimeout(Duration.ofMinutes(5))
            .dependsOn(fhirServer);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            flare.stop();
            fhirServer.stop();
        }));
        flare.start();
        fhirServer.start();
    }

    protected static URL getResource(final String name) {
        return FlareWebserviceClientImplBaseIT.class.getResource(name);
    }
}
