package de.medizininformatik_initiative.process.feasibility.spring.config;

import de.medizininformatik_initiative.process.feasibility.EnhancedFhirWebserviceClientProvider;
import de.medizininformatik_initiative.process.feasibility.EnhancedFhirWebserviceClientProviderImpl;
import dev.dsf.bpe.v1.service.FhirWebserviceClientProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnhancedFhirWebserviceClientProviderConfig {

    private final FhirWebserviceClientProvider fhirClientProvider;

    public EnhancedFhirWebserviceClientProviderConfig(FhirWebserviceClientProvider fhirClientProvider) {
        this.fhirClientProvider = fhirClientProvider;
    }

    @Bean
    public EnhancedFhirWebserviceClientProvider enhancedFhirClientProvider() {
        return new EnhancedFhirWebserviceClientProviderImpl(fhirClientProvider);
    }
}
