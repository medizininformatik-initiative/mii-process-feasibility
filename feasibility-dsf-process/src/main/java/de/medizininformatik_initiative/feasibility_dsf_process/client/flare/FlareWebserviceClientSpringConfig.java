package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import de.medizininformatik_initiative.feasibility_dsf_process.client.store.TlsClientFactory;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.BaseConfig;
import org.apache.http.client.HttpClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;

import javax.net.ssl.SSLContext;

@Configuration
@Import(BaseConfig.class)
public class FlareWebserviceClientSpringConfig {

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url:}")
    private String flareBaseUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.timeout.connect:2000}")
    private int connectTimeout;

    @Bean
    public FlareWebserviceClient flareWebserviceClient(HttpClient httpClient) {
        return new FlareWebserviceClientImpl(httpClient, URI.create(flareBaseUrl));
    }

    @Bean
    public HttpClient flareHttpClient(@Qualifier("base-client") SSLContext sslContext) {
        return new TlsClientFactory(null, sslContext)
                .getNativeHttpClientBuilder()
                .build();
    }
}
