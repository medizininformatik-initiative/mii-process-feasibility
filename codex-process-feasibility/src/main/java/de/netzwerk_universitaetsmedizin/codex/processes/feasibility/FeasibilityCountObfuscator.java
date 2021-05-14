package de.netzwerk_universitaetsmedizin.codex.processes.feasibility;

/**
 * Obfuscator for obfuscating feasibility counts.
 */
public class FeasibilityCountObfuscator {

    /**
     * Obfuscates the given feasibility count by calculating the same count rounded to the nearest ten.
     *
     * @param feasibilityCount The feasibility count that shall be obfuscated.
     * @return The obfuscated feasibility count.
     */
    public int obfuscate(int feasibilityCount) {
        return feasibilityCount - (feasibilityCount % 10) + 10;
    }
}
