package org.codehaus.jackson.map.type;

import java.util.*;

/**
 * Simple types are defined as anything other than one of recognized
 * container types (arrays, Collections, Maps). For our needs we
 * need not know anything further, since we have no way of dealing
 * with generic types other than Collections and Maps.
 */
public final class SimpleType
    extends JavaType
{
    /**
     * These are commonly seen types, for which we'll just reuse
     * flyweight eagerly constructed type instances. This to reduce
     * memory usage a bit (for type-heavy systems) and maybe improve
     * performance a bit too.
     */
    private final static HashMap<String, SimpleType> _simpleTypes = 
        new HashMap<String, SimpleType>();
    static {
        Class<?>[] simpleClasses = new Class<?>[] {
            // First, primitives
            boolean.class, char.class,
                byte.class, short.class, int.class, long.class,
                float.class, double.class,
                
                // Then wrappers for same:
                Boolean.class, Character.class,
                Byte.class, Short.class, Integer.class, Long.class,
                Float.class, Double.class,
                
                // Then other common simple (and importantly, final) types:
                
                String.class
                };
        for (Class<?> cls : simpleClasses) {
            _simpleTypes.put(cls.getName(), new SimpleType(cls));
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////////
     */

    private SimpleType(Class<?> cls)
    {
        super(cls);
    }

    public static SimpleType construct(Class<?> cls)
    {
        SimpleType result = _simpleTypes.get(cls.getName());
        if (result == null) {
            result = new SimpleType(cls);
        }
        return result;
    }

    /**
     * Method that can be called to add known simple types into given
     * class-to-type map.
     */
    protected static void addSimpleTypes(Map<String, JavaType> types)
    {
        types.putAll(_simpleTypes);
    }

    /*
    //////////////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
        public String toString()
    {
        return "[simple type "+_class.getName()+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        SimpleType other = (SimpleType) o;

        // Classes must be identical... 
        return (other._class == this._class);
    }
}
