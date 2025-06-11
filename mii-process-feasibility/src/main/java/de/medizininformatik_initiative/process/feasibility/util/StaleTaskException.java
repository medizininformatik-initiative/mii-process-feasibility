package de.medizininformatik_initiative.process.feasibility.util;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class StaleTaskException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public StaleTaskException(String taskId, Instant requestDate, Instant currentDate, Duration requestTimeout) {
        super("Task (%s) is stale at %s. The task was requested at %s and the task request timeout is %s."
                .formatted(taskId,
                        DateTimeFormatter.ISO_INSTANT.format(currentDate),
                        DateTimeFormatter.ISO_INSTANT.format(requestDate),
                        requestTimeout));
    }
}
