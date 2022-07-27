package de.medizininformatik_initiative.feasibility_dsf_process.client.store;

import de.medizininformatik_initiative.feasibility_dsf_process.client.store.StoreClientConfiguration.ProxyConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ProxyConfigurationTest {

    @Test
    public void credentialsRequireProxyHost() {
        assertThrows(RuntimeException.class, () -> ProxyConfiguration.builder()
                .proxyUsername("foo")
                .proxyPassword("bar")
                .build());

        assertDoesNotThrow(() -> ProxyConfiguration.builder()
                .proxyHost("localhost")
                .proxyUsername("foo")
                .proxyPassword("bar")
                .build());
    }
}
