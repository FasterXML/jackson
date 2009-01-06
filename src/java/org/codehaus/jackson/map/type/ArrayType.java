package org.codehaus.jackson.map.type;

import java.lang.reflect.Array;
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

    /**
     * We will also keep track of shareable instance of empty array,
     * since it usually needs to be constructed any way; and because
     * it is essentially immutable and thus can be shared.
     */
    final Object _emptyArray;

    private ArrayType(JavaType componentType, Object emptyInstance)
    {
        super(emptyInstance.getClass());
        _componentType = componentType;
        _emptyArray = emptyInstance;
        _hashCode += componentType.hashCode();
        _fullyTyped = componentType.isFullyTyped();
    }

    public static ArrayType construct(JavaType componentType)
    {
        /* This is bit messy: there is apparently no other way to
         * reconstruct actual concrete/raw array class from component
         * type, than to construct an instance, get class (same is
         * true for GenericArracyType as well; hence we won't bother
         * passing that in).
         */
        Object emptyInstance = Array.newInstance(componentType.getRawClass(), 0);
        return new ArrayType(componentType, emptyInstance);
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
            boolean.class, char.class,
                byte.class, short.class, int.class, long.class,
                float.class, double.class,
                
                // let's skip wrappers here (i.e. just construct as/if needed)
                
                // Then other basic types
                
                Object.class,
                String.class,
                };
        for (Class<?> cls : classes) {
            SimpleType compType = SimpleType.construct(cls);
            ArrayType arrayType = construct(compType);
            types.put(arrayType.getRawClass().getName(), arrayType);
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

    public boolean isContainerType() { return true; }

    public JavaType getComponentType() { return  _componentType; }

    /*
    //////////////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////////////
     */

    /**
     * Factory method that will construct an array instance with
     * specified length.
     */
    public Object newInstance(int length)
    {
        if (length == 0) {
            return _emptyArray;
        }
        return Array.newInstance(_componentType.getRawClass(), length);
    }

    public Object instanceFrom(Object[] src)
    {
        int len = src.length;

        if (len == 0) {
            return _emptyArray;
        }
        Object result = newInstance(len);
        System.arraycopy(src, 0, result, 0, len);
        return result;
    }

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
