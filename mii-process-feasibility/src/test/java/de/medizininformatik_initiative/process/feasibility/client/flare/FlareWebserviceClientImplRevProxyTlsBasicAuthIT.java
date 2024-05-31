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
public class FlareWebserviceClientImplRevProxyTlsBasicAuthIT extends FlareWebserviceClientImplBaseIT {

    @Autowired
    protected FlareWebserviceClient flareClient;

    private static URL nginxConf = getResource("nginx.conf");
    private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
    private static URL indexFile = getResource("index.html");
    private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
    private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
    private static URL trustStoreFile = getResource("../certs/ca.p12");
    private static URL passwordFile = getResource("reverse_proxy.htpasswd");
    private static String basicAuthUsername = "test";
    private static String basicAuthPassword = "foo";

    @Container
    public static GenericContainer<?> proxy = new GenericContainer<>(
            DockerImageName.parse("nginx:1.25.1"))
                    .withExposedPorts(8443)
                    .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                    .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                    .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                            "/etc/nginx/templates/default.conf.template",
                            READ_ONLY)
                    .withFileSystemBind(serverCertChain.getPath(), "/etc/nginx/certs/server_cert.pem", READ_ONLY)
                    .withFileSystemBind(serverCertKey.getPath(), "/etc/nginx/certs/server_cert_key.pem", READ_ONLY)
                    .withFileSystemBind(passwordFile.getPath(), "/etc/auth/.htpasswd", READ_ONLY)
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .dependsOn(flare);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        var proxyHost = proxy.getHost();
        var proxyPort = proxy.getFirstMappedPort();

        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                () -> "structured-query");
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                () -> String.format("https://%s:%s/", proxyHost, proxyPort));
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_path",
                () -> trustStoreFile.getPath());
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_password",
                () -> "changeit");
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username",
                () -> basicAuthUsername);
        registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password",
                () -> basicAuthPassword);
    }

    @Test
    void sendQuery() throws Exception {
        var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream().readAllBytes();
        var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
        assertEquals(0, feasibility);
    }
}
