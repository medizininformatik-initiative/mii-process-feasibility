package de.medizininformatik_initiative.feasibility_dsf_process.spring.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BaseConfig {

    @Bean
    @Qualifier("base")
    FhirContext fhirContext() {
        return FhirContext.forR4();
    }

}
