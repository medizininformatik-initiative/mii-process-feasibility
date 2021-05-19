package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EvaluationSettingsProviderImplTest {

    @Test
    public void testEvaluationStrategyRepresentation() {
        EvaluationSettingsProvider provider;
        for (EvaluationStrategy strategy : EvaluationStrategy.values()) {
            provider = new EvaluationSettingsProviderImpl(strategy, false);
            assertEquals(strategy.getStrategyRepresentation(), provider.evaluationStrategyRepresentation());
        }
    }

    @Test
    public void testEvaluationResultObfuscationEnabled() {
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, true);
        assertTrue(provider.evaluationResultObfuscationEnabled());
    }

    @Test
    public void testEvaluationResultObfuscationDisabled() {
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, false);
        assertFalse(provider.evaluationResultObfuscationEnabled());
    }
}
