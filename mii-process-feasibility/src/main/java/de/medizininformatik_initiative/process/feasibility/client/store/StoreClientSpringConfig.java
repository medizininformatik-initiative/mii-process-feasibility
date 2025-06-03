package de.medizininformatik_initiative.process.feasibility.client.store;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import de.medizininformatik_initiative.process.feasibility.spring.config.BaseConfig;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.security.KeyStore;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CLIENT_TIMEOUT_DEFAULT;
import static java.util.Objects.nonNull;

@Configuration
@Import(BaseConfig.class)
public class StoreClientSpringConfig {

    private static final Logger logger = LoggerFactory.getLogger(StoreClientSpringConfig.class);

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Proxy host, set if the server containing the FHIR data or the Flare service can only be reached through a proxy",
            example = "proxy.example.com")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.host:#{null}}")
    private String proxyHost;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Proxy port, set if the server containing the FHIR data or the Flare service can only be reached through a proxy",
            example = "8080")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.port:}")
    private Integer proxyPort;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Proxy username, set if the server containing the FHIR data or the Flare service can only be reached through a proxy which requests authentication")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.username:#{null}}")
    private String proxyUsername;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Proxy password, set if the server containing the FHIR data or the Flare service can only be reached through a proxy which requests authentication")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.password:#{null}}")
    private String proxyPassword;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Bearer token for authentication, set if the server containing the FHIR data or the Flare service requests authentication using a bearer token")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.bearer.token:#{null}}")
    private String bearerAuthToken;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Basic authentication username, set if the server containing the FHIR data or the Flare service requests authentication using basic auth")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username:#{null}}")
    private String basicAuthUsername;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Basic authentication password, set if the server containing the FHIR data or the Flare service requests authentication using basic auth")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password:#{null}}")
    private String basicAuthPassword;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Timeout in milliseconds for the store client to establish a connection to the server containing the FHIR data")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.timeout.connect:2000}")
    private Integer connectTimeout;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Timeout in milliseconds for requesting a connection from the connection pool for the store client")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.timeout.connect_request:20000}")
    private Integer connectRequestTimeout;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Timeout in milliseconds the store client waits for a response from the server containing the FHIR data")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.timeout.socket:" + CLIENT_TIMEOUT_DEFAULT + "}")
    private Integer socketTimeout;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "The base address of the server containing the FHIR data",
            required = true,
            example = "http://foo.bar/fhir")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url:}")
    private String storeBaseUrl;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "OAuth2.0 client secret, set if the server containing the FHIR data requests authentication using OAuth2.0")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.client.password:#{null}}")
    private String oauthClientSecret;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "OAuth2.0 client id, set if the server containing the FHIR data requests authentication using OAuth2.0")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.client.id:#{null}}")
    private String oauthClientId;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "OAuth2.0 issuer url, set if the server containing the FHIR data requests authentication using OAuth2.0",
            example = "https://auth.example.org/realms/dic-data")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.issuer.url:#{null}}")
    private String oauthIssuerUrl;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "OAuth2.0 proxy host, set if the server containing the FHIR data requests authentication using OAuth2.0 which requires a proxy",
            example = "proxy.example.org")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.proxy.host:#{null}}")
    private String oauthProxyHost;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "OAuth2.0 proxy port, set if the server containing the FHIR data requests authentication using OAuth2.0 which requires a proxy",
            example = "8080")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.proxy.port:}")
    private Integer oauthProxyPort;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "OAuth2.0 proxy username, set if the server containing the FHIR data requests authentication using OAuth2.0 which requires a proxy with basic authentication")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.proxy.username:#{null}}")
    private String oauthProxyUsername;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "OAuth2.0 proxy password, set if the server containing the FHIR data requests authentication using OAuth2.0 which requires a proxy with basic authentication")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.proxy.password:#{null}}")
    private String oauthProxyPassword;

    @Bean
    @Qualifier("store-client")
    IGenericClient client(@Qualifier("store-client") FhirContext fhirContext,
                          @Qualifier("store-client") RestfulClientFactory clientFactory,
                          @Qualifier("base-client-trust") KeyStore trustStore) {
        logger.info("Setting up store client for direct access using {}.",
                EvaluationStrategy.CQL);

        clientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        clientFactory.setConnectTimeout(connectTimeout);
        clientFactory.setConnectionRequestTimeout(connectRequestTimeout);
        clientFactory.setSocketTimeout(socketTimeout);

        if (proxyHost != null) {
            logger.info("Setting proxy (host: '{}', port: '{}') for store client.", proxyHost, proxyPort);
            clientFactory.setProxy(proxyHost, proxyPort);

            if (proxyUsername != null || proxyPassword != null) {
                logger.info("Setting proxy credentials (username: '{}', password: '***') for store client.",
                        proxyUsername);
                clientFactory.setProxyCredentials(proxyUsername, proxyPassword);
            }
        }
        var client = fhirContext.newRestfulGenericClient(storeBaseUrl);
        if (bearerAuthToken != null) {
            logger.info("Setting bearer token '***' for store client.");
            client.registerInterceptor(new BearerTokenAuthInterceptor(bearerAuthToken));
        } else if (!isNullOrEmpty(oauthClientId) && !isNullOrEmpty(oauthClientSecret)
                && !isNullOrEmpty(oauthIssuerUrl)) {
            logger.info("Setting OAuth2.0 authentication (issuer url: '{}', client id: '{}', password: '***')"
                    + " for store client.", oauthIssuerUrl, oauthClientId);
            if (nonNull(oauthProxyHost) && nonNull(oauthProxyPort)) {
                logger.info("Setting proxy (host: '{}', port: '{}', username: {},"
                        + " password {}) for OAuth2.0 authentication.", oauthProxyHost, oauthProxyPassword,
                        Optional.ofNullable(oauthProxyUsername).map(u -> "'" + u + "'").orElse("none"),
                        Optional.ofNullable(oauthProxyPassword).map(p -> "'***'").orElse("none"));
            }
            client.registerInterceptor(new OAuthInterceptor(oauthClientId, oauthClientSecret, oauthIssuerUrl,
                    trustStore, Optional.ofNullable(oauthProxyHost), Optional.ofNullable(oauthProxyPort),
                    Optional.ofNullable(oauthProxyUsername), Optional.ofNullable(oauthProxyPassword)));
        }

        if (basicAuthUsername != null || basicAuthPassword != null) {
            logger.info("Setting basic authentication (username: '{}', password: '***') for store client.",
                    basicAuthUsername);
            client.registerInterceptor(new BasicAuthInterceptor(basicAuthUsername, basicAuthPassword));
        }

        return client;
    }

    @Bean
    @Qualifier("store-client")
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }

    @Bean
    @Qualifier("store-client")
    RestfulClientFactory clientFactory(@Qualifier("store-client") FhirContext fhirContext,
                                       @Qualifier("base-client") SSLContext sslContext) {
        return new StoreClientFactory(fhirContext, sslContext);
    }
}
