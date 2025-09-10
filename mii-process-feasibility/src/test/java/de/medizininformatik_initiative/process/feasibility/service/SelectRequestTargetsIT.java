package de.medizininformatik_initiative.process.feasibility.service;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import de.medizininformatik_initiative.process.feasibility.client.store.StoreClientFactory;
import de.medizininformatik_initiative.process.feasibility.client.variables.TestConstantsFeasibility;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.ssl.SSLContexts;
import org.assertj.core.api.Condition;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Task;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.DockerHealthcheckWaitStrategy;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
public class SelectRequestTargetsIT {

    private static final String PASSWORD = "password";
    private static final String TEST_RESOURCES_PATH = "de/medizininformatik_initiative/process/feasibility/e2e";
    private static final String KEYSTORE_PATH = TEST_RESOURCES_PATH + "/secrets/hrp_keystore.p12";
    private static final String TRUSTSTORE_PATH = TEST_RESOURCES_PATH + "/secrets/hrp_truststore.p12";

    private static Network network = Network.newNetwork();

    static final GenericContainer<?> dbContainer = new PostgreSQLContainer<>(
            "postgres:" + TestConstantsFeasibility.POSTGRES_VERSION)
                    .withNetwork(network)
                    .withNetworkAliases("db")
                    .withDatabaseName("postgres")
                    .withUsername("liquibase_user")
                    .withPassword(PASSWORD)
                    .withInitScript(TEST_RESOURCES_PATH + "/db/init.sql");

