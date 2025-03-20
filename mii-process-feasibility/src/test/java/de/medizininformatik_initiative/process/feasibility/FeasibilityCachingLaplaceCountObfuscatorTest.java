package de.medizininformatik_initiative.process.feasibility;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class FeasibilityCachingLaplaceCountObfuscatorTest {

    private record ResultPair(int originalResult, int obfuscatedResult) {
    }

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
    @DisplayName("obfuscated originalResult is rounded to nearest tens")
    public void checkIfObfuscatedResultIsRoundedToNearestTens() {
        var tries = 10000;
        var maxPopulation = 100000000;
        var avgDev = 20;
        var outlierPercentage = 3;
        var rand = new Random();
        var obfuscatedResults = IntStream.range(0, tries).boxed()
                .map(i -> rand.nextInt(0, maxPopulation))
                .map(r -> new ResultPair(r, obfuscator.obfuscate(r)))
                .toList();

        assertThat(obfuscatedResults).describedAs("obfuscated originalResult is rounded to tens")
                .allMatch(rp -> rp.obfuscatedResult % 10 == 0);
        assertThat(obfuscatedResults.stream().filter(rp -> Math.abs(rp.obfuscatedResult - rp.originalResult) >= avgDev))
                .describedAs("Percentage of outliers deviating more than %d is less than %d%% (test size: %d)", avgDev,
                        outlierPercentage, tries)
                .hasSizeLessThan(Math.round(tries * outlierPercentage / 100.0f));
    }

    @RepeatedTest(100)
    @DisplayName("for the same originalResult value the obfuscater always returns the same obfuscated originalResult")
    public void obfuscatedResultStaysSameForSameInputValue() {
        Integer resultA = obfuscator.obfuscate(RESULT);
        Integer resultB = obfuscator.obfuscate(RESULT);

        assertThat(resultA).isEqualTo(resultB);
    }

    @ParameterizedTest
    @MethodSource("resultRange")
    @DisplayName("the obfuscated originalResult is always >= 0 for any given originalResult")
    public void nonNegativeObfuscatedResult(Integer result) {
        assertThat(obfuscator.obfuscate(result)).isGreaterThanOrEqualTo(0);
    }
}
