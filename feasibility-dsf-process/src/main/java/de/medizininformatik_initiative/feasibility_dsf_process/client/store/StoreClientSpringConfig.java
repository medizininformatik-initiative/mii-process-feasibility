package de.medizininformatik_initiative.feasibility_dsf_process.client.store;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.BaseConfig;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.net.ssl.SSLContext;

@Configuration
@Import(BaseConfig.class)
public class StoreClientSpringConfig {
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

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.timeout.socket:20000}")
    private Integer socketTimeout;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.base_url}")
    private String storeBaseUrl;

    @Bean
    @Qualifier("store-client")
    IGenericClient client(@Qualifier("store-client") FhirContext fhirContext,
                          @Qualifier("store-client") RestfulClientFactory clientFactory) {
        if (storeBaseUrl == null || storeBaseUrl.isBlank()) {
            throw new IllegalArgumentException("Store url is not set.");
        }

        clientFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        clientFactory.setConnectTimeout(connectTimeout);
        clientFactory.setConnectionRequestTimeout(connectRequestTimeout);
        clientFactory.setSocketTimeout(socketTimeout);

        if (proxyHost != null) {
            clientFactory.setProxy(proxyHost, proxyPort);

            if (proxyUsername != null || proxyPassword != null) {
                clientFactory.setProxyCredentials(proxyUsername, proxyPassword);
            }
        }

        fhirContext.setRestfulClientFactory(clientFactory);
        var client = fhirContext.newRestfulGenericClient(storeBaseUrl);
        if (bearerAuthToken != null) {
            client.registerInterceptor(new BearerTokenAuthInterceptor(bearerAuthToken));
        }
        if (basicAuthUsername != null || basicAuthPassword != null) {
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
        return new TlsClientFactory(fhirContext, sslContext);
    }
}
