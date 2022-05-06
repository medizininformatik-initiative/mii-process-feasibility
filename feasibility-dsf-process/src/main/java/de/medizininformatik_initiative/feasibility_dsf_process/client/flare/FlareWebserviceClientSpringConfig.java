package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class FlareWebserviceClientSpringConfig {

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.webservice.base_url:}")
    private String flareBaseUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.webservice.connect_timeout:2000}")
    private int connectTimeout;

    @Bean
    public FlareWebserviceClient flareWebserviceClient(HttpClient httpClient) {
        return new FlareWebserviceClientImpl(httpClient, URI.create(flareBaseUrl));
    }

    @Bean
    public HttpClient flareHttpClient() {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeout))
                .build();
    }
}
