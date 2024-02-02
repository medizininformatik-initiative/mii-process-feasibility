package de.medizininformatik_initiative.process.feasibility;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimitTest {

    @Test
    @DisplayName("once the limit has been exceeded it stays exceeded")
    void noResetLimit() throws Exception {
        var rateLimit = new RateLimit(1, Duration.ofSeconds(1));

        assertTrue(rateLimit.countRequestAndCheckLimit());
        assertFalse(rateLimit.countRequestAndCheckLimit());

        sleep(1000);

        assertFalse(rateLimit.countRequestAndCheckLimit());
    }

    @Test
    @DisplayName("if the inital limit is 0 it stays exceeded from the first check")
    void initialLimitIsZero() throws Exception {
        var rateLimit = new RateLimit(0, Duration.ofSeconds(1));

        assertFalse(rateLimit.countRequestAndCheckLimit());

        sleep(1000);

        assertFalse(rateLimit.countRequestAndCheckLimit());
    }

    @Test
    @DisplayName("rate gets reset after time interval")
    void countGetsResetAfterTimeRunsOut() throws Exception {
        var rateLimit = new RateLimit(10, Duration.ofSeconds(1));

        for (var i = 0; i < 10; i++) {
            assertTrue(rateLimit.countRequestAndCheckLimit());
        }

        sleep(1100);

        for (var i = 0; i < 10; i++) {
            assertTrue(rateLimit.countRequestAndCheckLimit());
        }
    }
}