    static final GenericContainer<?> proxyContainer = new GenericContainer<>(
            "nginx:" + TestConstantsFeasibility.NGINX_VERSION)
                    .withNetwork(network)
                    .withNetworkAliases("hrp")
                    .withExposedPorts(443)
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(TEST_RESOURCES_PATH + "/proxy/nginx.conf"),
                            "/etc/nginx/nginx.conf")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(TEST_RESOURCES_PATH + "/proxy/conf.d"),
                            "/etc/nginx/conf.d")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    TEST_RESOURCES_PATH + "/secrets/proxy_certificate_and_int_cas.pem"),
                            "/run/secrets/proxy_certificate_and_int_cas.pem")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    TEST_RESOURCES_PATH + "/secrets/proxy_certificate_private_key.pem"),
                            "/run/secrets/proxy_certificate_private_key.pem")
                    .withCopyFileToContainer(
                            MountableFile.forClasspathResource(
                                    TEST_RESOURCES_PATH + "/secrets/proxy_trusted_client_cas.pem"),
                            "/run/secrets/proxy_trusted_client_cas.pem")
                    .dependsOn(dbContainer);

    static final GenericContainer<?> hrpFhirContainer = new GenericContainer<>(
            "ghcr.io/datasharingframework/fhir:" + TestConstantsFeasibility.DSF_FHIR_VERSION)
                    .withNetwork(network)
                    .withNetworkAliases("hrp-fhir-app")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TEST_RESOURCES_PATH
                            + "/hrp/fhir/conf/bundle.xml"), "/opt/fhir/conf/bundle.xml")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TEST_RESOURCES_PATH + "/hrp/fhir/log"),
                            "/opt/fhir/log")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TEST_RESOURCES_PATH +
                            "/secrets/app_client_trust_certificates.pem"), "/secrets/app_client_trust_certificates.pem")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TEST_RESOURCES_PATH +
                            "/secrets/app_hrp_client_certificate.pem"), "/secrets/app_hrp_client_certificate.pem")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TEST_RESOURCES_PATH +
                            "/secrets/app_hrp_client_certificate_private_key.pem"),
                            "/secrets/app_hrp_client_certificate_private_key.pem")
                    .withEnv("DEV_DSF_SERVER_AUTH_TRUST_CLIENT_CERTIFICATE_CAS",
                            "/secrets/app_client_trust_certificates.pem")
                    .withEnv("DEV_DSF_FHIR_CLIENT_TRUST_SERVER_CERTIFICATE_CAS",
                            "/secrets/app_client_trust_certificates.pem")
                    .withEnv("DEV_DSF_FHIR_CLIENT_CERTIFICATE", "/secrets/app_hrp_client_certificate.pem")
                    .withEnv("DEV_DSF_FHIR_CLIENT_CERTIFICATE_PRIVATE_KEY",
                            "/secrets/app_hrp_client_certificate_private_key.pem")
                    .withEnv("DEV_DSF_FHIR_CLIENT_CERTIFICATE_PRIVATE_KEY_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_FHIR_DB_LIQUIBASE_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_FHIR_DB_USER_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_FHIR_DB_USER_PERMANENT_DELETE_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_FHIR_DB_URL", "jdbc:postgresql://db:5432/hrp_fhir")
                    .withEnv("DEV_DSF_FHIR_DB_USER_GROUP", "hrp_fhir_users")
                    .withEnv("DEV_DSF_FHIR_DB_USER_USERNAME", "hrp_fhir_server_user")
                    .withEnv("DEV_DSF_FHIR_DB_USER_PERMANENT_DELETE_GROUP", "hrp_fhir_permanent_delete_users")
                    .withEnv("DEV_DSF_FHIR_DB_USER_PERMANENT_DELETE_USERNAME", "hrp_fhir_server_permanent_delete_user")
                    .withEnv("DEV_DSF_FHIR_SERVER_BASE_URL", "https://hrp/fhir")
                    .withEnv("DEV_DSF_FHIR_SERVER_ORGANIZATION_IDENTIFIER_VALUE", "Test_HRP")
                    .withEnv("DEV_DSF_FHIR_SERVER_ORGANIZATION_THUMBPRINT",
                            "35b68d6bf4591cb9b2db79b74caeeff6cba0c5ae535213cba28a018e249c613e762fc78dd9d0c866f1dcaf9afcb57c9e744a55d7270d9473e434dc99a92ce549")
                    .waitingFor(new DockerHealthcheckWaitStrategy())
                    .dependsOn(dbContainer, proxyContainer);

    static final GenericContainer<?> hrpBpeContainer = new GenericContainer<>(
            "ghcr.io/datasharingframework/bpe:" + TestConstantsFeasibility.DSF_BPE_VERSION)
                    .withNetwork(network)
                    .withNetworkAliases("bpe-app")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TEST_RESOURCES_PATH + "/hrp/bpe/process"),
                            "/opt/bpe/process")
                    .withCopyFileToContainer(MountableFile.forClasspathResource(TEST_RESOURCES_PATH + "/hrp/bpe/log"),
                            "/opt/bpe/log")
                    .withCopyFileToContainer(MountableFile
                            .forClasspathResource(TEST_RESOURCES_PATH + "/secrets/app_client_trust_certificates.pem"),
                            "/secrets/app_client_trust_certificates.pem")
                    .withCopyFileToContainer(MountableFile
                            .forClasspathResource(TEST_RESOURCES_PATH + "/secrets/app_hrp_client_certificate.pem"),
                            "/secrets/app_hrp_client_certificate.pem")
                    .withCopyFileToContainer(MountableFile
                            .forClasspathResource(
                                    TEST_RESOURCES_PATH + "/secrets/app_hrp_client_certificate_private_key.pem"),
                            "/secrets/app_hrp_client_certificate_private_key.pem")
                    .withEnv("DEV_DSF_SERVER_AUTH_TRUST_CLIENT_CERTIFICATE_CAS",
                            "/secrets/app_client_trust_certificates.pem")
                    .withEnv("DEV_DSF_BPE_DB_LIQUIBASE_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_BPE_DB_USER_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_BPE_DB_USER_CAMUNDA_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_BPE_FHIR_CLIENT_TRUST_SERVER_CERTIFICATE_CAS",
                            "/secrets/app_client_trust_certificates.pem")
                    .withEnv("DEV_DSF_BPE_FHIR_CLIENT_CERTIFICATE", "/secrets/app_hrp_client_certificate.pem")
                    .withEnv("DEV_DSF_BPE_FHIR_CLIENT_CERTIFICATE_PRIVATE_KEY",
                            "/secrets/app_hrp_client_certificate_private_key.pem")
                    .withEnv("DEV_DSF_BPE_FHIR_CLIENT_CERTIFICATE_PRIVATE_KEY_PASSWORD", PASSWORD)
                    .withEnv("DEV_DSF_BPE_DB_URL", "jdbc:postgresql://db/hrp_bpe")
                    .withEnv("DEV_DSF_BPE_DB_USER_GROUP", "hrp_bpe_users")
                    .withEnv("DEV_DSF_BPE_DB_USER_USERNAME", "hrp_bpe_server_user")
                    .withEnv("DEV_DSF_BPE_DB_USER_CAMUNDA_GROUP", "hrp_camunda_users")
                    .withEnv("DEV_DSF_BPE_DB_USER_CAMUNDA_USERNAME", "hrp_camunda_server_user")
                    .withEnv("DEV_DSF_BPE_FHIR_SERVER_BASE_URL", "https://hrp/fhir")
                    .withEnv("DEV_DSF_BPE_PROCESS_EXCLUDED", "medizininformatik-initiativede_feasibilityExecute|0.0")
                    .waitingFor(new DockerHealthcheckWaitStrategy())
                    .dependsOn(dbContainer, proxyContainer, hrpFhirContainer);

    @AfterAll
    public static void tearDown() {
        hrpBpeContainer.stop();
        hrpFhirContainer.stop();
        proxyContainer.stop();
        dbContainer.stop();
        network.close();
    }

    @Test
    @DisplayName("Stale Task is not processed and marked as failed")
    public void staleTaskIsNotProcessed() throws Exception {
        dbContainer.start();
        proxyContainer.start();
        hrpFhirContainer.start();
        hrpBpeContainer.start();
        hrpBpeContainer.stop();
        var baseUrl = "https://%s:%d/hrp/fhir/".formatted(proxyContainer.getHost(), proxyContainer.getMappedPort(443));
        var client = getClient(baseUrl);
        var bundle = (Bundle) client.getFhirContext()
                .newJsonParser()
                .parseResource(
                        Files.readString(getResourceFilePath("fhir/feasibility-request-bundle.json")));
        var result = client.transaction()
                .withBundle(bundle)
                .preferResponseTypes(List.of(Bundle.class, OperationOutcome.class))
                .execute();
        var requestTimeout = "PT5S";
        var config = """
                general:
                  requestTaskTimeout: %s
                """.formatted(requestTimeout);

        assertThat(result.getType()).isEqualTo(Bundle.BundleType.TRANSACTIONRESPONSE);
        assertThat(result.getEntry())
                .hasSize(3)
                .areExactly(1, new Condition<>(entry -> entry.hasResource() && entry.getResource() instanceof Task,
                        "Measure resource"))
                .areExactly(1, new Condition<>(entry -> entry.hasResource() && entry.getResource() instanceof Measure,
                        "Measure resource"))
                .areExactly(1, new Condition<>(entry -> entry.hasResource() && entry.getResource() instanceof Library,
                        "Measure resource"))
                .allMatch(e -> e.getResponse().getStatus().startsWith("201 Created"));
        var taskId = result.getEntry().stream()
                .filter(e -> e.hasResource() && e.getResource() instanceof Task)
                .findFirst()
                .get()
                .getResource()
                .getIdElement()
                .withServerBase(baseUrl, "Task")
                .toVersionless();
        Thread.sleep(5000); // wait before restarting the BPE container to ensure the task is stale
        hrpBpeContainer
                .withEnv("DE_MEDIZININFORMATIK_INITIATIVE_FEASIBILITY_DSF_PROCESS_CONFIGURATION", config)
                .start();
        var taskResponse = client.read().resource(Task.class).withUrl(taskId).execute();
        // wait for the task to be processed
        while (Set.of(Task.TaskStatus.REQUESTED, Task.TaskStatus.INPROGRESS).contains(taskResponse.getStatus())) {
            Thread.sleep(1000);
            taskResponse = client.read().resource(Task.class).withUrl(taskId).execute();
        }

        assertThat(taskResponse.getStatus()).describedAs("task status").isEqualTo(Task.TaskStatus.FAILED);
        assertThat(taskResponse.getOutputFirstRep().getValue().toString()).describedAs("task output")
                .contains(taskId.getIdPart(), "stale", requestTimeout);
    }

    private IGenericClient getClient(String baseUrl) throws NoSuchAlgorithmException, KeyManagementException,
            KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
        var password = PASSWORD.toCharArray();
        var sslContext = SSLContexts.custom()
                .loadKeyMaterial(new File(getResourceFilePath(KEYSTORE_PATH).toString()), password, password)
                .loadTrustMaterial(new File(getResourceFilePath(TRUSTSTORE_PATH).toString()), password,
                        new TrustSelfSignedStrategy())
                .build();
        var client = new StoreClientFactory(FhirContext.forR4(), sslContext).newGenericClient(baseUrl);
        return client;
    }

    private Path getResourceFilePath(String keystorePath) {
        return Paths.get(Thread.currentThread().getContextClassLoader().getResource(keystorePath).getFile());
    }
}
