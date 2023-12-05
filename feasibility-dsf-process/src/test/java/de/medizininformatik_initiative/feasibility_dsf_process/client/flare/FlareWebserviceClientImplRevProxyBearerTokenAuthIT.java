package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

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
public class FlareWebserviceClientImplRevProxyBearerTokenAuthIT extends FlareWebserviceClientImplBaseIT {

    @Autowired
    protected FlareWebserviceClient flareClient;

    private static URL nginxConf = getResource("nginx.conf");
    private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_bearer_token_auth.conf.template");
    private static URL indexFile = getResource("index.html");
    private static URL passwordFile = getResource("reverse_proxy.htpasswd");
    private static String bearerToken = "1234";

    @Container
    public static GenericContainer<?> proxy = new GenericContainer<>(
            DockerImageName.parse("nginx:1.25.1"))
                    .withExposedPorts(8080)
                    .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                    .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                    .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                            "/etc/nginx/templates/default.conf.template",
                            READ_ONLY)
                    .withFileSystemBind(passwordFile.getPath(), "/etc/auth/.htpasswd", READ_ONLY)
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .dependsOn(flare);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        var proxyHost = proxy.getHost();
        var proxyPort = proxy.getFirstMappedPort();

        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                () -> String.format("http://%s:%s/", proxyHost, proxyPort));
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.bearer.token",
                () -> bearerToken);
    }

    @Test
    void sendQuery() throws Exception {
        var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream().readAllBytes();
        var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
        assertEquals(0, feasibility);
    }
}
