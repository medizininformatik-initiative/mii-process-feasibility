package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.spring.config;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EnhancedFhirWebserviceClientProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EnhancedFhirWebserviceClientProviderImpl;
import org.highmed.dsf.fhir.client.FhirWebserviceClientProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnhancedFhirWebserviceClientProviderConfig {

    @Autowired
    private FhirWebserviceClientProvider fhirClientProvider;

    @Bean
    @Qualifier("enhancedFhirWebserviceClientProvider")
    public EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider() {
        return new EnhancedFhirWebserviceClientProviderImpl(fhirClientProvider);
    }
}
