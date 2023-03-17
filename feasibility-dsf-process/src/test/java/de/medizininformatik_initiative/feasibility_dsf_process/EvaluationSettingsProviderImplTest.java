package de.medizininformatik_initiative.feasibility_dsf_process;


import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EvaluationSettingsProviderImplTest {

    @Test
    public void testEvaluationStrategyRepresentation() {
        EvaluationSettingsProvider provider;
        for (EvaluationStrategy strategy : EvaluationStrategy.values()) {
            provider = new EvaluationSettingsProviderImpl(strategy, false, 0d, 0d);
            assertEquals(strategy.getStrategyRepresentation(), provider.evaluationStrategyRepresentation());
        }
    }

    @Test
    public void testEvaluationResultObfuscationEnabled() {
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, true, 1d, 0d);
        assertTrue(provider.evaluationResultObfuscationEnabled());
    }

    @Test
    public void testEvaluationResultObfuscationDisabled() {
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, false, 1d, 0d);
        assertFalse(provider.evaluationResultObfuscationEnabled());
    }
}
