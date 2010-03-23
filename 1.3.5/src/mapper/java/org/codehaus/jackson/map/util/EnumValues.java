package org.codehaus.jackson.map.util;

import java.util.*;

import org.codehaus.jackson.map.*;

public final class EnumValues
{
    private final EnumMap<?,String> _values;

    @SuppressWarnings("unchecked")
    private EnumValues(Map<Enum<?>,String> v) {
        _values = new EnumMap(v);
    }

    @SuppressWarnings("unchecked")
    public static EnumValues construct(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        /* [JACKSON-214]: Enum types with per-instance sub-classes
         *   need special handling
         */
        Class<Enum<?>> curr = enumClass;
        do {
            Enum<?>[] values = curr.getEnumConstants();
            if (values != null) {
                // Type juggling... unfortunate
                Map<Enum<?>,String> map = new HashMap<Enum<?>,String>();
                for (Enum<?> en : values) {
                    map.put(en, intr.findEnumValue(en));
                }
                return new EnumValues(map);
            }
            curr = (Class<Enum<?>>)curr.getEnclosingClass();
        } while (curr != null);
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
