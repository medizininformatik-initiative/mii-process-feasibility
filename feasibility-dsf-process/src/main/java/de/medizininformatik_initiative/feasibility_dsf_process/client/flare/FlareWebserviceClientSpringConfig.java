package de.medizininformatik_initiative.feasibility_dsf_process.client.flare;

import de.medizininformatik_initiative.feasibility_dsf_process.client.store.TlsClientFactory;
import de.medizininformatik_initiative.feasibility_dsf_process.spring.config.BaseConfig;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HttpContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.net.ssl.SSLContext;

import static ca.uhn.fhir.rest.api.Constants.HEADER_AUTHORIZATION;
import static ca.uhn.fhir.rest.api.Constants.HEADER_AUTHORIZATION_VALPREFIX_BEARER;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.lang.String.format;

@Configuration
@Import(BaseConfig.class)
public class FlareWebserviceClientSpringConfig {

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url:}")
    private String flareBaseUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.timeout.connect:2000}")
    private int connectTimeout;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.host:#{null}}")
    private String proxyHost;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.port:}")
    private Integer proxyPort;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.username:#{null}}")
    private String proxyUsername;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.proxy.password:#{null}}")
    private String proxyPassword;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.username:#{null}}")
    private String basicAuthUsername;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.basic.password:#{null}}")
    private String basicAuthPassword;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.store.auth.bearer.token:#{null}}")
    private String bearerAuthToken;

    @Bean
    public FlareWebserviceClient flareWebserviceClient(HttpClient httpClient) {
        return new FlareWebserviceClient() {

            FlareWebserviceClient client;

            @Override
            public int requestFeasibility(byte[] structuredQuery) throws IOException, InterruptedException {
                return getClient().requestFeasibility(structuredQuery);
            }

            private FlareWebserviceClient getClient() {
                if (client == null) {
                    checkArgument(!isNullOrEmpty(flareBaseUrl), "FLARE_BASE_URL is not set.");
                    try {
                        URI parsedFlareBaseUrl = new URI(flareBaseUrl);
                        client = new FlareWebserviceClientImpl(httpClient, parsedFlareBaseUrl);
                    } catch (URISyntaxException e) {
                        throw new IllegalArgumentException(
                                format("Could not parse FLARE_BASE_URL '%s' as URI.", flareBaseUrl), e);
                    }
                }
                return client;
            }

            @Override
            public void testConnection() throws ClientProtocolException, IOException {
                getClient().testConnection();
            }
        };
    }

    @Bean
    public HttpClient flareHttpClient(@Qualifier("base-client") SSLContext sslContext) {
        HttpClientBuilder builder = new TlsClientFactory(null, sslContext).getNativeHttpClientBuilder();

        BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();

        if (!isNullOrEmpty(proxyHost) && proxyPort != null) {
            HttpHost proxy = new HttpHost(proxyHost, proxyPort);
            builder.setProxy(proxy);
            if (!isNullOrEmpty(proxyUsername) && !isNullOrEmpty(proxyPassword)) {
                builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
                credentialsProvider.setCredentials(new AuthScope(proxy),
                        new UsernamePasswordCredentials(proxyUsername, proxyPassword));
            }
        }
        if (!isNullOrEmpty(basicAuthUsername) && !isNullOrEmpty(basicAuthPassword)) {
            URI flareUri = URI.create(flareBaseUrl);
            credentialsProvider.setCredentials(new AuthScope(new HttpHost(flareUri.getHost(), flareUri.getPort())),
                    new UsernamePasswordCredentials(basicAuthUsername, basicAuthPassword));
        } else if (!isNullOrEmpty(bearerAuthToken)) {
            return new BearerHttpClient(builder.setDefaultCredentialsProvider(credentialsProvider).build());
        }
        return builder.setDefaultCredentialsProvider(credentialsProvider).build();
    }

    private final class BearerHttpClient extends CloseableHttpClient {
        private CloseableHttpClient client;

        public BearerHttpClient(CloseableHttpClient client) {
            this.client = client;
        }

        @Override
        public HttpParams getParams() {
            return client.getParams();
        }

        @Override
        public ClientConnectionManager getConnectionManager() {
            return client.getConnectionManager();
        }

        @Override
        public void close() throws IOException {
            client.close();
        }

        @Override
        protected CloseableHttpResponse doExecute(HttpHost target, HttpRequest request, HttpContext context)
                throws IOException, ClientProtocolException {
            return client.execute(target, request, context);
        }

        @Override
        public <T> T execute(HttpUriRequest request, ResponseHandler<? extends T> responseHandler)
                throws IOException, ClientProtocolException {
            request.setHeader(new BasicHeader(HEADER_AUTHORIZATION,
                    HEADER_AUTHORIZATION_VALPREFIX_BEARER + "1234"));
            return super.execute(request, responseHandler);
        }
    }
}
