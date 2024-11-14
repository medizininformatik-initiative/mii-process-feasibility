package de.medizininformatik_initiative.process.feasibility;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.security.SecureRandom;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FeasibilityCachingLaplaceCountObfuscatorTest {

    private static final int RESULT = 490715;
    private static SecureRandom rand;

    private FeasibilityCachingLaplaceCountObfuscator obfuscator;

    private static Stream<Integer> resultRange() {
        return IntStream.rangeClosed(-100, 1000).boxed();
    }

    @BeforeAll
    public static void init() {
        rand = new SecureRandom();
    }

    @BeforeEach
    public void setUp() {
        obfuscator = new FeasibilityCachingLaplaceCountObfuscator(1, 0.28);
    }

    @RepeatedTest(100)
    @DisplayName("obfuscated result is rounded to nearest tens")
    public void checkIfObfuscatedResultIsNearestTens() {
        var result = rand.nextInt(0, Integer.MAX_VALUE);
        var obfuscatedResult = obfuscator.obfuscate(result);

        assertThat(obfuscatedResult % 10).as("obfuscated result is not rounded to tens").isEqualTo(0);
        assertThat(Math.abs(result - obfuscatedResult)).as("obfuscated result is not rounded to nearest tens")
                .isLessThanOrEqualTo(10);
    }

    @RepeatedTest(100)
    @DisplayName("for the same result value the obfuscater always returns the same obfuscated result")
    public void obfuscatedResultStaysSameForSameInputValue() {
        Integer resultA = obfuscator.obfuscate(RESULT);
        Integer resultB = obfuscator.obfuscate(RESULT);

        assertThat(resultA).isEqualTo(resultB);
    }

    @ParameterizedTest
    @MethodSource("resultRange")
    @DisplayName("the obfuscated result is always >= 0 for any given result")
    public void nonNegativeObfuscatedResult(Integer result) {
        assertThat(obfuscator.obfuscate(result)).isGreaterThanOrEqualTo(0);
    }
}
