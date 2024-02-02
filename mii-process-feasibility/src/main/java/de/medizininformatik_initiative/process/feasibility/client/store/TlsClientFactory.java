package de.medizininformatik_initiative.process.feasibility.client.store;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.EncodingEnum;
import ca.uhn.fhir.rest.api.RequestTypeEnum;
import ca.uhn.fhir.rest.client.apache.ApacheHttpClient;
import ca.uhn.fhir.rest.client.api.Header;
import ca.uhn.fhir.rest.client.api.IHttpClient;
import ca.uhn.fhir.rest.client.api.IHttpRequest;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.ProxyAuthenticationStrategy;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.hl7.fhir.instance.model.api.IBaseBinary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

// TODO: doc
@Slf4j
@RequiredArgsConstructor
public class TlsClientFactory extends RestfulClientFactory {
    private HttpClient myHttpClient;

    private HttpHost myProxy;

    private final Map<String, ApacheHttpClient> clientByServerBase = new HashMap<>();

    private SSLContext sslContext;

    public TlsClientFactory(FhirContext fhirContext, SSLContext sslContext) {
        super(fhirContext);
        this.sslContext = Objects.requireNonNull(sslContext, "SSL Context must not be null.");
    }

    @Override
    protected synchronized IHttpClient getHttpClient(String serverBase) {
        return new IHttpClient() {

            @Override
            public IHttpRequest createParamRequest(FhirContext theContext, Map<String, List<String>> theParams,
                                                   EncodingEnum theEncoding) {
                return getClient().createParamRequest(theContext, theParams, theEncoding);
            }

            @Override
            public IHttpRequest createGetRequest(FhirContext theContext, EncodingEnum theEncoding) {
                return getClient().createGetRequest(theContext, theEncoding);
            }

            @Override
            public IHttpRequest createByteRequest(FhirContext theContext, String theContents, String theContentType,
                                                  EncodingEnum theEncoding) {
                return getClient().createByteRequest(theContext, theContents, theContentType, theEncoding);
            }

            @Override
            public IHttpRequest createBinaryRequest(FhirContext theContext, IBaseBinary theBinary) {
                return getClient().createBinaryRequest(theContext, theBinary);
            }

            private ApacheHttpClient getClient() {
                if (serverBase == null || serverBase.isBlank()) {
                    throw new IllegalArgumentException("Store url is not set.");
                }
                if (clientByServerBase.containsKey(serverBase)) {
                    log.debug("Reusing ApacheHttpClient for ServerBase {}", serverBase);
                    return clientByServerBase.get(serverBase);
                } else {
                    log.debug("Returning new ApacheHttpClient for ServerBase {}", serverBase);
                    var client = new ApacheHttpClient(getNativeHttpClient(), new StringBuilder(serverBase),
                            null, null, null, null);
                    clientByServerBase.put(serverBase, client);
                    return client;
                }
            }
        };
    }

    @Override
    public synchronized IHttpClient getHttpClient(StringBuilder theUrl, Map<String, List<String>> theIfNoneExistParams,
                                                  String theIfNoneExistString, RequestTypeEnum theRequestType,
                                                  List<Header> theHeaders) {
        return new ApacheHttpClient(getNativeHttpClient(), theUrl, theIfNoneExistParams, theIfNoneExistString,
                theRequestType, theHeaders);
    }

    public HttpClient getNativeHttpClient() {
        if (myHttpClient == null) {
            myHttpClient = getNativeHttpClientBuilder().build();
        }

        return myHttpClient;
    }

    public HttpClientBuilder getNativeHttpClientBuilder() {
        var sslContext = getSslContext();

        var socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", new SSLConnectionSocketFactory(sslContext)).build();

        var connectionManager = new PoolingHttpClientConnectionManager(
                socketFactoryRegistry, null, null, null, 5000,
                TimeUnit.MILLISECONDS);

        connectionManager.setMaxTotal(getPoolMaxTotal());
        connectionManager.setDefaultMaxPerRoute(getPoolMaxPerRoute());

        var defaultRequestConfig = RequestConfig.custom().setSocketTimeout(getSocketTimeout())
                .setConnectTimeout(getConnectTimeout()).setConnectionRequestTimeout(getConnectionRequestTimeout())
                .setProxy(myProxy).build();

        var builder = HttpClients.custom().setConnectionManager(connectionManager)
                .setSSLContext(sslContext).setDefaultRequestConfig(defaultRequestConfig).disableCookieManagement();

        if (myProxy != null && StringUtils.isNotBlank(getProxyUsername())
                && StringUtils.isNotBlank(getProxyPassword())) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(new AuthScope(myProxy.getHostName(), myProxy.getPort()),
                    new UsernamePasswordCredentials(getProxyUsername(), getProxyPassword()));
            builder.setProxyAuthenticationStrategy(new ProxyAuthenticationStrategy());
            builder.setDefaultCredentialsProvider(credsProvider);
        }
        return builder;
    }

    protected SSLContext getSslContext() {
        return sslContext;
    }

    @Override
    protected void resetHttpClient() {
        this.myHttpClient = null;
    }

    /**
     * Only allows to set an instance of type org.apache.http.client.HttpClient
     *
     * @see ca.uhn.fhir.rest.client.api.IRestfulClientFactory#setHttpClient(Object)
     */
    @Override
    public synchronized void setHttpClient(Object theHttpClient) {
        this.myHttpClient = (HttpClient) theHttpClient;
    }

    @Override
    public void setProxy(String theHost, Integer thePort) {
        if (theHost != null) {
            myProxy = new HttpHost(theHost, thePort, "http");
        } else {
            myProxy = null;
        }
    }
}
