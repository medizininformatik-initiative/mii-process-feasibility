package de.medizininformatik_initiative.feasibility_dsf_process.spring.config;

import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProvider;
import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationSettingsProviderImpl;
import de.medizininformatik_initiative.feasibility_dsf_process.EvaluationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaluationConfig {

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy:cql}")
    private String evaluationStrategy;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.obfuscate:true}")
    private boolean obfuscateEvaluationResult;

    @Bean
    public EvaluationSettingsProvider executionSettingsProvider() {
        return new EvaluationSettingsProviderImpl(
                EvaluationStrategy.fromStrategyRepresentation(evaluationStrategy),
                obfuscateEvaluationResult
        );
    }
}
