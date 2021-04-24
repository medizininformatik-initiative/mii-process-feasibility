package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

public interface EvaluationStrategyProvider {
    /**
     * Provides an evaluation strategy.
     *
     * @return An evaluation strategy.
     */
    EvaluationStrategy provideEvaluationStrategy();
}
