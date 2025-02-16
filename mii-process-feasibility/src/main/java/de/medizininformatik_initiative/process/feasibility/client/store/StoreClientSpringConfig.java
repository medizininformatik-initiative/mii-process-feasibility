package de.medizininformatik_initiative.process.feasibility.client.store;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import de.medizininformatik_initiative.process.feasibility.spring.config.BaseConfig;
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

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.port:}")
    private Integer proxyPort;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.username:#{null}}")
    private String proxyUsername;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.password:#{null}}")
    private String proxyPassword;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.bearer.token:#{null}}")
    private String bearerAuthToken;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username:#{null}}")
    private String basicAuthUsername;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password:#{null}}")
    private String basicAuthPassword;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.timeout.connect:2000}")
    private Integer connectTimeout;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.timeout.connect_request:20000}")
    private Integer connectRequestTimeout;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.timeout.socket:" + CLIENT_TIMEOUT_DEFAULT + "}")
    private Integer socketTimeout;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url:}")
    private String storeBaseUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.client.password:#{null}}")
    private String oauthClientSecret;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.client.id:#{null}}")
    private String oauthClientId;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.issuer.url:#{null}}")
    private String oauthIssuerUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.proxy.host:#{null}}")
    private String oauthProxyHost;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.proxy.port:}")
    private Integer oauthProxyPort;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.oauth.proxy.username:#{null}}")
    private String oauthProxyUsername;

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
        fhirContext.setRestfulClientFactory(clientFactory);
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
