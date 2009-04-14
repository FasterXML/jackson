package org.codehaus.jackson.map.deser;

import java.util.*;

/**
 * Helper class used to resolve String values (either Json Object field
 * names or regular String values) into Java Enum instances.
 */
public final class EnumResolver
{
    protected final Class<Enum<?>> _enumClass;

    protected final Enum<?>[] _enums;

    /**
     *
     */
    protected final HashMap<String, Enum<?>> _enumsById;

    private EnumResolver(Class<Enum<?>> enumClass, Enum<?>[] enums, HashMap<String, Enum<?>> map)
    {
        _enumClass = enumClass;
        _enums = enums;
        _enumsById = map;
    }

    public static EnumResolver constructFor(Class<?> rawEnumCls)
    {
        @SuppressWarnings("unchecked")
        Class<Enum<?>> enumCls = (Class<Enum<?>>) rawEnumCls;

        Enum<?>[] enumValues = enumCls.getEnumConstants();
        HashMap<String, Enum<?>> map = new HashMap<String, Enum<?>>();
        for (Enum<?> e : enumValues) {
            map.put(e.name(), e);
        }
        return new EnumResolver(enumCls, enumValues, map);
    }

    public Enum<?> findEnum(String key)
    {
        return _enumsById.get(key);
    }

    public Enum<?> getEnum(int index)
    {
        if (index < 0 || index >= _enums.length) {
            return null;
        }
        return _enums[index];
    }

    public Class<Enum<?>> getEnumClass() { return _enumClass; }

    public int lastValidIndex() { return _enums.length-1; }
}

