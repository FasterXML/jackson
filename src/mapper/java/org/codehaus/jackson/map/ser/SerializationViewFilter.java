package org.codehaus.jackson.map.ser;

/**
 * Abstract class that defines whether associated property is to be
 * included in serialization/deserialization of given view.
 * 
 * @since 1.4
 */
public abstract class SerializationViewFilter {
    public abstract boolean includeInView(Class<?> view);
}
