package de.medizininformatik_initiative.process.feasibility.client.store;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import de.medizininformatik_initiative.process.feasibility.FeasibilitySettings;
import de.medizininformatik_initiative.process.feasibility.FeasibilitySettings.StoreSettings;
import de.medizininformatik_initiative.process.feasibility.spring.config.BaseConfig;
import de.medizininformatik_initiative.process.feasibility.spring.config.DefaultTrustStoreUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CLIENT_TIMEOUT_DEFAULT;

@Configuration
@Import(BaseConfig.class)
public class StoreClientSpringConfig {

    private static final Logger logger = LoggerFactory.getLogger(StoreClientSpringConfig.class);

    @Bean
    @Qualifier("store-client")
    Map<String, IGenericClient> clients(FeasibilitySettings feasibilitySettings) {


        return feasibilitySettings.stores().entrySet().stream()
                .filter(e -> e.getValue().evaluationStrategy() == EvaluationStrategy.CQL)
                .collect(
                        Collectors.toMap(e -> e.getKey(), e -> convertToClient(e.getKey(), e.getValue())));

    }

    private IGenericClient convertToClient(String storeId, StoreSettings store) {
        logger.info("Setting up FHIR client for store '{}' using {}.", storeId, EvaluationStrategy.CQL);
        try {
            var trustedCCertificates = store.trustedCACertificates();
            var clientCert = Optional.ofNullable(store.clientCertificate());
            var privKey = Optional.ofNullable(store.privateKey());
            var privKeyPassword = Optional.ofNullable(store.privateKeyPassword());
            var requestTimeout = Optional.ofNullable(store.requestTimeout());
            var proxy = Optional.ofNullable(store.proxy());
            var sslContext = DefaultTrustStoreUtils.createSslContext(trustedCCertificates, clientCert, privKey,
                    privKeyPassword);
            var fhirContext = FhirContext.forR4();
            var clientFactory = new TlsClientFactory(fhirContext, sslContext);

            clientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
            clientFactory.setSocketTimeout(requestTimeout.orElse(CLIENT_TIMEOUT_DEFAULT));

            if (proxy.isPresent()) {
                var proxySettings = proxy.get();
                if (proxySettings.host() != null) {
                    logger.info("Setting proxy (host: '{}', port: '{}') for store client.", proxySettings.host(),
                            proxySettings.port());
                    clientFactory.setProxy(proxySettings.host(), proxySettings.port());

                    if (proxySettings.username() != null || proxySettings.password() != null) {
                        logger.info(
                                "Setting proxy credentials (username: '{}', password: '***') for store '{}' FHIR client.",
                                proxySettings.username(), storeId);
                        clientFactory.setProxyCredentials(proxySettings.username(), proxySettings.password());
                    }
                }
            }
            fhirContext.setRestfulClientFactory(clientFactory);
            var client = fhirContext.newRestfulGenericClient(store.baseUrl().toString());
            if (store.bearerAuth() != null) {
                logger.info("Setting bearer token '***' for store '{}' FHIR client.", storeId);
                client.registerInterceptor(new BearerTokenAuthInterceptor(store.bearerAuth().token()));
            } else if (store.oAuth() != null) {
                var oAuth = store.oAuth();
                logger.info("Setting OAuth2.0 authentication (issuer url: '{}', client id: '{}', password: '***')"
                        + " for store '{}' FHIR client.", oAuth.issuerUrl(), oAuth.clientId(), storeId);
                if (oAuth.proxy() != null) {
                    logger.info("Setting proxy (host: '{}', port: '{}', username: {},"
                            + " password {}) for OAuth2.0 authentication for store '{}' FHIR client.",
                            oAuth.proxy().host(), oAuth.proxy().port(),
                            Optional.ofNullable(oAuth.proxy().username()).map(u -> "'" + u + "'").orElse("none"),
                            Optional.ofNullable(oAuth.proxy().password()).map(p -> "'***'").orElse("none"),
                            storeId);
                    client.registerInterceptor(new OAuthInterceptor(oAuth.clientId(), oAuth.clientPassword(),
                            oAuth.issuerUrl().toString(), DefaultTrustStoreUtils.loadTrustStore(trustedCCertificates),
                            Optional.ofNullable(oAuth.proxy().host()), Optional.ofNullable(oAuth.proxy().port()),
                            Optional.ofNullable(oAuth.proxy().username()),
                            Optional.ofNullable(oAuth.proxy().password())));
                }
            } else if (store.basicAuth() != null) {
                logger.info(
                        "Setting basic authentication (username: '{}', password: '***') for store '{}' FHIR client.",
                        store.basicAuth().username(), storeId);
                client.registerInterceptor(
                        new BasicAuthInterceptor(store.basicAuth().username(), store.basicAuth().password()));
            }
            return client;
        } catch (Exception e) {
            throw new IllegalArgumentException("Error creating FHIR client for store '%s'.".formatted(storeId), e);
        }
    }
}
