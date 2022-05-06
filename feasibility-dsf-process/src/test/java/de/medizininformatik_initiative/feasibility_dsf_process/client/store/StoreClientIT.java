package de.medizininformatik_initiative.feasibility_dsf_process.client.store;

import ca.uhn.fhir.context.FhirContext;
import de.medizininformatik_initiative.feasibility_dsf_process.client.store.StoreClientConfiguration.ConnectionConfiguration;
import de.medizininformatik_initiative.feasibility_dsf_process.client.store.StoreClientConfiguration.ProxyConfiguration;
import de.medizininformatik_initiative.feasibility_dsf_process.client.store.StoreClientConfiguration.StoreAuthenticationConfiguration;
import lombok.NonNull;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.apache.http.ssl.SSLContexts;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.Objects;

import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.junit.jupiter.api.Assertions.*;
import static org.testcontainers.containers.BindMode.READ_ONLY;

@Tag("client")
@Tag("store")
@Testcontainers
public class StoreClientIT {

    private static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();

    @Container
    public GenericContainer<?> fhirServer = new GenericContainer<>(DockerImageName.parse("ghcr.io/samply/blaze:0.16.5"))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("fhir-server")
            .withEnv("LOG_LEVEL", "debug")
            .withReuse(true);

    private StoreClientFactory storeClientFactory;

    private static SSLContext DEFAULT_SSL_CONTEXT;

    private static ConnectionConfiguration DEFAULT_CONNECTION_CONFIGURATION;

    @BeforeAll
    static void setUpDefaults() throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        var defaultTrustStore = DefaultTrustStoreUtils.loadDefaultTrustStore();
        DEFAULT_SSL_CONTEXT = SSLContexts.custom()
                .loadTrustMaterial(defaultTrustStore, null)
                .build();

