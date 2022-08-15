package de.medizininformatik_initiative.feasibility_dsf_process;

import java.util.Random;

import static java.util.Objects.requireNonNull;

/**
 * Obfuscator for obfuscating feasibility counts.
 */
public class FeasibilityCountObfuscator implements Obfuscator<Integer> {

    private static final Integer MAX_RANDOM_OBFUSCATOR_VALUE = 10;

    // Obfuscated feasibility counts less than this value get discarded by setting
    // the obfuscation result to 0.
    private static final Integer MIN_ALLOWED_OBFUSCATED_RESULT = 5;

    // We can't use RandomGenerator of Java 17 since this plugin will run within a framework still
    // using Java 11 and is built for this version. Thus, using a lightweight wrapper should be fine.
    private final RandomNumberGenerator randomGenerator;

    public FeasibilityCountObfuscator(RandomNumberGenerator randomGenerator) {
        this.randomGenerator = requireNonNull(randomGenerator, "random number generator must not be null");
    }

    /**
     * Obfuscates the given feasibility count by randomly adding a value in the range of [-5, 5] to it.
     * <p>
     * Important:
     * <b>Returns 0, should the obfuscated result be less than 5.</b>
     * </p>
     *
     * @param feasibilityCount The feasibility count that shall be obfuscated.
     * @return The obfuscated feasibility count.
     */
    public Integer obfuscate(Integer feasibilityCount) {
        var obfuscationOffset = randomGenerator.generateRandomNumber(MAX_RANDOM_OBFUSCATOR_VALUE + 1)
                - (MAX_RANDOM_OBFUSCATOR_VALUE / 2);

        var obfuscatedFeasibilityCount = feasibilityCount + obfuscationOffset;
        return (obfuscatedFeasibilityCount < MIN_ALLOWED_OBFUSCATED_RESULT) ? 0 : obfuscatedFeasibilityCount;
    }

    public static class ObfuscationRandomNumberGenerator implements RandomNumberGenerator {

        private final Random random;

        public ObfuscationRandomNumberGenerator() {
            random = new Random();
        }

        @Override
        public int generateRandomNumber(int bound) {
            return random.nextInt(bound);
        }
    }
}
