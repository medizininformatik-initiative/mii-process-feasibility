package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("client")
@Tag("flare")
@SpringBootTest(classes = FlareWebserviceClientSpringConfig.class)
@Testcontainers
public class FlareWebserviceClientImplNonProxyIT extends FlareWebserviceClientImplBaseIT {

    @Autowired
    protected FlareWebserviceClient flareClient;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        var flareHost = flare.getHost();
        var flarePort = flare.getFirstMappedPort();

        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                () -> String.format("http://%s:%s/", flareHost, flarePort));
    }

    @Test
    public void sendQuery() throws IOException {
        var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json")
                .openStream().readAllBytes();

        var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
        assertEquals(0, feasibility);
    }
}
