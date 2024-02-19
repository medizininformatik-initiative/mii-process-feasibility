package de.medizininformatik_initiative.process.feasibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeasibilityCachingLaplaceCountObfuscatorTest {

    private static final int RESULT = 490715;
    private FeasibilityCachingLaplaceCountObfuscator obfuscator;

    private static Stream<Integer> resultRange() {
        return IntStream.rangeClosed(-100, 1000).boxed();
    }

    @BeforeEach
    public void setUp() {
        obfuscator = new FeasibilityCachingLaplaceCountObfuscator(1, 0.5);
    }

    @Test
    @DisplayName("obfuscated result is rounded to nearest tens")
    public void checkIfObfuscatedResultIsNearestTens() {
        var result = 490715;
        var obfuscatedResult = obfuscator.obfuscate(result);

        assertTrue(obfuscatedResult % 10 == 0, "obfuscated result is not rounded to tens");
        assertTrue(Math.abs(result - obfuscatedResult) < 10, "obfuscated result is not rounded to nearest tens");
    }

    @RepeatedTest(100)
    @DisplayName("for the same result value the obfuscater always returns the same obfuscated result")
    public void obfuscatedResultStaysSameForSameInputValue() {
        assertEquals(obfuscator.obfuscate(RESULT), obfuscator.obfuscate(RESULT));
    }

    @ParameterizedTest
    @MethodSource("resultRange")
    @DisplayName("the obfuscated result is always >= 0 for any given result")
    public void nonNegativeObfuscatedResult(Integer result) {
        assertThat(obfuscator.obfuscate(result), greaterThanOrEqualTo(0));
    }
}
