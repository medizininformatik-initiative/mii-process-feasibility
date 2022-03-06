package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.spring.config;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationStrategy;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationSettingsProvider;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationSettingsProviderImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaluationConfig {

    @Value("${de.netzwerk_universitaetsmedizin.codex.processes.feasibility.evaluation.strategy:cql}")
    private String evaluationStrategy;

    @Value("${de.netzwerk_universitaetsmedizin.codex.processes.feasibility.evaluation.obfuscate:true}")
    private boolean obfuscateEvaluationResult;

    @Bean
    public EvaluationSettingsProvider executionSettingsProvider() {
        return new EvaluationSettingsProviderImpl(
                EvaluationStrategy.fromStrategyRepresentation(evaluationStrategy),
                obfuscateEvaluationResult
        );
    }
}
