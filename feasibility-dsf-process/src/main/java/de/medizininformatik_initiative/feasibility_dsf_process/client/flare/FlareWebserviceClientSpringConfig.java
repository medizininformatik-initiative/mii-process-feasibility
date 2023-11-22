package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import de.medizininformatik_initiative.feasibility_dsf_process.client.store.TlsClientFactory;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.BaseConfig;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.net.URI;

import javax.net.ssl.SSLContext;

import static com.google.common.base.Strings.isNullOrEmpty;

@Configuration
@Import(BaseConfig.class)
public class FlareWebserviceClientSpringConfig {

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url:}")
    private String flareBaseUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.timeout.connect:2000}")
    private int connectTimeout;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username:#{null}}")
    private String basicAuthUsername;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password:#{null}}")
    private String basicAuthPassword;

    @Bean
    public FlareWebserviceClient flareWebserviceClient(HttpClient httpClient) {
        return new FlareWebserviceClientImpl(httpClient, URI.create(flareBaseUrl));
    }

    @Bean
    public HttpClient flareHttpClient(@Qualifier("base-client") SSLContext sslContext) {
        HttpClientBuilder builder = new TlsClientFactory(null, sslContext).getNativeHttpClientBuilder();

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (!isNullOrEmpty(basicAuthUsername) && !isNullOrEmpty(basicAuthPassword)) {
            URI flareUri = URI.create(flareBaseUrl);
            credentialsProvider.setCredentials(new AuthScope(new HttpHost(flareUri.getHost(), flareUri.getPort())),
                    new UsernamePasswordCredentials(basicAuthUsername, basicAuthPassword));
        }
        return builder.setDefaultCredentialsProvider(credentialsProvider).build();
    }
}
