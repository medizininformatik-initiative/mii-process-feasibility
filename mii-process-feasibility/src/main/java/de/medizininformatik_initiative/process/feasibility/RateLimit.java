package de.medizininformatik_initiative.process.feasibility;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import io.github.bucket4j.local.LocalBucket;

import java.time.Duration;

/**
 * Implements rate limiting with a hard limit.
 * <p>
 * Each
 * </p>
 *
 * @author <a href="mailto:math2306@hotmail.com">Mathias Rühle</a>
 */
public class RateLimit {
    private final LocalBucket bucket;
    private boolean isLimitExceeded;

    public RateLimit(long requestLimit, Duration intervalDuration) {
        isLimitExceeded = requestLimit <= 0;
        bucket = Bucket.builder()
                .addLimit(Bandwidth.classic(Math.max(1, requestLimit),
                        Refill.intervally(Math.max(1, requestLimit), intervalDuration)))
                .build();
    }

    /**
     * Checks if the rate limit has already been exceeded or else increments the request count in the current rate limit
     * time interval and checks again if the limit has been exceeded.
     *
     * @return true, if the current request count is still below or equal the rate limit <br>
     *         no, if the rate limit has been exceeded once
     */
    public boolean countRequestAndCheckLimit() {
        if (isLimitExceeded) {
            return false;
        }

        if (bucket.tryConsume(1)) {
            return true;
        } else {
            isLimitExceeded = true;
            return false;
        }
    }
}
