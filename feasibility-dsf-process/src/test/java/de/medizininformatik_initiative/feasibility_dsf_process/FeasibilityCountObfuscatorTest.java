package de.medizininformatik_initiative.feasibility_dsf_process;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FeasibilityCountObfuscatorTest {

    @Mock
    private RandomNumberGenerator randomNumberGenerator;

    @InjectMocks
    private FeasibilityCountObfuscator feasibilityCountObfuscator;

    @Test
    public void testObfuscateFeasibility_ObfuscatedResultsLowerThanFiveGetReturnedAsZero() {
        when(randomNumberGenerator.generateRandomNumber(anyInt())).thenReturn(1, 2, 3, 4);

        int nonObfuscatedFeasibilityResult = 5;
        List<Integer> obfuscatedFeasibilityResults = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            obfuscatedFeasibilityResults.add(feasibilityCountObfuscator.obfuscate(nonObfuscatedFeasibilityResult));
        }

        assertIterableEquals(List.of(0, 0, 0, 0), obfuscatedFeasibilityResults);
    }

    @Test
    public void testObfuscateFeasibility_ObfustedResultsGreaterOrEqualToFiveAreKept() {
        when(randomNumberGenerator.generateRandomNumber(anyInt())).thenReturn(0, 5, 10);

        int nonObfuscatedFeasibilityResult = 10;
        List<Integer> obfuscatedFeasibilityResults = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            obfuscatedFeasibilityResults.add(feasibilityCountObfuscator.obfuscate(nonObfuscatedFeasibilityResult));
        }

        assertIterableEquals(List.of(5, 10, 15), obfuscatedFeasibilityResults);
    }
}
