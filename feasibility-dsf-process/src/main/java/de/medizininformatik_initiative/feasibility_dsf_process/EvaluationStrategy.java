package de.medizininformatik_initiative.feasibility_dsf_process;

import java.util.Arrays;
import java.util.Objects;

/**
 * Determines which evaluation strategy to be used within the process.
 */
public enum EvaluationStrategy {

    /**
     * Measures will be evaluated using CQL.
     * Requires a FHIR server capable of handling CQL queries.
     */
    CQL("cql"),

    /**
     * Measures will be evaluated using a structured query.
     */
    STRUCTURED_QUERY("structured-query");

    private final String strategyRepresentation;

    EvaluationStrategy(String strategyRepresentation) {
        this.strategyRepresentation = Objects.requireNonNull(strategyRepresentation);
    }

    /**
     * Gets the string representation of this evaluation strategy.
     *
     * @return The evaluation strategy's string representation.
     */
    @Override
    public String toString() {
        return strategyRepresentation;
    }

    /**
     * Gets the appropriate evaluation strategy instance for a given representation.
     *
     * @param strategyRepresentation An evaluation strategy's string representation.
     * @return The appropriate evaluation strategy instance.
     * @throws IllegalArgumentException If the given strategy representation is not supported.
     */
    public static EvaluationStrategy fromStrategyRepresentation(String strategyRepresentation) {
        return Arrays.stream(EvaluationStrategy.values())
                .filter(es -> es.strategyRepresentation.equalsIgnoreCase(strategyRepresentation))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No known evaluation strategy with the representation: " + strategyRepresentation));
    }
}
