package org.codehaus.jackson.map.type;

import java.util.*;

/**
 * Array types represent Java arrays, both primitive and object valued.
 * Further, Object-valued arrays can have element type of any other
 * legal {@link JavaType}.
 */
public final class ArrayType
    extends JavaType
{
    /**
     * Type of elements in the array.
     */
    final JavaType _componentType;

    final boolean _fullyTyped;

    public ArrayType(Class<?> arrayClass, JavaType componentType)
    {
        super(arrayClass);
        _hashCode += componentType.hashCode();
        _componentType = componentType;
        _fullyTyped = componentType.isFullyTyped();
    }

    /**
     * Method that can be called to add known simple types into given
     * class-to-type map.
     */
    protected static void addCommonTypes(Map<String, JavaType> types)
    {
        /**
         * These are commonly seen simple array types, for which we'll just reuse
         * flyweight eagerly constructed type instances. This to reduce
         * memory usage a bit (for type-heavy systems) and maybe improve
         * performance a bit too.
         */
        Class<?>[] classes = new Class<?>[] {
            // First, primitive arrays
            boolean[].class, char[].class,
                byte[].class, short[].class, int[].class, long[].class,
                float[].class, double[].class,
                
                // let's skip wrappers here (i.e. just construct as/if needed)
                
                // Then other basic types
                
                Object.class,
                String.class,
                };
        for (Class<?> cls : classes) {
            SimpleType st = SimpleType.construct(cls.getComponentType());
            types.put(cls.getName(), new ArrayType(cls, st));
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////////////
     */

    /**
     * Simple types are always fully typed: this requires that
     * no instances are ever created for Collection/Map types.
     */
    public boolean isFullyTyped() { return _fullyTyped; }

    /*
    //////////////////////////////////////////////////////////
    // Standard methods
    //////////////////////////////////////////////////////////
     */

    @Override
        public String toString()
    {
        return "[array type, component type: "+_componentType+"]";
    }

    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) return false;

        ArrayType other = (ArrayType) o;
        return _componentType.equals(other._componentType);
    }
}
