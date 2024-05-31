package de.medizininformatik_initiative.process.feasibility;

/**
 * Describes a simple random number generator.
 */
public interface RandomNumberGenerator {

    /**
     * Returns a pseudorandom, uniformly distributed int value between 0 (inclusive) and the specified value (exclusive).
     *
     * @param bound The upper bound (must be positive).
     * @return A pseudorandom, uniformly distributed int value between zero (inclusive) and bound (exclusive)
     */
    int generateRandomNumber(int bound);
}
