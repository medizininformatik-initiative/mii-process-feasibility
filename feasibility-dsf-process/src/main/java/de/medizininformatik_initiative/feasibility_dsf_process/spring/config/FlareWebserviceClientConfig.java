package de.medizininformatik_initiative.feasibility_dsf_process.spring.config;

import de.medizininformatik_initiative.feasibility_dsf_process.FlareWebserviceClient;
import de.medizininformatik_initiative.feasibility_dsf_process.FlareWebserviceClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class FlareWebserviceClientConfig {

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.flare.webservice.baseUrl:}")
    private String flareBaseUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.flare.webservice.connectTimeout:2000}")
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
