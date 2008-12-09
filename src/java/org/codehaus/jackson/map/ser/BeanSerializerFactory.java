package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.JsonSerializer;

/**
 * Factory class that can provide serializers for any regular Java beans
 * (as defined by "having at least one get method recognizable as bean
 * accessor" -- where {@link Object#getClass} does not count);
 * as well as for "standard" JDK types. Latter is achieved
 * by sub-classing {@link StdSerializerFactory} to augment its functionality
 * by bean introspection
 */
public class BeanSerializerFactory
    extends StdSerializerFactory
{
    public final static BeanSerializerFactory instance = new BeanSerializerFactory();

    /*
    ////////////////////////////////////////////////////////////
    // Life cycle
    ////////////////////////////////////////////////////////////
     */

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected BeanSerializerFactory() { }

    /*
    ////////////////////////////////////////////////////////////
    // JsonSerializerFactory impl
    ////////////////////////////////////////////////////////////
     */

    /**
     * Main serializer constructor method. We will have to be careful
     * with respect to ordering of various method calls: essentially
     * we want to reliably figure out which classes are standard types,
     * and which are beans. The problem is that some bean Classes may
     * implement standard interfaces (say, {@link java.lang.Iterable}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonSerializer<T> createSerializer(Class<T> type)
    {
        // First, fast lookup for exact type:
        JsonSerializer<?> ser = findSerializerByLookup(type);
        if (ser == null) {
            // and then introspect for some safe (?) JDK types
            ser = findSerializerByPrimaryType(type);
            if (ser == null) {
                // But if no  match, let's see if it might be a bean
                ser = findBeanType(type);
                // if not, then fall back to other JDK types
                if (ser == null) {
                    ser = findSerializerByAddonType(type);
                }
            }
        }
        return (JsonSerializer<T>) ser;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////////
     */

    protected JsonSerializer<?> findBeanType(Class<?> type)
    {
        // !!! TBI
        return null;
    }
}
