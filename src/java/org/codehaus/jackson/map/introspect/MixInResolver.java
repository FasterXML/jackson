package org.codehaus.jackson.map.introspect;

/**
 * Interface for decoupling details of how mix-in annotation definitions
 * are stored, and how they are accessed.
 */
public interface MixInResolver
{
    /**
     * Method that will check if there are "mix-in" classes (with mix-in
     * annotations) for given class
     */
    public Class<?> findMixInClassFor(Class<?> cls);
}
