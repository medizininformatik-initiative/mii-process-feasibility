package de.medizininformatik_initiative.process.feasibility.spring.config;

import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProvider;
import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProviderImpl;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class EvaluationConfig {

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy:cql}")
    private String evaluationStrategy;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.obfuscate:true}")
    private boolean obfuscateEvaluationResult;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.obfuscation.sensitivity:1.0}")
    private double obfuscationLaplaceSensitivity;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.obfuscation.epsilon:0.5}")
    private double obfuscationLaplaceEpsilon;

    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.rate.limit.count:999}")
    private Integer rateLimitCount;

    @Value("#{T(java.time.Duration).parse('${de.medizininformatik_initiative.feasibility_dsf_process.rate.limit.interval.duration:PT1H}')}")
    private Duration rateLimitTimeIntervalDuration;

    @Bean
    public EvaluationSettingsProvider executionSettingsProvider() {
        return new EvaluationSettingsProviderImpl(
                EvaluationStrategy.fromStrategyRepresentation(evaluationStrategy),
                obfuscateEvaluationResult,
                obfuscationLaplaceSensitivity,
                obfuscationLaplaceEpsilon,
                rateLimitCount,
                rateLimitTimeIntervalDuration
        );
    }
}
