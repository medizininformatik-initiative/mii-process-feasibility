package de.medizininformatik_initiative.process.feasibility;

/**
 * Describes how values of a type T can get obfuscated.
 *
 * @param <T> The type of the value that shall get obfuscated.
 */
public interface Obfuscator<T> {

    /**
     * Obfuscates the specified value and returns the result.
     *
     * @param value Gets obfuscated.
     * @return The obfuscated value.
     */
    T obfuscate(T value);
}
