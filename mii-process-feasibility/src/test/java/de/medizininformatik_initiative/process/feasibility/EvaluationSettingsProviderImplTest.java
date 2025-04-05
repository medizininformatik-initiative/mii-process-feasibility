package de.medizininformatik_initiative.process.feasibility;


import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.time.Duration.ofSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class EvaluationSettingsProviderImplTest {

    @Test
    public void testEvaluationStrategyRepresentation() {
        EvaluationSettingsProvider provider;
        for (EvaluationStrategy strategy : EvaluationStrategy.values()) {
            provider = new EvaluationSettingsProviderImpl(strategy, false, 0d, 0d, 0, Duration.ofSeconds(1));
            assertEquals(strategy, provider.evaluationStrategy());
        }
    }

    @Test
    public void testEvaluationResultObfuscationEnabled() {
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, true, 1d, 0d,
                0, Duration.ofSeconds(1));
        assertTrue(provider.evaluationResultObfuscationEnabled());
    }

    @Test
    public void testEvaluationResultObfuscationDisabled() {
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, false, 1d, 0d,
                0, Duration.ofSeconds(1));
        assertFalse(provider.evaluationResultObfuscationEnabled());
    }

    @Test
    @DisplayName("is returned value of rate limit time interval seconds equal to input")
    public void rateLimitTimeInterval() {
        var duration = ofSeconds(134651);
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, false, 1d, 0d,
                0, duration);
        assertEquals(duration, provider.getRateLimitTimeIntervalDuration());
    }

    @Test
    @DisplayName("invalid rate limit time interval ends in error")
    public void invalidRateLimitTimeInterval() {
        assertThrows(IllegalArgumentException.class,
                () -> new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, false, 1d, 0d, 0, Duration.ZERO));
    }

    @Test
    @DisplayName("is returned value of rate limit equal to input")
    public void rateLimitMaxCount() {
        Integer limit = 134651;
        EvaluationSettingsProvider provider = new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, false, 1d, 0d,
                limit, Duration.ofSeconds(1));
        assertEquals(limit, provider.getRateLimitCount());
    }

    @Test
    @DisplayName("invalid rate limit ends in error")
    public void invalidRateLimit() {
        assertThrows(IllegalArgumentException.class,
                () -> new EvaluationSettingsProviderImpl(EvaluationStrategy.CQL, false, 1d, 0d, -140942,
                        Duration.ofSeconds(1)));
    }
}
