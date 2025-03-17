package de.medizininformatik_initiative.process.feasibility.spring.config;

import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProvider;
import de.medizininformatik_initiative.process.feasibility.EvaluationSettingsProviderImpl;
import de.medizininformatik_initiative.process.feasibility.EvaluationStrategy;
import dev.dsf.bpe.v1.documentation.ProcessDocumentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class EvaluationConfig {

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "The query language ('cql' or 'structured-query') in which feasibility queries are sent to the configured FHIR store",
            required = true,
            example = "structured-query")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.strategy:cql}")
    private String evaluationStrategy;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "Whether the feasibility result should be obfuscated or not")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.obfuscate:true}")
    private boolean obfuscateEvaluationResult;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "The sensitivity of the Laplace distribution function used for obfuscation")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.obfuscation.sensitivity:1.0}")
    private double obfuscationLaplaceSensitivity;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "The epsilon of the Laplace distribution function used for obfuscation")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.evaluation.obfuscation.epsilon:0.5}")
    private double obfuscationLaplaceEpsilon;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "The number of requests allowed in the given time interval")
    @Value("${de.medizininformatik_initiative.feasibility_dsf_process.rate.limit.count:999}")
    private Integer rateLimitCount;

    @ProcessDocumentation(
            processNames = { "medizininformatik-initiativede_feasibilityExecute" },
            description = "The time interval in which the rate limit count is applied given in ISO 8601 format")
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
