package de.medizininformatik_initiative.process.feasibility;

import java.time.Period;

public record FeasibilityContext(String evaluationStrategy, Boolean evaluationObfuscate,
        Double evaluationObfuscationSensitivity,
        Double evaluationObfuscationEpsilon, Integer rateLimitCount, Period rateLimitIntervalDuration,
        ClientStoreConfig store) {
}
