package de.netzwerk_universitaetsmedizin.codex.processes.feasibility.spring.config;

import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationStrategy;
import de.netzwerk_universitaetsmedizin.codex.processes.feasibility.EvaluationStrategyProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EvaluationConfig {

    @Value("${de.netzwerk_universitaetsmedizin.codex.processes.feasibility.evaluation.strategy:cql}")
    private String evaluationStrategy;

    @Bean
    public EvaluationStrategyProvider evaluationStrategy() {
        return EvaluationStrategy.fromStrategyRepresentation(evaluationStrategy);
    }
}
