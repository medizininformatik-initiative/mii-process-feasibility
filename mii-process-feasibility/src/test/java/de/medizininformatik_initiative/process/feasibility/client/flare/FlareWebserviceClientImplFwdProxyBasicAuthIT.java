package de.medizininformatik_initiative.process.feasibility.client.flare;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.BindMode.READ_ONLY;

@Tag("client")
@Tag("flare")
@SpringBootTest(classes = FlareWebserviceClientSpringConfig.class)
@Testcontainers
public class FlareWebserviceClientImplFwdProxyBasicAuthIT extends FlareWebserviceClientImplBaseIT {

    @Autowired
    protected FlareWebserviceClient flareClient;

    private static URL squidProxyConf = getResource("forward_proxy_basic_auth.conf");
    private static URL passwordFile = getResource("forward_proxy.htpasswd");

    @Container
    public static GenericContainer<?> forwardProxy = new GenericContainer<>(
            DockerImageName.parse("ubuntu/squid:6.6-24.04_edge"))
                    .withExposedPorts(8080)
                    .withFileSystemBind(squidProxyConf.getPath(), "/etc/squid/squid.conf", READ_ONLY)
                    .withFileSystemBind(passwordFile.getPath(), "/etc/squid/passwd", READ_ONLY)
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .dependsOn(flare);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        var proxyHost = forwardProxy.getHost();
        var proxyPort = forwardProxy.getFirstMappedPort();

        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                () -> "structured-query");
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                () -> "http://flare:8080/");
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.host",
                () -> proxyHost);
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.port",
                () -> proxyPort);
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.username",
                () -> "test");
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.password",
                () -> "bar");
    }

    @Test
    void sendQuery() throws Exception {
        var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream().readAllBytes();
        var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
        assertEquals(0, feasibility);
    }
}
