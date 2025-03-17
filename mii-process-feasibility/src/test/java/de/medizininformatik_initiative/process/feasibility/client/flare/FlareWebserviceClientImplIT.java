package de.medizininformatik_initiative.process.feasibility.client.flare;

import com.google.common.base.Stopwatch;
import de.medizininformatik_initiative.process.feasibility.client.variables.TestConstantsFeasibility;
import eu.rekawek.toxiproxy.Proxy;
import eu.rekawek.toxiproxy.ToxiproxyClient;
import eu.rekawek.toxiproxy.model.ToxicDirection;
import eu.rekawek.toxiproxy.model.toxic.Latency;
import org.assertj.core.api.Condition;
import org.assertj.core.description.Description;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.ToxiproxyContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.testcontainers.containers.BindMode.READ_ONLY;


@Tag("client")
@Tag("flare")
@SpringBootTest(classes = FlareWebserviceClientSpringConfig.class)
@Testcontainers
public class FlareWebserviceClientImplIT {

    protected static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();

    public static GenericContainer<?> fhirServer = new GenericContainer<>(
            DockerImageName.parse("samply/blaze:" + TestConstantsFeasibility.BLAZE_VERSION))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("fhir-server")
            .withEnv("LOG_LEVEL", "debug");

    public static GenericContainer<?> flare = new GenericContainer<>(
            DockerImageName.parse("ghcr.io/medizininformatik-initiative/flare:" + TestConstantsFeasibility.FLARE_VERSION))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("flare")
            .withEnv(Map.of("FLARE_FHIR_SERVER", "http://fhir-server:8080/fhir/"))
            .withStartupTimeout(Duration.ofMinutes(5))
            .dependsOn(fhirServer);

    @BeforeAll
    static void init() {
        flare.start();
        fhirServer.start();
    }

    @AfterAll
    static void shutdown() {
        flare.stop();
        fhirServer.stop();
    }

    protected static URL getResource(final String name) {
        return FlareWebserviceClientImplIT.class.getResource(name);
    }

    @Nested
    @DisplayName("No Proxy")
    class NoProxyIT {

        @Autowired protected FlareWebserviceClient flareClient;

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            var flareHost = flare.getHost();
            var flarePort = flare.getFirstMappedPort();

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                    () -> "structured-query");
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

    @Nested
    @DisplayName("Forward Proxy")
    class FwdProxyIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL squidProxyConf = getResource("forward_proxy.conf");

        @Container public static GenericContainer<?> forwardProxy = new GenericContainer<>(
                // renovate
                DockerImageName.parse("ubuntu/squid:" + TestConstantsFeasibility.SQUID_VERSION))
                        .withExposedPorts(8080)
                        .withFileSystemBind(squidProxyConf.getPath(), "/etc/squid/squid.conf", READ_ONLY)
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
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth")
    class FwdProxyBasicAuthIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL squidProxyConf = getResource("forward_proxy_basic_auth.conf");
        private static URL passwordFile = getResource("forward_proxy.htpasswd");

