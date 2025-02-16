package de.medizininformatik_initiative.process.feasibility.client.flare;

import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProvider;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import de.medizininformatik_initiative.process.feasibility.client.store.StoreClientFactory;
import de.medizininformatik_initiative.process.feasibility.spring.config.BaseConfig;
import de.medizininformatik_initiative.process.feasibility.spring.config.EvaluationConfig;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import static com.google.common.base.Strings.isNullOrEmpty;
import static de.medizininformatik_initiative.process.feasibility.variables.ConstantsFeasibility.CLIENT_TIMEOUT_DEFAULT;
import static java.lang.String.format;

@Configuration
@Import({ BaseConfig.class, EvaluationConfig.class })
public class FlareWebserviceClientSpringConfig {

    private static final Logger logger = LoggerFactory.getLogger(FlareWebserviceClientSpringConfig.class);

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.base_url:}")
    private String flareBaseUrl;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.client.flare.timeout.connect:" + CLIENT_TIMEOUT_DEFAULT + "}")
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
    public FlareWebserviceClient flareWebserviceClient(HttpClient httpClient,
                                                       EvaluationSettingsProvider evaluationSettingsProvider) {
        if (EvaluationStrategy.STRUCTURED_QUERY == evaluationSettingsProvider.evaluationStrategy()) {
            return createFlareClient(httpClient);
        } else {
            return new ErrorFlareWebserviceClient(new IllegalStateException(
                    format("EVALUATION_STRATEGY is not set to '%s'.", EvaluationStrategy.STRUCTURED_QUERY)));
        }
    }

    private FlareWebserviceClient createFlareClient(HttpClient httpClient) {
        if (isNullOrEmpty(flareBaseUrl)) {
            return new ErrorFlareWebserviceClient(new IllegalArgumentException("FLARE_BASE_URL is not set."));
        } else {
            try {
                var parsedFlareBaseUrl = new URI(flareBaseUrl);
                return new FlareWebserviceClientImpl(httpClient, parsedFlareBaseUrl);
            } catch (URISyntaxException e) {
                return new ErrorFlareWebserviceClient(new IllegalArgumentException(
                        format("Could not parse FLARE_BASE_URL '%s' as URI.", flareBaseUrl), e));
            }
        }
    }

    @Bean
    public HttpClient flareHttpClient(@Qualifier("base-client") SSLContext sslContext,
                                      EvaluationSettingsProvider evaluationSettingsProvider) {
        if (EvaluationStrategy.STRUCTURED_QUERY == evaluationSettingsProvider.evaluationStrategy()) {
            logger.info("Setting up store client for indirect access over flare using {}.",
                    EvaluationStrategy.STRUCTURED_QUERY);

            var clientFactory = new StoreClientFactory(null, sslContext);
            clientFactory.setConnectTimeout(connectTimeout);
            clientFactory.setConnectionRequestTimeout(connectTimeout);
            clientFactory.setSocketTimeout(connectTimeout);
            var builder = clientFactory.getNativeHttpClientBuilder();

            var credentialsProvider = new BasicCredentialsProvider();

            if (!isNullOrEmpty(proxyHost) && proxyPort != null) {
                var proxy = new HttpHost(proxyHost, proxyPort);
                builder.setProxy(proxy);
                logger.info("Setting proxy (host: '{}', port: '{}') for store client.", proxyHost, proxyPort);
                if (!isNullOrEmpty(proxyUsername) && !isNullOrEmpty(proxyPassword)) {
                    logger.info("Setting proxy credentials (username: '{}', password: '***') for store client.",
                            proxyUsername);
                    builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
                    credentialsProvider.setCredentials(new AuthScope(proxy),
                            new UsernamePasswordCredentials(proxyUsername, proxyPassword));
                }
            }
            if (!isNullOrEmpty(basicAuthUsername) && !isNullOrEmpty(basicAuthPassword)) {
                logger.info("Setting basic authentication (username: '{}', password: '***') for store client.",
                        basicAuthUsername);
                var flareUri = URI.create(flareBaseUrl);
                credentialsProvider.setCredentials(new AuthScope(new HttpHost(flareUri.getHost(), flareUri.getPort())),
                        new UsernamePasswordCredentials(basicAuthUsername, basicAuthPassword));
            } else if (!isNullOrEmpty(bearerAuthToken)) {
                logger.info("Setting bearer token '***' for store client.");
                return new BearerHttpClient(builder.setDefaultCredentialsProvider(credentialsProvider).build());
            }
            return builder.setDefaultCredentialsProvider(credentialsProvider).build();
        } else {
            return HttpClientBuilder.create().build();
        }
    }

    private final class ErrorFlareWebserviceClient implements FlareWebserviceClient {
        private final RuntimeException exception;

        private ErrorFlareWebserviceClient(RuntimeException exception) {
            this.exception = exception;
        }

        @Override
        public void testConnection() throws IOException {
            throw exception;
        }

        @Override
        public int requestFeasibility(byte[] structuredQuery) throws IOException, InterruptedException {
            throw exception;
        }
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
