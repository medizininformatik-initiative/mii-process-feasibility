package de.medizininformatik_initiative.feasibility_dsf_process;

/**
 * Provides different settings required for evaluating a measure.
 */
public interface EvaluationSettingsProvider {

    /**
     * Returns a string representation of the evaluation strategy.
     *
     * @return Representation of the evaluation strategy.
     */
    String evaluationStrategyRepresentation();

    /**
     * Returns whether evaluation result obfuscation is enabled.
     *
     * @return True if evaluation result obfuscation is enabled and false otherwise.
     */
    boolean evaluationResultObfuscationEnabled();

    /**
     * Returns the sensitivity value of the Laplace algorithm to be used for obfuscating the feasibility result.
     *
     * @return value of sensitivity used for the result obfuscation Laplace algorithm
     */
    double resultObfuscationLaplaceSensitivity();

    /**
     * Returns the epsilon value of the Laplace algorithm to be used for obfuscating the feasibility result.
     *
     * @return value of epsilon used for the result obfuscation Laplace algorithm
     */
    double resultObfuscationLaplaceEpsilon();
}