        @Container public static GenericContainer<?> forwardProxy = new GenericContainer<>(
                DockerImageName.parse("ubuntu/squid:" + TestConstantsFeasibility.SQUID_VERSION))
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
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth with Reverse Proxy Bearer Token Auth")
    class FwdProxyBasicAuthRevProxyBearerTokenAuthIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_bearer_token_auth.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL squidProxyConf = getResource("forward_proxy_basic_auth.conf");
        private static URL forwardProxyPasswordFile = getResource("forward_proxy.htpasswd");
        private static String bearerToken = "1234";

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
                        .withExposedPorts(8080)
                        .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                        .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                        .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                                "/etc/nginx/templates/default.conf.template",
                                READ_ONLY)
                        .withNetwork(DEFAULT_CONTAINER_NETWORK)
                        .withNetworkAliases("proxy")
                        .dependsOn(flare);
        @Container public static GenericContainer<?> forwardProxy = new GenericContainer<>(
                DockerImageName.parse("ubuntu/squid:" + TestConstantsFeasibility.SQUID_VERSION))
                        .withExposedPorts(8080)
                        .withFileSystemBind(squidProxyConf.getPath(), "/etc/squid/squid.conf", READ_ONLY)
                        .withFileSystemBind(forwardProxyPasswordFile.getPath(), "/etc/squid/passwd", READ_ONLY)
                        .withNetwork(DEFAULT_CONTAINER_NETWORK)
                        .dependsOn(proxy);

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            var proxyHost = forwardProxy.getHost();
            var proxyPort = forwardProxy.getFirstMappedPort();

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                    () -> "structured-query");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                    () -> "http://proxy:8080/");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.host",
                    () -> proxyHost);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.port",
                    () -> proxyPort);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.username",
                    () -> "test");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.password",
                    () -> "bar");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.bearer.token",
                    () -> bearerToken);
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Basic Auth")
    class RevProxyBasicAuthIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_basic_auth.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL passwordFile = getResource("reverse_proxy.htpasswd");
        private static String basicAuthUsername = "test";
        private static String basicAuthPassword = "foo";

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
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

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                    () -> "structured-query");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                    () -> String.format("http://%s:%s/", proxyHost, proxyPort));
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username",
                    () -> basicAuthUsername);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password",
                    () -> basicAuthPassword);
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Bearer Token Auth")
    class RevProxyBearerTokenAuthIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_bearer_token_auth.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL passwordFile = getResource("reverse_proxy.htpasswd");
        private static String bearerToken = "1234";

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
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

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                    () -> "structured-query");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                    () -> String.format("http://%s:%s/", proxyHost, proxyPort));
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.bearer.token",
                    () -> bearerToken);
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy TLS")
    class RevProxyTlsIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.p12");

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
                        .withExposedPorts(8443)
                        .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                        .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                        .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                                "/etc/nginx/templates/default.conf.template",
                                READ_ONLY)
                        .withFileSystemBind(serverCertChain.getPath(), "/etc/nginx/certs/server_cert.pem", READ_ONLY)
                        .withFileSystemBind(serverCertKey.getPath(), "/etc/nginx/certs/server_cert_key.pem", READ_ONLY)
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
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Basic Auth with TLS")
    class RevProxyTlsBasicAuthIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.p12");
        private static URL passwordFile = getResource("reverse_proxy.htpasswd");
        private static String basicAuthUsername = "test";
        private static String basicAuthPassword = "foo";

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
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
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Client Certificate Auth")
    class RevProxyTlsClientCertIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.p12");
        private static URL keyStoreFile = getResource("../certs/client_key_store.p12");

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
                        .withExposedPorts(8443)
                        .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                        .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                        .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                                "/etc/nginx/templates/default.conf.template",
                                READ_ONLY)
                        .withFileSystemBind(serverCertChain.getPath(), "/etc/nginx/certs/server_cert.pem", READ_ONLY)
                        .withFileSystemBind(serverCertKey.getPath(), "/etc/nginx/certs/server_cert_key.pem", READ_ONLY)
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
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.key_store_path",
                    () -> keyStoreFile.getPath());
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.key_store_password",
                    () -> "changeit");
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth with Reverse Proxy TLS")
    class FwdProxyBasicAuthRevProxyTlsIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL squidProxyConf = getResource("forward_proxy_basic_auth.conf");
        private static URL passwordFile = getResource("forward_proxy.htpasswd");
        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.p12");

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
                        .withExposedPorts(8443)
                        .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                        .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                        .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                                "/etc/nginx/templates/default.conf.template",
                                READ_ONLY)
                        .withFileSystemBind(serverCertChain.getPath(), "/etc/nginx/certs/server_cert.pem", READ_ONLY)
                        .withFileSystemBind(serverCertKey.getPath(), "/etc/nginx/certs/server_cert_key.pem", READ_ONLY)
                        .withNetwork(DEFAULT_CONTAINER_NETWORK)
                        .withNetworkAliases("proxy")
                        .dependsOn(flare);

        @Container public static GenericContainer<?> forwardProxy = new GenericContainer<>(
                DockerImageName.parse("ubuntu/squid:" + TestConstantsFeasibility.SQUID_VERSION))
                        .withExposedPorts(8080)
                        .withFileSystemBind(squidProxyConf.getPath(), "/etc/squid/squid.conf", READ_ONLY)
                        .withFileSystemBind(passwordFile.getPath(), "/etc/squid/passwd", READ_ONLY)
                        .withNetwork(DEFAULT_CONTAINER_NETWORK)
                        .dependsOn(proxy);

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            var proxyHost = forwardProxy.getHost();
            var proxyPort = forwardProxy.getFirstMappedPort();

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                    () -> "structured-query");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                    () -> "https://proxy:8443/");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_path",
                    () -> trustStoreFile.getPath());
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_password",
                    () -> "changeit");
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
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth with Reverse Proxy Basic Auth")
    class FwdRevProxyBasicAuthIT {

        @Autowired protected FlareWebserviceClient flareClient;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_basic_auth.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL reverseProxyPasswordFile = getResource("reverse_proxy.htpasswd");
        private static URL squidProxyConf = getResource("forward_proxy_basic_auth.conf");
        private static URL forwardProxyPasswordFile = getResource("forward_proxy.htpasswd");

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
                        .withExposedPorts(8080)
                        .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                        .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                        .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                                "/etc/nginx/templates/default.conf.template",
                                READ_ONLY)
                        .withFileSystemBind(reverseProxyPasswordFile.getPath(), "/etc/auth/.htpasswd", READ_ONLY)
                        .withNetwork(DEFAULT_CONTAINER_NETWORK)
                        .withNetworkAliases("proxy")
                        .dependsOn(flare);
        @Container public static GenericContainer<?> forwardProxy = new GenericContainer<>(
                DockerImageName.parse("ubuntu/squid:" + TestConstantsFeasibility.SQUID_VERSION))
                        .withExposedPorts(8080)
                        .withFileSystemBind(squidProxyConf.getPath(), "/etc/squid/squid.conf", READ_ONLY)
                        .withFileSystemBind(forwardProxyPasswordFile.getPath(), "/etc/squid/passwd", READ_ONLY)
                        .withNetwork(DEFAULT_CONTAINER_NETWORK)
                        .dependsOn(proxy);

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            var proxyHost = forwardProxy.getHost();
            var proxyPort = forwardProxy.getFirstMappedPort();

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                    () -> "structured-query");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                    () -> "http://proxy:8080/");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.host",
                    () -> proxyHost);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.port",
                    () -> proxyPort);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.username",
                    () -> "test");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.password",
                    () -> "bar");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username",
                    () -> "test");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password",
                    () -> "foo");
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClient.requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Timeouts")
    class TimeoutsIT {

        private static final int PROXY_PORT = 8666;
        private static final int RANDOM_CLIENT_TIMEOUT = new Random().nextInt(5000, 20000);

        private Stopwatch executionTimer = Stopwatch.createUnstarted();

        @Autowired protected FlareWebserviceClient flareClient;

        @Container
        public static ToxiproxyContainer toxiproxy = new ToxiproxyContainer(
                "ghcr.io/shopify/toxiproxy:" + TestConstantsFeasibility.TOXIPROXY_VERSION)
                .withNetwork(DEFAULT_CONTAINER_NETWORK)
                .dependsOn(flare);
        private static ToxiproxyClient toxiproxyClient;
        private static Proxy proxy;
        private static Latency latency;

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            var flareHost = toxiproxy.getHost();
            var flarePort = toxiproxy.getMappedPort(PROXY_PORT);

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.timeout.connect",
                    () -> RANDOM_CLIENT_TIMEOUT);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy",
                    () -> "structured-query");
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url",
                    () -> String.format("http://%s:%s/", flareHost, flarePort));
        }

        @BeforeAll
        static void setup() throws IOException {
            toxiproxyClient = new ToxiproxyClient(toxiproxy.getHost(), toxiproxy.getControlPort());
            proxy = toxiproxyClient.createProxy("flare", "0.0.0.0:" + PROXY_PORT,
                    format("%s:%s", flare.getNetworkAliases().get(0), flare.getExposedPorts().get(0)));
            latency = proxy.toxics().latency("latency", ToxicDirection.UPSTREAM, 0);
        }

        @BeforeEach
        void startClock() {
            executionTimer.reset().start();
        }

        @Test
        @DisplayName("flare client fails getting no response after given socket timeout")
        public void requestFeasibilityWithLongerProxyTimeoutFails() throws IOException {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json")
                    .openStream().readAllBytes();
            var proxyTimeout = RANDOM_CLIENT_TIMEOUT + 10000;
            latency.setLatency(proxyTimeout);

            assertThatThrownBy(() -> flareClient.requestFeasibility(rawStructuredQuery))
                    .describedAs(new Description() {

                        @Override
                        public String value() {
                            executionTimer.stop();
                            return format("execution time is %s ms", executionTimer.elapsed(TimeUnit.MILLISECONDS));
                        }
                    })
                    .isInstanceOf(IOException.class)
                    .hasMessageStartingWith("Error sending POST request to flare webservice")
                    .hasCauseInstanceOf(SocketTimeoutException.class)
                    .is(new Condition<>(
                            _e -> Duration.ofMillis(RANDOM_CLIENT_TIMEOUT).minus(executionTimer.elapsed()).isNegative(),
                            "executed longer than client timeout of %dms", RANDOM_CLIENT_TIMEOUT))
                    .is(new Condition<>(_e -> executionTimer.elapsed().minusMillis(proxyTimeout).isNegative(),
                            "executed shorter than proxy timeout of %dms", proxyTimeout));
        }

        @Test
        @DisplayName("flare client succeeds getting a response before given socket timeout")
        public void requestFeasibilityWithShorterProxyTimeoutSucceeds() throws IOException {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json")
                    .openStream().readAllBytes();
            latency.setLatency(RANDOM_CLIENT_TIMEOUT - 2000);

            assertThatNoException().isThrownBy(() -> flareClient.requestFeasibility(rawStructuredQuery));
        }
    }

}
