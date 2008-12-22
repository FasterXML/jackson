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
     * These are commonly seen types, for which we'll just reuse
     * flyweight eagerly constructed type instances. This to reduce
     * memory usage a bit (for type-heavy systems) and maybe improve
     * performance a bit too.
     */

    /**
     * Type of elements in the array.
     */
    final JavaType _componentType;

    public ArrayType(Class<?> arrayClass, JavaType componentType)
    {
        super(arrayClass);
        _hashCode += componentType.hashCode();
        _componentType = componentType;
    }

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
