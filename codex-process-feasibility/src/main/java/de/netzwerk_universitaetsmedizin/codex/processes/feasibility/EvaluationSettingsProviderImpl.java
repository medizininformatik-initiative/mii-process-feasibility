package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

import java.util.Objects;

/**
 * Can provide several information necessary for running the evaluation pipeline step.
 */
public class EvaluationSettingsProviderImpl implements EvaluationSettingsProvider {

    private final EvaluationStrategy evaluationStrategy;
    private final boolean evaluationResultObfuscationEnabled;

    public EvaluationSettingsProviderImpl(EvaluationStrategy evaluationStrategy,
                                          Boolean evaluationResultObfuscationEnabled) {
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
}
