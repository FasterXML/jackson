package org.codehaus.jackson.map.util;

import java.util.*;

import org.codehaus.jackson.map.*;

/**
 * Helper class used for storing String serializations of
 * enumerations.
 */
public final class EnumValues
{
    private final EnumMap<?,String> _values;

    @SuppressWarnings("unchecked")
    private EnumValues(Map<Enum<?>,String> v) {
        _values = new EnumMap(v);
    }

    public static EnumValues construct(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        return constructFromName(enumClass, intr);
    }

    public static EnumValues constructFromName(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        /* [JACKSON-214]: Enum types with per-instance sub-classes
         *   need special handling
         */
    	Class<? extends Enum<?>> cls = ClassUtil.findEnumType(enumClass);
        Enum<?>[] values = cls.getEnumConstants();
        if (values != null) {
            // Type juggling... unfortunate
            Map<Enum<?>,String> map = new HashMap<Enum<?>,String>();
            for (Enum<?> en : values) {
                map.put(en, intr.findEnumValue(en));
            }
            return new EnumValues(map);
        }
        throw new IllegalArgumentException("Can not determine enum constants for Class "+enumClass.getName());
    }

    public static EnumValues constructFromToString(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        Class<? extends Enum<?>> cls = ClassUtil.findEnumType(enumClass);
        Enum<?>[] values = cls.getEnumConstants();
        if (values != null) {
            // Type juggling... unfortunate
            Map<Enum<?>,String> map = new HashMap<Enum<?>,String>();
            for (Enum<?> en : values) {
                map.put(en, en.toString());
            }
            return new EnumValues(map);
        }
        throw new IllegalArgumentException("Can not determine enum constants for Class "+enumClass.getName());
    }
    
    public String valueFor(Enum<?> key)
    {
        return _values.get(key);
    }

    public Collection<String> values() {
        return _values.values();
    }
}
