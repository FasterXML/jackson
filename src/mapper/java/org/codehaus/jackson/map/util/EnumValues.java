package org.codehaus.jackson.map.util;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public final class EnumValues
{
    private final EnumMap<?,String> _values;

    @SuppressWarnings("unchecked")
    private EnumValues(Map<Enum<?>,String> v) {
        _values = new EnumMap(v);
    }

    public static EnumValues construct(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        // Type juggling... unfortunate
        Map<Enum<?>,String> map = new HashMap<Enum<?>,String>();
        for (Enum<?> en : enumClass.getEnumConstants()) {
            map.put(en, intr.findEnumValue(en));
        }
        return new EnumValues(map);
    }

    public String valueFor(Enum<?> key)
    {
        return _values.get(key);
    }

    public Collection<String> values() {
        return _values.values();
    }
}
