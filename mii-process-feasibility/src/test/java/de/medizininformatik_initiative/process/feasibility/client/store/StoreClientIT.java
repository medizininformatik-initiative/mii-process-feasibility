package de.medizininformatik_initiative.process.feasibility.client.store;

import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.NonNull;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.tls.HandshakeCertificates;
import okhttp3.tls.HeldCertificate;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CapabilityStatement;
import org.hl7.fhir.r4.model.DateType;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import javax.net.ssl.SSLContext;

import static de.medizininformatik_initiative.process.feasibility.client.store.MockServerProxyDispatcher.createForwardProxyDispatcher;
import static de.medizininformatik_initiative.process.feasibility.client.store.MockServerProxyDispatcher.createReverseProxyDispatcher;
import static de.medizininformatik_initiative.process.feasibility.client.variables.TestConstantsFeasibility.BLAZE_VERSION;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.HEADER_PREFER_RESPOND_ASYNC;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_END;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_PERIOD_START;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.MEASURE_REPORT_TYPE_POPULATION;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_LOCATION;
import static org.apache.http.HttpHeaders.PROXY_AUTHORIZATION;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("client")
@Tag("store")
@SpringBootTest(classes = StoreClientSpringConfig.class)
@Testcontainers
public class StoreClientIT {

    private static final Network DEFAULT_CONTAINER_NETWORK = Network.newNetwork();

    private static GenericContainer<?> fhirServer = new GenericContainer<>(
            DockerImageName.parse("samply/blaze:" + BLAZE_VERSION))
            .withExposedPorts(8080)
            .withNetwork(DEFAULT_CONTAINER_NETWORK)
            .withNetworkAliases("fhir-server")
            .withEnv("LOG_LEVEL", "debug")
            .withReuse(true);

    @BeforeAll
    static void init() {
        fhirServer.start();
    }

    @AfterAll
    static void shutdown() {
        fhirServer.stop();
    }

    private static MockWebServer createProxyServer(Dispatcher dispatcher) {
        var mockServer = new MockWebServer();
        mockServer.setDispatcher(dispatcher);
        return mockServer;
    }

    private static MockWebServer createForwardProxyServer(HttpUrl proxyTargetUrl) {
        var mockServer = new MockWebServer();
        mockServer.setDispatcher(createForwardProxyDispatcher(new OkHttpClient.Builder().build(), proxyTargetUrl));
        return mockServer;
    }

    private static MockWebServer createProxyServer(Dispatcher dispatcher, SSLContext sslContext) {
        var mockServer = createProxyServer(dispatcher);
        mockServer.useHttps(sslContext.getSocketFactory(), false);
        return mockServer;
    }

    @NonNull
    private static HttpUrl getTestFhirServerUrl() {
        return Objects.requireNonNull(HttpUrl.parse(String.format("http://%s:%d/fhir/",
                        fhirServer.getHost(),
                        fhirServer.getFirstMappedPort())),
                "Can not parse URL of FHIR server.");
    }

    @Nested
    @DisplayName("No Proxy")
    class NoProxy {

