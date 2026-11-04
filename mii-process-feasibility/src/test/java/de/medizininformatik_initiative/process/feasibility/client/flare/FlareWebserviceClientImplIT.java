package de.medizininformatik_initiative.process.feasibility.client.flare;

import com.google.common.base.Stopwatch;
import dasniko.testcontainers.keycloak.KeycloakContainer;
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
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
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

    protected static final String STORE_ID = "foo";
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

        private static URL feasibilityConfig = getResource("nonProxy.yml");

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            var flareHost = flare.getHost();
            var flarePort = flare.getFirstMappedPort();

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration.file",
                    () -> feasibilityConfig.getPath());
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL", () -> "http://%s:%s/".formatted(flareHost, flarePort));
        }

        @Test
        public void sendQuery() throws IOException {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json")
                    .openStream().readAllBytes();

            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy")
    class FwdProxyIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: http://flare:8080
                        evaluationStrategy: ccdl
                        proxy:
                          host: ${PROXY_HOST}
                          port: ${PROXY_PORT}

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("PROXY_HOST", () -> forwardProxy.getHost());
            registry.add("PROXY_PORT", () -> forwardProxy.getFirstMappedPort());
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth")
    class FwdProxyBasicAuthIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: http://flare:8080
                        evaluationStrategy: ccdl
                        proxy:
                          host: ${PROXY_HOST}
                          port: ${PROXY_PORT}
                          username: ${PROXY_USERNAME}
                          password: ${PROXY_PASSWORD}

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("PROXY_HOST", () -> forwardProxy.getHost());
            registry.add("PROXY_PORT", () -> forwardProxy.getFirstMappedPort());
            registry.add("PROXY_USERNAME", () -> "test");
            registry.add("PROXY_PASSWORD", () -> "bar");
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(() -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth with Reverse Proxy Bearer Token Auth")
    class FwdProxyBasicAuthRevProxyBearerTokenAuthIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: "http://proxy:8080/"
                        evaluationStrategy: ccdl
                        proxy:
                          host: ${PROXY_HOST}
                          port: ${PROXY_PORT}
                          username: ${PROXY_USERNAME}
                          password: ${PROXY_PASSWORD}
                        bearerAuth:
                          token: ${TOKEN}

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("PROXY_HOST", () -> forwardProxy.getHost());
            registry.add("PROXY_PORT", () -> forwardProxy.getFirstMappedPort());
            registry.add("PROXY_USERNAME", () -> "test");
            registry.add("PROXY_PASSWORD", () -> "bar");
            registry.add("TOKEN", () -> bearerToken);
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Basic Auth")
    class RevProxyBasicAuthIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_basic_auth.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL passwordFile = getResource("reverse_proxy.htpasswd");

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: ${BASE_URL_${STORE_ID}}
                        evaluationStrategy: ccdl
                        basicAuth:
                          username: test
                          password: foo

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL_foo",
                    () -> "http://%s:%s/".formatted(proxy.getHost(), proxy.getFirstMappedPort().toString()));
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Bearer Token Auth")
    class RevProxyBearerTokenAuthIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: ${BASE_URL}
                        evaluationStrategy: ccdl
                        bearerAuth:
                          token: "1234"

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL",
                    () -> "http://%s:%s/".formatted(proxy.getHost(), proxy.getFirstMappedPort().toString()));
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy TLS")
    class RevProxyTlsIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.pem");

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: ${BASE_URL}
                        evaluationStrategy: ccdl
                        trustedCACertificates: ${TRUSTED_CA_FILE}

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL",
                    () -> "https://%s:%s/".formatted(proxy.getHost(), proxy.getFirstMappedPort().toString()));
            registry.add("TRUSTED_CA_FILE", () -> trustStoreFile.getPath());
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Basic Auth with TLS")
    class RevProxyTlsBasicAuthIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.pem");
        private static URL passwordFile = getResource("reverse_proxy.htpasswd");

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: ${BASE_URL}
                        evaluationStrategy: ccdl
                        trustedCACertificates: ${TRUSTED_CA_FILE}
                        basicAuth:
                          username: test
                          password: foo

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL",
                    () -> "https://%s:%s/".formatted(proxy.getHost(), proxy.getFirstMappedPort().toString()));
            registry.add("TRUSTED_CA_FILE", () -> trustStoreFile.getPath());
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy Client Certificate Auth")
    class RevProxyTlsClientCertIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.pem");
        private static URL clientCertificateFile = getResource("../certs/client_cert.pem");
        private static URL privateKeyFile = getResource("../certs/client_cert_key.pem");

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: ${BASE_URL}
                        evaluationStrategy: ccdl
                        trustedCACertificates: ${TRUSTED_CA_FILE}
                        clientCertificate: ${CLIENT_CERTIFICATE_FILE}
                        privateKey: ${PRIVATE_KEY_FILE}

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}

                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL",
                    () -> "https://%s:%s/".formatted(proxy.getHost(), proxy.getFirstMappedPort().toString()));
            registry.add("TRUSTED_CA_FILE", () -> trustStoreFile.getPath());
            registry.add("CLIENT_CERTIFICATE_FILE", () -> clientCertificateFile.getPath());
            registry.add("PRIVATE_KEY_FILE", () -> privateKeyFile.getPath());
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Reverse Proxy OAuth Authentication")
    class FlareWebserviceClientImplRevProxyOAuthIT {
        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("keycloak_reverse_proxy.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustStoreFile = getResource("../certs/ca.pem");

        @Container public static KeycloakContainer keycloak = new KeycloakContainer(
                "quay.io/keycloak/keycloak:" + TestConstantsFeasibility.KEYCLOAK_VERSION)
                .withNetwork(DEFAULT_CONTAINER_NETWORK)
                .withNetworkAliases("keycloak")
                .withAdminUsername("admin")
                .withAdminPassword("admin")
                .withEnv("KC_PROXY_HEADERS", "xforwarded")
                .withRealmImportFile("de/medizininformatik_initiative/process/feasibility/client/store/realm-test.json")
                .withReuse(true);

        @Container public static GenericContainer<?> proxy = new GenericContainer<>(
                DockerImageName.parse("nginx:" + TestConstantsFeasibility.NGINX_VERSION))
                        .withExposedPorts(8443)
                        .withNetworkAliases("proxy")
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
        static void dynamicProperties(DynamicPropertyRegistry registry) throws IOException {
            var flareHost = "localhost";
            var flarePort = 44180;
            var proxyHost = proxy.getHost();
            var proxyPort = proxy.getFirstMappedPort();
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: ${BASE_URL}
                        evaluationStrategy: ccdl
                        trustedCACertificates: ${TRUST_STORE_FILE}
                        oAuth:
                          issuerUrl: ${ISSUER_URL}
                          clientId: ${CLIENT_ID}
                          clientPassword: ${CLIENT_SECRET}
                    networks:
                      foo.bar:
                        obfuscate: false
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL", () -> "http://%s:%s".formatted(flareHost, flarePort));
            registry.add("TRUST_STORE_FILE", () -> trustStoreFile.getPath());
            registry.add("ISSUER_URL", () -> "https://%s:%s/realms/test".formatted(proxyHost, proxyPort));
            registry.add("CLIENT_ID", () -> "account");
            registry.add("CLIENT_SECRET", () -> "test");
        }

        @Test
        void sendQuery() throws Exception {

            try (GenericContainer<?> oAuth2Proxy = new GenericContainer<>(
                    DockerImageName.parse("quay.io/oauth2-proxy/oauth2-proxy:" + TestConstantsFeasibility.OAUTH2_PROXY_VERSION))
                            .withNetworkMode("host")
                            .withCopyFileToContainer(MountableFile.forHostPath(trustStoreFile.getPath()),
                                    "/secrets/trusted_cas.pem")
                            .withEnv("OAUTH2_PROXY_HTTP_ADDRESS", "http://0.0.0.0:44180")
                            .withEnv("OAUTH2_PROXY_UPSTREAMS",
                                    "http://%s:%d".formatted(flare.getHost(), flare.getFirstMappedPort()))
                            .withEnv("OAUTH2_PROXY_PROVIDER", "oidc")
                            .withEnv("OAUTH2_PROXY_PROVIDER_CA_FILES", "/secrets/trusted_cas.pem")
                            .withEnv("OAUTH2_PROXY_OIDC_ISSUER_URL",
                                    "https://%s:%d/realms/test".formatted(proxy.getHost(), proxy.getFirstMappedPort()))
                            .withEnv("OAUTH2_PROXY_CLIENT_ID", "account")
                            .withEnv("OAUTH2_PROXY_CLIENT_SECRET", "foobar")
                            .withEnv("OAUTH2_PROXY_COOKIE_SECRET", "foobar0123456789")
                            .withEnv("OAUTH2_PROXY_SKIP_JWT_BEARER_TOKENS", "true")
                            .withEnv("OAUTH2_PROXY_OIDC_AUDIENCE_CLAIMS", "azp")
                            .withEnv("OAUTH2_PROXY_INSECURE_OIDC_ALLOW_UNVERIFIED_EMAIL", "true")
                            .withEnv("OAUTH2_PROXY_EMAIL_DOMAINS", "*")
                            .waitingFor(waitForHostUrl("http://localhost:44180"));) {
                var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                        .readAllBytes();
                oAuth2Proxy.start();
                var feasibility = assertDoesNotThrow(
                        () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
                assertEquals(0, feasibility);
            }
        }

        private AbstractWaitStrategy waitForHostUrl(String url) {
            return new AbstractWaitStrategy() {

                @Override
                protected void waitUntilReady() {
                    try {
                        var request = HttpRequest.newBuilder(URI.create(url))
                                .GET()
                                .build();
                        var client = HttpClient.newHttpClient();
                        var retries = 0;
                        var statusCode = 0;

                        while (statusCode != 403 && retries < 10) {
                            if (retries > 0) {
                                Thread.sleep(500);
                            }
                            try {
                                statusCode = client.send(request, BodyHandlers.ofString()).statusCode();
                            } catch (IOException e) {
                                statusCode = 0;
                            }
                            retries++;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException("Waiting for oauth2-proxy cancelled.", e);
                    }
                }
            };
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth with Reverse Proxy TLS")
    class FwdProxyBasicAuthRevProxyTlsIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

        private static URL squidProxyConf = getResource("forward_proxy_basic_auth.conf");
        private static URL passwordFile = getResource("forward_proxy.htpasswd");
        private static URL nginxConf = getResource("nginx.conf");
        private static URL nginxTestProxyConfTemplate = getResource("reverse_proxy_tls.conf.template");
        private static URL indexFile = getResource("index.html");
        private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
        private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
        private static URL trustedCAFile = getResource("../certs/ca.pem");

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: https://proxy:8443/
                        evaluationStrategy: ccdl
                        trustedCACertificates: ${TRUSTED_CA_FILE}
                        proxy:
                          host: ${PROXY_HOST}
                          port: ${PROXY_PORT}
                          username: ${PROXY_USERNAME}
                          password: ${PROXY_PASSWORD}

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("PROXY_HOST", () -> forwardProxy.getHost());
            registry.add("PROXY_PORT", () -> forwardProxy.getFirstMappedPort());
            registry.add("PROXY_USERNAME", () -> "test");
            registry.add("PROXY_PASSWORD", () -> "bar");
            registry.add("TRUSTED_CA_FILE", () -> trustedCAFile.getPath());
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth with Reverse Proxy Basic Auth")
    class FwdRevProxyBasicAuthIT {

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

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
            var config = """
                    stores:
                      ${STORE_ID}:
                        baseUrl: http://proxy:8080
                        evaluationStrategy: ccdl
                        proxy:
                          host: ${PROXY_HOST}
                          port: ${PROXY_PORT}
                          username: test
                          password: bar
                        basicAuth:
                          username: test
                          password: foo

                    networks:
                      medizininformatik-initiative.de:
                        obfuscate: true
                        stores:
                        - ${STORE_ID}
                    """;

            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration", () -> config);
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("PROXY_HOST", () -> forwardProxy.getHost());
            registry.add("PROXY_PORT", () -> forwardProxy.getFirstMappedPort());
        }

        @Test
        void sendQuery() throws Exception {
            var rawStructuredQuery = this.getClass().getResource("valid-structured-query.json").openStream()
                    .readAllBytes();
            var feasibility = assertDoesNotThrow(
                    () -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
            assertEquals(0, feasibility);
        }
    }

    @Nested
    @DisplayName("Timeouts")
    class TimeoutsIT {

        private static final int PROXY_PORT = 8666;
        private static final Integer RANDOM_CLIENT_TIMEOUT = new Random().nextInt(5000, 20000);
        private static URL feasibilityConfig = getResource("nonProxy_timeout.yml");

        private Stopwatch executionTimer = Stopwatch.createUnstarted();

        @Autowired protected Map<String, FlareWebserviceClient> flareClients;

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
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.configuration.file",
                    () -> feasibilityConfig.getPath());
            registry.add("STORE_ID", () -> STORE_ID);
            registry.add("BASE_URL",
                    () -> "http://%s:%d/".formatted(toxiproxy.getHost(), toxiproxy.getMappedPort(PROXY_PORT)));
            registry.add("TIMEOUT", () -> RANDOM_CLIENT_TIMEOUT.toString());
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

            assertThatThrownBy(() -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery))
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

            assertThatNoException().isThrownBy(() -> flareClients.get(STORE_ID).requestFeasibility(rawStructuredQuery));
        }
    }

}
