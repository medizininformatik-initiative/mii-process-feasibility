package de.medizininformatik_initiative.process.feasibility.client.store;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import ca.uhn.fhir.rest.client.api.ServerValidationModeEnum;
import ca.uhn.fhir.rest.client.impl.RestfulClientFactory;
import ca.uhn.fhir.rest.client.interceptor.BasicAuthInterceptor;
import ca.uhn.fhir.rest.client.interceptor.BearerTokenAuthInterceptor;
import de.medizininformatik_initiative.process.feasibility.client.store.StoreClientConfiguration.ConnectionConfiguration;
import de.medizininformatik_initiative.process.feasibility.client.store.StoreClientConfiguration.ProxyConfiguration;
import de.medizininformatik_initiative.process.feasibility.client.store.StoreClientConfiguration.StoreAuthenticationConfiguration;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import javax.net.ssl.SSLContext;

// TODO: doc
@RequiredArgsConstructor
class StoreClientFactory {

    @NonNull
    private FhirContext fhirContext;

    IGenericClient createClient(String storeBaseUrl, StoreClientConfiguration clientCfg) {
        var baseFactory = createBaseFactory(clientCfg.getSslContext());
        baseFactory.setServerValidationMode(ServerValidationModeEnum.NEVER);
        configureConnectionSettings(baseFactory, clientCfg.getConnectionConfiguration());

        var proxyCfg = clientCfg.getProxyConfiguration();
        if (proxyCfg != null) {
            configureProxySettings(baseFactory, proxyCfg);
        }

        fhirContext.setRestfulClientFactory(baseFactory);
        var client = fhirContext.newRestfulGenericClient(storeBaseUrl);

        var authCfg = clientCfg.getStoreAuthenticationConfiguration();
        if (authCfg != null) {
            configureAuthentication(client, authCfg);
        }
        return client;
    }


    private void configureConnectionSettings(RestfulClientFactory baseFactory, ConnectionConfiguration connCfg) {
        baseFactory.setConnectTimeout(connCfg.getConnectionTimeoutMs());
        baseFactory.setConnectionRequestTimeout(connCfg.getConnectionRequestTimeoutMs());
        baseFactory.setSocketTimeout(connCfg.getSocketTimeoutMs());
    }

    private void configureProxySettings(RestfulClientFactory baseFactory, ProxyConfiguration proxyCfg) {
        if (proxyCfg.getProxyHost() != null) {
            baseFactory.setProxy(proxyCfg.getProxyHost(), proxyCfg.getProxyPort());

            if (proxyCfg.getProxyUsername() != null || proxyCfg.getProxyPassword() != null) {
                baseFactory.setProxyCredentials(proxyCfg.getProxyUsername(), proxyCfg.getProxyPassword());
            }
        }
    }

    private void configureAuthentication(IGenericClient client, StoreAuthenticationConfiguration authCfg) {
        if (authCfg.getBearerToken() != null) {
            client.registerInterceptor(new BearerTokenAuthInterceptor(authCfg.getBearerToken()));
        }
        if (authCfg.getBasicAuthUsername() != null || authCfg.getBasicAuthPassword() != null) {
            client.registerInterceptor(new BasicAuthInterceptor(authCfg.getBasicAuthUsername(),
                    authCfg.getBasicAuthPassword()));
        }
    }

    private RestfulClientFactory createBaseFactory(SSLContext sslContext) {
        return new TlsClientFactory(fhirContext, sslContext);
    }
}
