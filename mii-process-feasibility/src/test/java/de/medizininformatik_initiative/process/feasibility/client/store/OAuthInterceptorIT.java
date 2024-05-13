package de.medizininformatik_initiative.process.feasibility.client.store;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.net.URL;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class OAuthInterceptorIT {

    protected static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();

    @Container
    public static KeycloakContainer keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:24.0")
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("keycloak")
            .withAdminUsername("admin")
            .withAdminPassword("admin")
            .withRealmImportFile("de/medizininformatik_initiative/process/feasibility/client/store/realm-test.json")
            .withReuse(true);

    @Container
    public static GenericContainer<?> forwardProxyNoAuth = new GenericContainer<>(
            DockerImageName.parse("ubuntu/squid:6.1-23.10_edge"))
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .withExposedPorts(8080)
                    .withFileSystemBind(getResource("forward_proxy.conf").getPath(), "/etc/squid/squid.conf",
                            BindMode.READ_ONLY);

    @Container
    public static GenericContainer<?> forwardProxyBasicAuth = new GenericContainer<>(
            DockerImageName.parse("ubuntu/squid:6.1-23.10_edge"))
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .withExposedPorts(8080)
                    .withFileSystemBind(getResource("forward_proxy_basic_auth.conf").getPath(), "/etc/squid/squid.conf",
                            BindMode.READ_ONLY)
                    .withFileSystemBind(getResource("forward_proxy.htpasswd").getPath(), "/etc/squid/passwd",
                            BindMode.READ_ONLY);

    @Test
    public void getToken() {
        String tokenUrl = "http://" + keycloak.getHost() + ":" + keycloak.getFirstMappedPort()
                + "/realms/test/protocol/openid-connect/token";
        OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", tokenUrl, Optional.empty(),
                Optional.empty(), Optional.empty(), Optional.empty());

        String token = interceptor.getToken();

        assertThat(token).isNotNull();
    }

    @Test
    public void getTokenViaProxyNoAuth() {
        String tokenUrl = "http://keycloak:8080/realms/test/protocol/openid-connect/token";
            OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", tokenUrl,
                    Optional.of(forwardProxyNoAuth.getHost()),
                    Optional.of(forwardProxyNoAuth.getFirstMappedPort()),
                    Optional.empty(), Optional.empty());

            String token = interceptor.getToken();

            assertThat(token).isNotNull();
    }

    @Test
    public void getTokenViaProxyBasicAuth() {
        String tokenUrl = "http://keycloak:8080/realms/test/protocol/openid-connect/token";
        OAuthInterceptor interceptor = new OAuthInterceptor("account", "test", tokenUrl,
                Optional.of(forwardProxyBasicAuth.getHost()),
                Optional.of(forwardProxyBasicAuth.getFirstMappedPort()),
                Optional.of("test"),
                Optional.of("bar"));

        String token = interceptor.getToken();

        assertThat(token).isNotNull();
    }

    private static URL getResource(final String name) {
        return OAuthInterceptorIT.class.getResource(name);
    }
}