        @Autowired @Qualifier("store-client") protected IGenericClient storeClient;

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url",
                    () -> "http://%s:%s/fhir/".formatted(fhirServer.getHost(), fhirServer.getFirstMappedPort()));
        }

        @Test
        @DisplayName("direct access without forwardProxy succeeds")
        void noProxy() throws InterruptedException {
            var capabilities = storeClient.capabilities().ofType(CapabilityStatement.class).execute();

            assertThat(capabilities.getSoftware().getName()).containsIgnoringCase("blaze");
        }
    }

    @Nested
    @DisplayName("Basic Auth")
    class RevProxyBasicAuth {

        private static final String BASIC_AUTH_USERNAME = "foo";
        private static final String BASIC_AUTH_PASSWORD = "bar";

        static MockWebServer proxy = createProxyServer(
                createReverseProxyDispatcher(new OkHttpClient.Builder().build(), getTestFhirServerUrl()));

        @AfterAll
        static void tearDown() throws IOException {
            proxy.close();
        }

        @Autowired @Qualifier("store-client") protected IGenericClient storeClient;
        static final String BEARER_TOKEN = "not-a-bearer-token-but-sufficient-for-test";

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username",
                    () -> BASIC_AUTH_USERNAME);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password",
                    () -> BASIC_AUTH_PASSWORD);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url",
                    () -> "http://%s:%s/fhir/".formatted(proxy.getHostName(), proxy.getPort()));
        }

        @Test
        @DisplayName("configured basic auth credentials are set in authorization header")
        void basicAuth() throws InterruptedException {
            var basicAuthEncoded = "Basic %s".formatted(Base64.getEncoder()
                    .encodeToString("%s:%s".formatted(BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD).getBytes()));

            var capabilities = storeClient.capabilities().ofType(CapabilityStatement.class).execute();
            var recordedRequest = proxy.takeRequest();
            assertThat(capabilities.getSoftware().getName()).containsIgnoringCase("blaze");
            assertThat(recordedRequest.getHeader(AUTHORIZATION)).isEqualTo(basicAuthEncoded);
        }
    }

    @Nested
    @DisplayName("Bearer Token")
    class RevProxyBearerToken {

        static MockWebServer proxy = createProxyServer(
                createReverseProxyDispatcher(new OkHttpClient.Builder().build(), getTestFhirServerUrl()));

        @AfterAll
        static void tearDown() throws IOException {
            proxy.close();
        }

        @Autowired @Qualifier("store-client") protected IGenericClient storeClient;
        static final String BEARER_TOKEN = "not-a-bearer-token-but-sufficient-for-test";

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.bearer.token",
                    () -> BEARER_TOKEN);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url",
                    () -> "http://%s:%s/fhir/".formatted(proxy.getHostName(), proxy.getPort()));
        }

        @Test
        @DisplayName("configured bearer token is set in request header")
        void bearerToken() throws InterruptedException {
            var capabilities = storeClient.capabilities().ofType(CapabilityStatement.class).execute();
            var recordedRequest = proxy.takeRequest();

            assertThat(capabilities.getSoftware().getName()).containsIgnoringCase("blaze");
            assertThat(recordedRequest.getHeader(AUTHORIZATION)).isNotNull().contains(BEARER_TOKEN);
        }
    }

    @Nested
    @DisplayName("Client Certificate")
    class RevProxyClientCert {

        private static final String CLIENT_CERT_PASSWORD = "foobar";
        private static final String SERVER_CERT_PASSWORD = "barfoo";
        static File clientCertificateStore;
        static File serverCertificateStore;
        static HeldCertificate rootCertificate = new HeldCertificate.Builder().certificateAuthority(0).build();
        static HeldCertificate clientCertificate = new HeldCertificate.Builder().signedBy(rootCertificate).build();
        static HeldCertificate serverCertificate = new HeldCertificate.Builder().commonName("ingen")
                .addSubjectAlternativeName("localhost")
                .signedBy(rootCertificate)
                .build();
        static HandshakeCertificates serverCertificates = new HandshakeCertificates.Builder()
                .addTrustedCertificate(rootCertificate.certificate())
                .heldCertificate(serverCertificate)
                .build();
        static MockWebServer proxy = createProxyServer(
                createReverseProxyDispatcher(new OkHttpClient.Builder().build(), getTestFhirServerUrl()),
                serverCertificates.sslContext());

        @BeforeAll
        static void setUp() throws Exception {
            clientCertificateStore = createCertificateStore(clientCertificate, CLIENT_CERT_PASSWORD);
            serverCertificateStore = createCertificateStore(serverCertificate, SERVER_CERT_PASSWORD);
            proxy.requestClientAuth();
        }

        private static File createCertificateStore(HeldCertificate certificate, String password)
                throws KeyStoreException, IOException, NoSuchAlgorithmException,
                CertificateException, FileNotFoundException {
            var store = KeyStore.getInstance("PKCS12");
            var tempFile = File.createTempFile("cert", ".p12");
            tempFile.deleteOnExit();
            store.load(null, null);
            store.setKeyEntry("cert", certificate.keyPair().getPrivate(), password.toCharArray(),
                    new java.security.cert.X509Certificate[] { certificate.certificate() });
            store.store(new FileOutputStream(tempFile), password.toCharArray());
            return tempFile;
        }

        @AfterAll
        static void tearDown() throws IOException {
            proxy.close();
        }

        @Autowired @Qualifier("store-client") protected IGenericClient storeClient;

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.key_store_path",
                    () -> clientCertificateStore.getAbsolutePath());
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.key_store_password",
                    () -> CLIENT_CERT_PASSWORD);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_path",
                    () -> serverCertificateStore.getAbsolutePath());
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_password",
                    () -> SERVER_CERT_PASSWORD);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url",
                    () -> "https://%s:%s/fhir/".formatted(proxy.getHostName(), proxy.getPort()));
        }

        @Test
        @DisplayName("configured client certificate is sent to forwardProxy")
        void clientCert() throws InterruptedException {
            var capabilities = storeClient.capabilities().ofType(CapabilityStatement.class).execute();
            var recordedRequest = proxy.takeRequest();

            assertThat(capabilities.getSoftware().getName()).containsIgnoringCase("blaze");
            assertThat(recordedRequest.getHandshake().peerPrincipal())
                    .isEqualTo(clientCertificate.certificate().getSubjectX500Principal());
        }
    }

    @Nested
    @DisplayName("Forward Proxy Basic Auth")
    class ForwardProxyBasicAuth {

        private static final String BASIC_AUTH_USERNAME = "foo";
        private static final String BASIC_AUTH_PASSWORD = "bar";

        static MockWebServer forwardProxy = createForwardProxyServer(getTestFhirServerUrl());

        @AfterAll
        static void tearDown() throws IOException {
            forwardProxy.close();
        }

        @Autowired @Qualifier("store-client") protected IGenericClient storeClient;
        static final String BEARER_TOKEN = "not-a-bearer-token-but-sufficient-for-test";

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.host",
                    () -> forwardProxy.getHostName());
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.port",
                    () -> forwardProxy.getPort());
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.username",
                    () -> BASIC_AUTH_USERNAME);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.password",
                    () -> BASIC_AUTH_PASSWORD);
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url",
                    () -> "http://%s:%s/fhir/".formatted(fhirServer.getHost(), fhirServer.getFirstMappedPort()));
        }

        @Test
        @DisplayName("configured forward proxy is used with basic auth credentials")
        void basicAuth() throws InterruptedException {
            var basicAuthEncoded = "Basic %s".formatted(Base64.getEncoder()
                    .encodeToString("%s:%s".formatted(BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD).getBytes()));

            var capabilities = storeClient.capabilities().ofType(CapabilityStatement.class).execute();

            if (forwardProxy.getRequestCount() == 2) {
                var recordedRequest = forwardProxy.takeRequest(); // first request is the unauthorized one
                assertThat(recordedRequest.getHeaders().names()).doesNotContain(PROXY_AUTHORIZATION);
            }
            var recordedRequest = forwardProxy.takeRequest();
            assertThat(capabilities.getSoftware().getName()).containsIgnoringCase("blaze");
            assertThat(recordedRequest.getHeader(PROXY_AUTHORIZATION)).isEqualTo(basicAuthEncoded);
        }
    }

    @Nested
    @DisplayName("Asynchronous Request")
    class AsyncRequest {

        static MockServerProxyDispatcher dispatcher = createReverseProxyDispatcher(new OkHttpClient.Builder().build(),
                getTestFhirServerUrl());
        static MockWebServer proxy = createProxyServer(dispatcher);

        @AfterAll
        static void tearDown() throws IOException {
            proxy.close();
        }

        protected IGenericClient storeClient;
        protected String measureUrl;

        @DynamicPropertySource
        static void dynamicProperties(DynamicPropertyRegistry registry) {
            registry.add("de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url",
                    () -> "http://%s:%s/fhir/".formatted(proxy.getHostName(), proxy.getPort()));
        }

        public AsyncRequest(@Autowired @Qualifier("store-client") IGenericClient storeClient) {
            this.storeClient = storeClient;
            this.measureUrl = uploadMeasure(storeClient);
        }

        private String uploadMeasure(IGenericClient client) throws IllegalStateException {
            try {
                var bundle = Files
                        .readString(Paths.get(this.getClass().getResource("measure-library-bundle.json").getPath()));
                return ((Bundle) client.getFhirContext()
                        .newJsonParser()
                        .parseResource(client.transaction()
                                .withBundle(bundle)
                                .execute())).getEntry().stream()
                                        .filter(e -> e.hasResponse())
                                        .map(e -> e.getResponse())
                                        .filter(r -> r.hasLocation())
                                        .map(r -> r.getLocation())
                                        .filter(l -> l.contains("/Measure/"))
                                        .findFirst()
                                        .orElseThrow();
            } catch (IOException e) {
                throw new IllegalStateException("Could not upload measure resource.", e);
            }
        }

        @Test
        @DisplayName("preferring asynchronous response is met and asynchronous response is handled correctly")
        void preferAndGetAsync() throws Exception {
            dispatcher.clearRecordedRequests();
            dispatcher.clearRecordedResponses();
            var response = storeClient.operation()
                    .onInstance(measureUrl.substring(measureUrl.indexOf("Measure/")))
                    .named("evaluate-measure")
                    .withParameter(Parameters.class, "periodStart", new DateType(MEASURE_REPORT_PERIOD_START))
                    .andParameter("periodEnd", new DateType(MEASURE_REPORT_PERIOD_END))
                    .andParameter("reportType", new StringType(MEASURE_REPORT_TYPE_POPULATION))
                    .useHttpGet()
                    .preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))
                    .withAdditionalHeader(HEADER_PREFER, HEADER_PREFER_RESPOND_ASYNC)
                    .execute();
            var requests = dispatcher.getRecordedRequests();
            var responses = dispatcher.getRecordedResponses();

            assertThat(response.hasParameter()).isTrue();
            assertThat(response.getParameter()).hasSize(1);
            assertThat(response.getParameter().get(0).hasResource()).isTrue();
            assertThat(response.getParameter().get(0).getResource()).isInstanceOf(Bundle.class);
            var bundle = (Bundle) response.getParameter().get(0).getResource();
            assertThat(bundle.hasEntry()).isTrue();
            assertThat(bundle.getEntry()).hasSize(1);
            assertThat(bundle.getEntry().get(0).hasResource()).isTrue();
            assertThat(bundle.getEntry().get(0).getResource()).isInstanceOf(MeasureReport.class);
            assertThat(requests).hasSizeGreaterThanOrEqualTo(2);
            assertThat(responses).hasSizeGreaterThanOrEqualTo(2);
            assertThat(requests.get(0).getHeader(HEADER_PREFER)).isEqualTo(HEADER_PREFER_RESPOND_ASYNC);
            assertThat(responses.get(0).getHeaders().get(CONTENT_LOCATION)).isNotEmpty();
            assertThat(requests.get(1).getRequestUrl().toString())
                    .isEqualTo(responses.get(0).getHeaders().get(CONTENT_LOCATION));
        }

        @Test
        @DisplayName("not preferring asynchronous response results in synchronous response")
        void notPreferAsyncAndGetSync() throws Exception {
            dispatcher.clearRecordedRequests();
            dispatcher.clearRecordedResponses();
            var response = storeClient.operation()
                    .onInstance(measureUrl.substring(measureUrl.indexOf("Measure/")))
                    .named("evaluate-measure")
                    .withParameter(Parameters.class, "periodStart", new DateType(MEASURE_REPORT_PERIOD_START))
                    .andParameter("periodEnd", new DateType(MEASURE_REPORT_PERIOD_END))
                    .andParameter("reportType", new StringType(MEASURE_REPORT_TYPE_POPULATION))
                    .useHttpGet()
                    .preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))
                    .execute();
            var requests = dispatcher.getRecordedRequests();
            var responses = dispatcher.getRecordedResponses();

            assertThat(response.hasParameter()).isTrue();
            assertThat(response.getParameter()).hasSize(1);
            assertThat(response.getParameter().get(0).hasResource()).isTrue();
            assertThat(response.getParameter().get(0).getResource()).isInstanceOf(MeasureReport.class);
            assertThat(requests).hasSize(1);
            assertThat(responses).hasSize(1);
            assertThat(requests.get(0).getHeader(HEADER_PREFER)).isNullOrEmpty();
            assertThat(responses.get(0).getHeaders().get(CONTENT_LOCATION)).isNullOrEmpty();
        }

        @Test
        @DisplayName("preferring asynchronous response is not met and synchronous response is handled correctly")
        void preferAsyncButGetSync() throws Exception {
            var oldBlaze = new GenericContainer<>(DockerImageName.parse("samply/blaze:0.26.2"))
                    .withExposedPorts(8080)
                    .withNetwork(DEFAULT_CONTAINER_NETWORK)
                    .withNetworkAliases("old-fhir-server")
                    .withEnv("LOG_LEVEL", "debug")
                    .withReuse(true);

            try {
                oldBlaze.start();
                var oldBlazeDispatcher = createReverseProxyDispatcher(new OkHttpClient.Builder().build(),
                        HttpUrl.parse(String.format("http://%s:%d/fhir/",
                                oldBlaze.getHost(), oldBlaze.getFirstMappedPort())));
                proxy.setDispatcher(oldBlazeDispatcher);
                var oldBlazeMeasureUrl = uploadMeasure(storeClient);
                oldBlazeDispatcher.clearRecordedRequests();
                oldBlazeDispatcher.clearRecordedResponses();

                var response = storeClient.operation()
                        .onInstance(oldBlazeMeasureUrl.substring(oldBlazeMeasureUrl.indexOf("Measure/")))
                        .named("evaluate-measure")
                        .withParameter(Parameters.class, "periodStart", new DateType(MEASURE_REPORT_PERIOD_START))
                        .andParameter("periodEnd", new DateType(MEASURE_REPORT_PERIOD_END))
                        .andParameter("reportType", new StringType(MEASURE_REPORT_TYPE_POPULATION))
                        .useHttpGet()
                        .preferResponseTypes(List.of(MeasureReport.class, Bundle.class, OperationOutcome.class))
                        .withAdditionalHeader(HEADER_PREFER, HEADER_PREFER_RESPOND_ASYNC)
                        .execute();
                var requests = oldBlazeDispatcher.getRecordedRequests();
                var responses = oldBlazeDispatcher.getRecordedResponses();

                assertThat(response.hasParameter()).isTrue();
                assertThat(response.getParameter()).hasSize(1);
                assertThat(response.getParameter().get(0).hasResource()).isTrue();
                assertThat(response.getParameter().get(0).getResource()).isInstanceOf(MeasureReport.class);
                assertThat(requests).hasSize(1);
                assertThat(responses).hasSize(1);
                assertThat(requests.get(0).getHeader(HEADER_PREFER)).isEqualTo(HEADER_PREFER_RESPOND_ASYNC);
                assertThat(responses.get(0).getHeaders().get(CONTENT_LOCATION)).isNullOrEmpty();
            } finally {
                oldBlaze.stop();
            }
        }
    }
}