        DEFAULT_CONNECTION_CONFIGURATION = ConnectionConfiguration.builder()
                .connectionTimeoutMs(2000)
                .connectionRequestTimeoutMs(20000)
                .socketTimeoutMs(2000)
                .build();
    }

    @BeforeEach
    void createStoreClientFactory() {
        var fhirContext = FhirContext.forR4();
        storeClientFactory = new StoreClientFactory(fhirContext);
    }

    @Test
    public void configuredBearerTokenGetsSent() throws InterruptedException, IOException {
        var investigationServer = createProxyServer(getTestFhirServerUrl());
        investigationServer.start();

        var bearerToken = "not-a-bearer-token-but-sufficient-for-test";
        var authCfg = StoreAuthenticationConfiguration.builder()
                .bearerToken(bearerToken)
                .build();
        var clientCfg = StoreClientConfiguration.builder()
                .sslContext(DEFAULT_SSL_CONTEXT)
                .connectionConfiguration(DEFAULT_CONNECTION_CONFIGURATION)
                .storeAuthenticationConfiguration(authCfg)
                .build();

        var investigationServerUrl = getProxyServerUrl(investigationServer)
                .toString();
        var client = storeClientFactory.createClient(investigationServerUrl, clientCfg);
        client.capabilities().ofType(CapabilityStatement.class).execute();

        var recordedRequest = investigationServer.takeRequest();
        assertEquals("Bearer " + bearerToken, recordedRequest.getHeader(AUTHORIZATION));

        investigationServer.close();
    }

    @Test
    public void configuredBasicAuthCredentialsGetSent() throws InterruptedException, IOException {
        var investigationServer = createProxyServer(getTestFhirServerUrl());
        investigationServer.start();

        var basicAuthUsername = "test";
        var basicAuthPassword = "foo";
        var authCfg = StoreAuthenticationConfiguration.builder()
                .basicAuthUsername(basicAuthUsername)
                .basicAuthPassword(basicAuthPassword)
                .build();
        var clientCfg = StoreClientConfiguration.builder()
                .sslContext(DEFAULT_SSL_CONTEXT)
                .connectionConfiguration(DEFAULT_CONNECTION_CONFIGURATION)
                .storeAuthenticationConfiguration(authCfg)
                .build();

        var investigationServerUrl = getProxyServerUrl(investigationServer)
                .toString();
        var client = storeClientFactory.createClient(investigationServerUrl, clientCfg);
        client.capabilities().ofType(CapabilityStatement.class).execute();

        var recordedRequest = investigationServer.takeRequest();

        var basicAuthEncoded = Base64.getEncoder().encodeToString(String.format("%s:%s", basicAuthUsername,
                basicAuthPassword).getBytes());
        assertEquals("Basic " + basicAuthEncoded, recordedRequest.getHeader(AUTHORIZATION));

        investigationServer.close();
    }

    @Test
    public void requestToFhirServerWithoutProxySucceeds() {
        var clientCfg = StoreClientConfiguration.builder()
                .sslContext(DEFAULT_SSL_CONTEXT)
                .connectionConfiguration(DEFAULT_CONNECTION_CONFIGURATION)
                .build();
        var client = storeClientFactory.createClient(getTestFhirServerUrl().toString(), clientCfg);
        var res = client.capabilities().ofType(CapabilityStatement.class).execute();

        assertNotNull(res);
    }


    @Test
    public void testRequestToReverseProxyWithSelfSignedCertificate() throws IOException {
        var localhost = InetAddress.getByName("localhost").getCanonicalHostName();
        var localhostCert = new HeldCertificate.Builder()
                .addSubjectAlternativeName(localhost)
                .build();

        var proxyServerCert = new HandshakeCertificates.Builder()
                .heldCertificate(localhostCert)
                .build();

        var testFhirServerUrl = getTestFhirServerUrl();
        var proxyServer = createProxyServer(testFhirServerUrl, proxyServerCert.sslContext());
        proxyServer.start();

        var trustedCerts = new HandshakeCertificates.Builder()
                .addTrustedCertificate(localhostCert.certificate())
                .build();

        var clientCfg = StoreClientConfiguration.builder()
                .sslContext(trustedCerts.sslContext())
                .connectionConfiguration(DEFAULT_CONNECTION_CONFIGURATION)
                .build();

        var client = storeClientFactory.createClient(getHttpsProxyServerUrl(proxyServer).toString(),
                clientCfg);
        assertDoesNotThrow(() -> client.capabilities().ofType(CapabilityStatement.class).execute());

        proxyServer.close();
    }

    @Test
    public void testRequestToReverseProxyWithClientCert() throws KeyStoreException, CertificateException, IOException,
            NoSuchAlgorithmException, UnrecoverableKeyException, KeyManagementException {
        // Make sure to run `create_certs_for_store_client_tests.sh` from the `scripts` directory first. This will be
        // automatically triggered by maven when trying to run integration tests as it is coupled with a phase called
        // `pre-integration-test`.
        var nginxConf = getResource("nginx.conf");
        var nginxTestProxyConfTemplate = getResource("reverse_proxy_client_cert.conf.template");
        var staticFhirMetadata = getResource("fhir_metadata.json");
        var indexFile = getResource("index.html");
        var trustedCerts = getResource("./certs/ca.pem");
        var serverCertChain = getResource("./certs/server_cert_chain.pem");
        var serverCertKey = getResource("./certs/server_cert_key.pem");

        NginxContainer<?> nginx = new NginxContainer<>("nginx:1.21.6")
                .withExposedPorts(80)
                .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                .withFileSystemBind(staticFhirMetadata.getPath(), "/static/fhir_metadata.json", READ_ONLY)
                .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                .withFileSystemBind(nginxTestProxyConfTemplate.getPath(), "/etc/nginx/templates/default.conf.template", READ_ONLY)
                .withFileSystemBind(trustedCerts.getPath(), "/etc/nginx/certificates/clientCA.pem", READ_ONLY)
                .withFileSystemBind(serverCertChain.getPath(), "/etc/nginx/certs/server_cert.pem", READ_ONLY)
                .withFileSystemBind(serverCertKey.getPath(), "/etc/nginx/certs/server_cert_key.pem", READ_ONLY)
                .withNetwork(DEFAULT_CONTAINER_NETWORK);
        nginx.start();

        var serverTrustStoreStream = getResourceAsStream("./certs/ca.p12");
        var trustStore = KeyStore.getInstance("PKCS12");
        trustStore.load(serverTrustStoreStream, "changeit".toCharArray());
        serverTrustStoreStream.close();

        var clientCertStream = getResourceAsStream("./certs/client_key_store.p12");
        var keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(clientCertStream, "changeit".toCharArray());
        clientCertStream.close();

        var sslContextWithClientCert = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null)
                .loadKeyMaterial(keyStore, "changeit".toCharArray())
                .build();

        var clientCfg = StoreClientConfiguration.builder()
                .sslContext(sslContextWithClientCert)
                .connectionConfiguration(DEFAULT_CONNECTION_CONFIGURATION)
                .build();

        var nginxUrl = String.format("https://%s:%d/fhir/", nginx.getHost(), nginx.getFirstMappedPort());
        var client = storeClientFactory.createClient(nginxUrl, clientCfg);

        assertDoesNotThrow(() -> client.capabilities().ofType(CapabilityStatement.class).execute());

        nginx.close();
    }

    @Test
    public void testRequestToReverseProxyWithCredentials() {
        var nginxConf = getResource("nginx.conf");
        var nginxTestProxyConfTemplate = getResource("reverse_proxy_with_credentials.conf.template");
        var staticFhirMetadata = getResource("fhir_metadata.json");
        var indexFile = getResource("index.html");
        var passwordFile = getResource(".htpasswd");

        NginxContainer<?> nginx = new NginxContainer<>("nginx:1.21.6")
                .withExposedPorts(80)
                .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                .withFileSystemBind(staticFhirMetadata.getPath(), "/static/fhir_metadata.json", READ_ONLY)
                .withFileSystemBind(indexFile.getPath(), "/usr/share/nginx/html/index.html", READ_ONLY)
                .withFileSystemBind(passwordFile.getPath(), "/etc/auth/.htpasswd", READ_ONLY)
                .withFileSystemBind(nginxTestProxyConfTemplate.getPath(), "/etc/nginx/templates/default.conf.template", READ_ONLY)
                .withNetwork(DEFAULT_CONTAINER_NETWORK);
        nginx.start();

        var authCfg = StoreAuthenticationConfiguration.builder()
                .basicAuthUsername("foo")
                .basicAuthPassword("bar")
                .build();

        var clientCfg = StoreClientConfiguration.builder()
                .sslContext(DEFAULT_SSL_CONTEXT)
                .connectionConfiguration(DEFAULT_CONNECTION_CONFIGURATION)
                .storeAuthenticationConfiguration(authCfg)
                .build();

        var nginxUrl = String.format("http://%s:%d/fhir/", nginx.getHost(), nginx.getFirstMappedPort());
        var client = storeClientFactory.createClient(nginxUrl, clientCfg);
        assertDoesNotThrow(() -> client.capabilities().ofType(CapabilityStatement.class).execute());

        nginx.close();
    }

    @Test
    public void testRequestWithForwardProxy() {
        var nginxConf = this.getClass().getResource("nginx.conf");
        var forwardProxyConfigTemplate = getResource("forward_proxy.conf.template");

        NginxContainer<?> nginx = new NginxContainer<>("nginx:1.21.6")
                .withExposedPorts(80)
                .withFileSystemBind(nginxConf.getPath(), "/etc/nginx/nginx.conf", READ_ONLY)
                .withFileSystemBind(forwardProxyConfigTemplate.getPath(), "/etc/nginx/templates/default.conf.template", READ_ONLY)
                .withNetwork(DEFAULT_CONTAINER_NETWORK);
        nginx.start();

        var proxyCfg = ProxyConfiguration.builder()
                .proxyHost(nginx.getHost())
                .proxyPort(nginx.getFirstMappedPort())
                .build();
        var clientCfg = StoreClientConfiguration.builder()
                .sslContext(DEFAULT_SSL_CONTEXT)
                .connectionConfiguration(DEFAULT_CONNECTION_CONFIGURATION)
                .proxyConfiguration(proxyCfg)
                .build();

        var client = storeClientFactory.createClient("http://fhir-server:8080/fhir/", clientCfg);
        var res = client.capabilities().ofType(CapabilityStatement.class).execute();

        assertNotNull(res);

        nginx.close();
    }

    private MockWebServer createProxyServer(HttpUrl proxyTargetUrl) {
        var mockServer = new MockWebServer();
        mockServer.setDispatcher(new MockServerProxyDispatcher(new OkHttpClient.Builder().build(), proxyTargetUrl));
        return mockServer;
    }

    private MockWebServer createProxyServer(HttpUrl proxyTargetUrl, SSLContext sslContext) {
        var mockServer = createProxyServer(proxyTargetUrl);
        mockServer.useHttps(sslContext.getSocketFactory(), false);
        return mockServer;
    }

    @NonNull
    private HttpUrl getTestFhirServerUrl() {
        return Objects.requireNonNull(HttpUrl.parse(String.format("http://%s:%d/fhir/",
                        fhirServer.getHost(),
                        fhirServer.getFirstMappedPort())),
                "Can not parse URL of FHIR server.");
    }

    private HttpUrl getProxyServerUrl(MockWebServer proxyServer) {
        return HttpUrl.parse(String.format("http://%s:%d/fhir/", proxyServer.getHostName(), proxyServer.getPort()));
    }

    private HttpUrl getHttpsProxyServerUrl(MockWebServer proxyServer) {
        return getProxyServerUrl(proxyServer).newBuilder().scheme("https")
                .build();
    }

    private URL getResource(final String name) {
        return this.getClass().getResource(name);
    }

    private InputStream getResourceAsStream(final String name) {
        return this.getClass().getResourceAsStream(name);
    }
}
