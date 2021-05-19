package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class FeasibilityCountObfuscatorTest {

    private FeasibilityCountObfuscator obfuscator;

    @Before
    public void setUp() {
        obfuscator = new FeasibilityCountObfuscator();
    }

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {0, 10}, {5, 10}, {10, 20}, {20, 30}, {100, 110}, {113, 120}
        });
    }

    @Parameter
    public int feasibilityCount;

    @Parameter(1)
    public int expectedObfuscatedFeasibilityCount;


    @Test
    public void testObfuscateFeasibility() {
        assertEquals(expectedObfuscatedFeasibilityCount, obfuscator.obfuscate(feasibilityCount));
    }
}
