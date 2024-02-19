package de.medizininformatik_initiative.process.feasibility;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;

import java.security.SecureRandom;

/**
 * Obfuscator utilizing Laplace distributed random numbers to obfuscate result counts and caching each obfuscated result
 * for its given original result.
 *
 * @author <a href="mailto:math2306@hotmail.com">Mathias Rühle</a>
 */
public class FeasibilityCachingLaplaceCountObfuscator implements Obfuscator<Integer> {

    private static final int MAX_CACHE_SIZE = 10000;
    private LoadingCache<Integer, Integer> cache;
    private SecureRandom randomGenerator;

    public FeasibilityCachingLaplaceCountObfuscator(double sensitivity, double epsilon) {
        randomGenerator = new SecureRandom();
        cache = Caffeine.newBuilder()
                .maximumSize(MAX_CACHE_SIZE)
                .build(key -> load(key, sensitivity, epsilon));
    }

    @Override
    public Integer obfuscate(Integer value) {
        return cache.get(value);
    }

    private int load(Integer key, double sensitivity, double epsilon) {
        return (int) privatize(key, laplace(0, sensitivity, epsilon, randomGenerator));
    }

    /**
     * Permute a value with the (epsilon, 0) laplacian mechanism.
     * <p>
     * Copied from
     * https://github.com/medizininformatik-initiative/feasibility-aktin-plugin/blob/cd13545b9b1cec06049a05083717e38a2054cfd5/src/main/java/feasibility/SamplyLaplace.java
     * </p>
     *
     * @param value clear value to permute
     * @param obfuscationNum number to obfuscate the result by
     * @return the permuted value
     */
    private long privatize(int value, double obfuscationNum) {
        return Math.max(0, Math.round((value + obfuscationNum) / 10) * 10);
    }

    /**
     * Draw from a laplacian distribution.
     * <p>
     * Copied from
     * https://github.com/medizininformatik-initiative/feasibility-aktin-plugin/blob/cd13545b9b1cec06049a05083717e38a2054cfd5/src/main/java/feasibility/SamplyLaplace.java
     * </p>
     *
     * @param meanDist mean of distribution divDist diversity of distribution
     */
    private static double laplace(int meanDist, double sensitivity, double epsilon, SecureRandom rand) {
        var divDist = sensitivity / epsilon;
        var min = -0.5;
        var max = 0.5;
        var random = rand.nextDouble();
        var uniform = min + random * (max - min);
        return meanDist - divDist * Math.signum(uniform) * Math.log(1 - 2 * Math.abs(uniform));
    }
}
