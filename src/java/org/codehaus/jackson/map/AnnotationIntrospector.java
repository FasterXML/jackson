package org.codehaus.jackson.map;

import java.lang.annotation.Annotation;

/**
 * Abstract class that defines API used for introspecting annotation-based
 * configuration for serialization and deserialization. Separated
 * so that different sets of annotations can be supported, and support
 * plugged-in dynamically.
 */
public abstract class AnnotationIntrospector
{
    /**
     * Method called by framework to determine whether given annotation
     * is handled by this introspector.
     */
    public abstract boolean isHandled(Annotation ann);
}
