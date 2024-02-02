package de.medizininformatik_initiative.process.feasibility.client.store;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

import javax.net.ssl.SSLContext;

// TODO: doc
@Builder
@Getter
public class StoreClientConfiguration {

    /**
     * This trust store is solely used for server communication, i.e. for verifying trust chains.
     */
    @NonNull
    private SSLContext sslContext;
    @NonNull
    private ConnectionConfiguration connectionConfiguration;
    private ProxyConfiguration proxyConfiguration;
    private StoreAuthenticationConfiguration storeAuthenticationConfiguration;

    @Builder
    @Getter
    static class ProxyConfiguration {
        private String proxyHost;
        private int proxyPort;
        private String proxyUsername;
        private String proxyPassword;

        public static ProxyConfigurationBuilder builder() {
            return new ValidatedProxyConfigurationBuilder();
        }

        private static class ValidatedProxyConfigurationBuilder extends ProxyConfigurationBuilder {
            @Override
            public ProxyConfiguration build() {
                if ((super.proxyUsername != null || super.proxyPassword != null) && super.proxyHost == null) {
                    throw new RuntimeException("Proxy credentials require a configured proxy.");
                }

                return super.build();
            }
        }
    }

    @Builder
    @Getter
    static class ConnectionConfiguration {
        @NonNull
        private int connectionTimeoutMs;
        @NonNull
        private int connectionRequestTimeoutMs;
        @NonNull
        private int socketTimeoutMs;
    }

    @Builder
    @Getter
    static class StoreAuthenticationConfiguration {
        private String bearerToken;
        private String basicAuthUsername;
        private String basicAuthPassword;

        public static StoreAuthenticationConfigurationBuilder builder() {
            return new ValidatedStoreAuthenticationConfigurationBuilder();
        }

        private static class ValidatedStoreAuthenticationConfigurationBuilder
                extends StoreAuthenticationConfigurationBuilder {
            @Override
            public StoreAuthenticationConfiguration build() {
                if (super.bearerToken != null && (super.basicAuthUsername != null || super.basicAuthPassword != null)) {
                    throw new RuntimeException("Only one authentication method is allowed.");
                }

                return super.build();
            }
        }
    }
}
