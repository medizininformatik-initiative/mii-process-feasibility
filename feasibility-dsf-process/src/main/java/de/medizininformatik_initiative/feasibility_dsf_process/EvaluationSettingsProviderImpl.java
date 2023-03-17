package de.medizininformatik_initiative.feasibility_dsf_process;

import java.time.Duration;
import java.util.Objects;

import static java.lang.String.format;
import static org.springframework.util.Assert.isTrue;

/**
 * Can provide several information necessary for running the evaluation pipeline step.
 */
public class EvaluationSettingsProviderImpl implements EvaluationSettingsProvider {

    private final EvaluationStrategy evaluationStrategy;
    private final boolean evaluationResultObfuscationEnabled;
    private final double evaluationResultObfuscationLaplaceSensitivity;
    private final double evaluationResultObfuscationLaplaceEpsilon;
    private final Integer rateLimitCount;
    private final Duration rateLimitTimeIntervalDuration;

    public EvaluationSettingsProviderImpl(EvaluationStrategy evaluationStrategy,
                                          Boolean evaluationResultObfuscationEnabled,
                                          Double evaluationResultObfuscationLaplaceSensitivity,
                                          Double evaluationResultObfuscationLaplaceEpsilon,
                                          Integer rateLimitCount,
                                          Duration rateLimitTimeIntervalDuration) {
        this.evaluationStrategy = Objects.requireNonNull(evaluationStrategy);
        this.evaluationResultObfuscationEnabled = Objects.requireNonNull(evaluationResultObfuscationEnabled);
        this.evaluationResultObfuscationLaplaceSensitivity = Objects
                .requireNonNull(evaluationResultObfuscationLaplaceSensitivity);
        this.evaluationResultObfuscationLaplaceEpsilon = Objects
                .requireNonNull(evaluationResultObfuscationLaplaceEpsilon);
        this.rateLimitCount = Objects.requireNonNull(rateLimitCount);
        this.rateLimitTimeIntervalDuration = Objects.requireNonNull(rateLimitTimeIntervalDuration);

        isTrue(this.rateLimitCount >= 0, format("given request limit '%d' is not >= 0", this.getRateLimitCount()));
        isTrue(this.rateLimitTimeIntervalDuration.compareTo(Duration.ZERO) > 0,
                format("given request limit time interval '%s' is not > 0", this.rateLimitTimeIntervalDuration));
    }

    @Override
    public String evaluationStrategyRepresentation() {
        return evaluationStrategy.getStrategyRepresentation();
    }

    @Override
    public boolean evaluationResultObfuscationEnabled() {
        return evaluationResultObfuscationEnabled;
    }

    @Override
    public double resultObfuscationLaplaceSensitivity() {
        return evaluationResultObfuscationLaplaceSensitivity;
    }

    @Override
    public double resultObfuscationLaplaceEpsilon() {
        return evaluationResultObfuscationLaplaceEpsilon;
    }

    @Override
    public Integer getRateLimitCount() {
        return rateLimitCount;
    }

    @Override
    public Duration getRateLimitTimeIntervalDuration() {
        return rateLimitTimeIntervalDuration;
    }
}
