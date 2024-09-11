package de.medizininformatik_initiative.process.feasibility.client.store;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testcontainers.containers.BindMode.READ_ONLY;

@Testcontainers
public class OAuthInterceptorIT {

    protected static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();
    private static URL nginxConf = getResource("nginx.conf");
    private static URL nginxTestProxyConfTemplate = getResource("keycloak_reverse_proxy.conf.template");
    private static URL indexFile = getResource("index.html");
    private static URL serverCertChain = getResource("../certs/server_cert_chain.pem");
    private static URL serverCertKey = getResource("../certs/server_cert_key.pem");
    private static URL trustStoreFile = getResource("../certs/ca.p12");

    @Container
    public static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:25.0")
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("keycloak")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withEnv("KC_PROXY_HEADERS", "xforwarded")
            .withRealmImportFile("de/medizininformatik_initiative/process/feasibility/client/store/realm-test.json")
            .withReuse(true);

    @Container
    public static GenericContainer<?> proxy = new GenericContainer<>(
            DockerImageName.parse("nginx:1.27.1"))
                    .withExposedPorts(8443)
                    .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                    .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                    .withFileSystemBind(nginxTestProxyConfTemplate.getPath(),
                            "/etc/nginx/templates/default.conf.template", READ_ONLY)
                    .withFileSystemBind(serverCertChain.getPath(), "/etc/nginx/certs/server_cert.pem", READ_ONLY)
                    .withFileSystemBind(serverCertKey.getPath(), "/etc/nginx/certs/server_cert_key.pem", READ_ONLY)
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .withNetworkAliases("proxy")
                    .dependsOn(keycloak);

    @Container
    public static GenericContainer<?> forwardProxyNoAuth = new GenericContainer<>(
            DockerImageName.parse("ubuntu/squid:6.1-23.10_edge"))
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .withExposedPorts(8080)
                    .withFileSystemBind(getResource("keycloak_forward_proxy.conf").getPath(), "/etc/squid/squid.conf",
                            BindMode.READ_ONLY);

    @Container
    public static GenericContainer<?> forwardProxyBasicAuth = new GenericContainer<>(
            DockerImageName.parse("ubuntu/squid:6.1-23.10_edge"))
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .withExposedPorts(8080)
                    .withFileSystemBind(getResource("keycloak_forward_proxy_basic_auth.conf").getPath(),
                            "/etc/squid/squid.conf", BindMode.READ_ONLY)
                    .withFileSystemBind(getResource("keycloak_forward_proxy.htpasswd").getPath(), "/etc/squid/passwd",
                            BindMode.READ_ONLY);

    @Test
    public void getToken() throws Exception {
        String issuerUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort() + "/realms/test";
        OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", issuerUrl, getTrustStore(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        String token = interceptor.getToken();

        assertThat(token).isNotNull();
    }

    @Test
    public void getTokenTls() throws Exception {
        String issuerUrl = "https://" + proxy.getHost() + ":" + proxy.getFirstMappedPort() + "/realms/test";
        OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", issuerUrl, getTrustStore(),
                Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());

        String token = interceptor.getToken();

        assertThat(token).isNotNull();
    }

    @Test
    public void getTokenViaForwardProxyNoAuth() throws Exception {
        String issuerUrl = "http://keycloak:8080/realms/test";
        OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", issuerUrl, getTrustStore(),
                Optional.of(forwardProxyNoAuth.getHost()),
                Optional.of(forwardProxyNoAuth.getFirstMappedPort()),
                Optional.empty(), Optional.empty());

        String token = interceptor.getToken();

        assertThat(token).isNotNull();
    }

    @Test
    public void getTokenViaForwardProxyNoAuthTls() throws Exception {
        String issuerUrl = "https://proxy:8443/realms/test";
        OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", issuerUrl, getTrustStore(),
                    Optional.of(forwardProxyNoAuth.getHost()),
                    Optional.of(forwardProxyNoAuth.getFirstMappedPort()),
                    Optional.empty(), Optional.empty());

            String token = interceptor.getToken();

            assertThat(token).isNotNull();
    }

    /* Forward proxy with basic authentication only works for non-tls connections by default since Java 8u111. */
    @Test
    public void getTokenViaForwardProxyBasicAuth() throws Exception {
        String issuerUrl = "http://keycloak:8080/realms/test";
        OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", issuerUrl, getTrustStore(),
                Optional.of(forwardProxyBasicAuth.getHost()),
                Optional.of(forwardProxyBasicAuth.getFirstMappedPort()),
                Optional.of("test"),
                Optional.of("bar"));

        String token = interceptor.getToken();

        assertThat(token).isNotNull();
    }

    private static KeyStore getTrustStore()
            throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, URISyntaxException {
        return KeyStore.getInstance(new File(trustStoreFile.toURI()), "changeit".toCharArray());
    }

    private static URL getResource(final String name) {
        return OAuthInterceptorIT.class.getResource(name);
    }
}
