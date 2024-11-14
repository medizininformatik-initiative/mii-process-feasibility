package de.medizininformatik_initiative.process.feasibility.client.flare;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;

public abstract class FlareWebserviceClientImplBaseIT {

    protected static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();

    public static GenericContainer<?> fhirServer = new GenericContainer<>(DockerImageName.parse("samply/blaze:0.30"))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("fhir-server")
            .withEnv("LOG_LEVEL", "debug");

    public static GenericContainer<?> flare = new GenericContainer<>(DockerImageName.parse("ghcr.io/medizininformatik-initiative/flare:2.3.0"))
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

    protected static InputStream getResourceAsStream(final String name) {
        return FlareWebserviceClientImplBaseIT.class.getResourceAsStream(name);
    }

    static String readFile(URL file) throws IOException, UnsupportedEncodingException {
        InputStream in = file.openStream();
    
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        for (int length; (length = in.read(buffer)) != -1;) {
            result.write(buffer, 0, length);
        }
        // StandardCharsets.UTF_8.name() > JDK 7
        var config = result.toString("UTF-8");
        in.close();
        return config;
    }

    static File createTempConfigFile(String foo) throws IOException, FileNotFoundException {
        File tempFile = File.createTempFile("feasibility-", ".yml");
        try (PrintWriter out = new PrintWriter(tempFile)) {
            out.println(foo);
        }
        return tempFile;
    }
}
