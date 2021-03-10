package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.spring.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StoreConfig {

    private final FhirContext fhirContext;

    @Value("${de.netzwerk_universitaetsmedizin.codex.processes.feasibility.store.url:foo}")
    private String storeUrl;

    public StoreConfig(FhirContext fhirContext) {
        this.fhirContext = fhirContext;
    }

    @Bean
    @Qualifier("store")
    public IGenericClient storeClient() {
        return fhirContext.newRestfulGenericClient(storeUrl);
    }
}
