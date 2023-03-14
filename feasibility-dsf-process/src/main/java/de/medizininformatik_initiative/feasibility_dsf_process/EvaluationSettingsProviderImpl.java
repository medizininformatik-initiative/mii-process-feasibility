package de.medizininformatik_initiative.feasibility_dsf_process;

import java.util.Objects;

/**
 * Can provide several information necessary for running the evaluation pipeline step.
 */
public class EvaluationSettingsProviderImpl implements EvaluationSettingsProvider {

    private final EvaluationStrategy evaluationStrategy;
    private final boolean evaluationResultObfuscationEnabled;
    private final double evaluationResultObfuscationLaplaceSensitivity;
    private final double evaluationResultObfuscationLaplaceEpsilon;

    public EvaluationSettingsProviderImpl(EvaluationStrategy evaluationStrategy,
                                          Boolean evaluationResultObfuscationEnabled,
                                          Double evaluationResultObfuscationLaplaceSensitivity,
                                          Double evaluationResultObfuscationLaplaceEpsilon) {
        this.evaluationResultObfuscationLaplaceSensitivity = Objects
                .requireNonNull(evaluationResultObfuscationLaplaceSensitivity);
        this.evaluationResultObfuscationLaplaceEpsilon = Objects
                .requireNonNull(evaluationResultObfuscationLaplaceEpsilon);
        this.evaluationStrategy = Objects.requireNonNull(evaluationStrategy);
        this.evaluationResultObfuscationEnabled = Objects.requireNonNull(evaluationResultObfuscationEnabled);
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
}
