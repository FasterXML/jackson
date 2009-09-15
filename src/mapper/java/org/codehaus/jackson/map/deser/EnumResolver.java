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

    /**
     *
     */
    protected final HashMap<String, T> _enumsById;

    private EnumResolver(Class<T> enumClass, T[] enums, HashMap<String, T> map)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = map;
    }

    public static <ET extends Enum<ET>> EnumResolver constructFor(Class<ET> enumCls, AnnotationIntrospector ai)
    {
        T[] enumValues = enumCls.getEnumConstants();
        HashMap<String, ET> map = new HashMap<String, ET>();
        for (ET e : enumValues) {
            map.put(ai.findEnumValue(e), e);
        }
        return new EnumResolver<ET>(enumCls, enumValues, map);
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

