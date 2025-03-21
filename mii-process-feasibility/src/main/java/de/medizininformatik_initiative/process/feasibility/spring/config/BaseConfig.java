package de.medizininformatik_initiative.process.feasibility.spring.config;

import dev.dsf.bpe.v1.documentation.ProcessDocumentation;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.Nullable;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.SSLContext;

@Configuration
public class BaseConfig {

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "File path to the trust store with one or more trusted root certificate to validate the server containing the FHIR data or the FLARE service certificate when connecting via https")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_path:#{null}}")
    private String trustStorePath;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Password for the trust store with one or more trusted root certificate to validate the server containing the FHIR data or the FLARE service certificate when connecting via https")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.trust_store_password:#{null}}")
    private String trustStorePassword;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "File path to the key store containing a client certificate if the server containing the FHIR data or the FLARE service requests client certificate authentication")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.key_store_path:#{null}}")
    private String keyStorePath;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Password for the trust store containing a client certificate if the server containing the FHIR data or the FLARE service requests client certificate authentication")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.key_store_password:#{null}}")
    private String keyStorePassword;

    @Bean
    @Qualifier("base-client-trust")
    KeyStore loadTrustStore() throws IOException, KeyStoreException, CertificateException, NoSuchAlgorithmException {
        if (trustStorePath == null || trustStorePath.isBlank()) {
            return DefaultTrustStoreUtils.loadDefaultTrustStore();
        }

        var trustStoreInputStream = new FileInputStream(trustStorePath);

        var trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        trustStore.load(trustStoreInputStream, (trustStorePassword == null) ? null : trustStorePassword.toCharArray());
        trustStoreInputStream.close();

        return trustStore;
    }

    @Bean
    @Qualifier("base-client-key")
    @Nullable
    KeyStore loadKeyStore() throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        if (keyStorePath == null) {
            return null;
        }

        var keyStoreInputStream = new FileInputStream(keyStorePath);

        var keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
        keyStore.load(keyStoreInputStream, (keyStorePassword == null) ? null : keyStorePassword.toCharArray());
        keyStoreInputStream.close();

        return keyStore;
    }

    @Bean
    @Qualifier("base-client")
    SSLContext createSslContext(@Qualifier("base-client-trust") KeyStore trustStore,
                                @Nullable @Qualifier("base-client-key") KeyStore keyStore)
            throws NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        var sslContextBuilder = SSLContexts.custom()
                .loadTrustMaterial(trustStore, null);

        if (keyStore != null) {
            sslContextBuilder.loadKeyMaterial(keyStore, (keyStorePassword == null) ? null :
                    keyStorePassword.toCharArray());
        }

        return sslContextBuilder.build();
    }

}
