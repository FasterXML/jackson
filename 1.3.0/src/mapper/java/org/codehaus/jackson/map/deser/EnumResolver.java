package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.AnnotationIntrospector;

import java.util.*;

/**
 * Helper class used to resolve String values (either Json Object field
 * names or regular String values) into Java Enum instances.
 */
public final class EnumResolver<T extends Enum<T>>
{
    protected final Class<T> _enumClass;

    protected final T[] _enums;

    protected final HashMap<String, T> _enumsById;

    private EnumResolver(Class<T> enumClass, T[] enums, HashMap<String, T> map)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = map;
    }

    public static <ET extends Enum<ET>> EnumResolver<ET> constructFor(Class<ET> enumCls, AnnotationIntrospector ai)
    {
        ET[] enumValues = enumCls.getEnumConstants();
        HashMap<String, ET> map = new HashMap<String, ET>();
        for (ET e : enumValues) {
            map.put(ai.findEnumValue(e), e);
        }
        return new EnumResolver<ET>(enumCls, enumValues, map);
    }

    /**
     * This method is needed because of the dynamic nature of constructing Enum
     * resolvers.
     */
    @SuppressWarnings("unchecked")
    public static EnumResolver<?> constructUnsafe(Class<?> rawEnumCls, AnnotationIntrospector ai)
    {            
        /* This is oh so wrong... but at least ugliness is mostly hidden in just
         * this one place.
         */
        Class<Enum> enumCls = (Class<Enum>) rawEnumCls;
        return constructFor(enumCls, ai);
    }
    
    public T findEnum(String key)
    {
        return _enumsById.get(key);
    }

    public T getEnum(int index)
    {
        if (index < 0 || index >= _enums.length) {
            return null;
        }
        return _enums[index];
    }

    public Class<T> getEnumClass() { return _enumClass; }

    public int lastValidIndex() { return _enums.length-1; }
}

