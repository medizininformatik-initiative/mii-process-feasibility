package de.medizininformatik_initiative.feasibility_dsf_process;

import java.time.Duration;

/**
 * Provides different settings required for evaluating a measure.
 */
public interface EvaluationSettingsProvider {

    /**
     * Returns the evaluation strategy.
     *
     * @return the evaluation strategy.
     */
    EvaluationStrategy evaluationStrategy();

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
    /**
     * Provides the user specified maximum number of request allowed for the given time interval
     *
     * @return maximum number of allowed requests (>= 0)
     */
    public Integer getRateLimitCount();

    /**
     * Provides the time interval duration in which the request rate limit is evaluated
     *
     * @return time duration of rate limit interval
     */
    public Duration getRateLimitTimeIntervalDuration();
}
