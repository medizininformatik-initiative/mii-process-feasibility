package de.medizininformatik_initiative.process.feasibility.client.store;

import de.medizininformatik_initiative.process.feasibility.client.store.StoreClientConfiguration.StoreAuthenticationConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class StoreAuthenticationConfigurationTest {

    @Test
    public void onlyOneAuthenticationMethodIsAllowed() {
        assertThrows(RuntimeException.class, () -> StoreAuthenticationConfiguration.builder()
                .bearerToken("something")
                .basicAuthUsername("foo")
                .basicAuthPassword("bar")
                .build());

        assertDoesNotThrow(() -> StoreAuthenticationConfiguration.builder()
                .bearerToken("something")
                .build());

        assertDoesNotThrow(() -> StoreAuthenticationConfiguration.builder()
                .basicAuthUsername("foo")
                .basicAuthPassword("bar")
                .build());
    }
}
