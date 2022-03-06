package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.spring.config;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.FlareWebserviceClient;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.FlareWebserviceClientImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class FlareWebserviceClientConfig {

    @Value("${de.netzwerk_universitaetsmedizin.codex.processes.feasibility.flare.webservice.baseUrl:}")
    private String flareBaseUrl;

    @Value("${de.netzwerk_universitaetsmedizin.codex.processes.feasibility.flare.webservice.connectTimeout:2000}")
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
