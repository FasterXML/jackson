package org.codehaus.jackson.map.deser;

import java.util.HashMap;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.util.EnumResolver} instead.
 */
@Deprecated
public final class EnumResolver<T extends Enum<T>>
    extends org.codehaus.jackson.map.util.EnumResolver<T>
{
    private EnumResolver(Class<T> enumClass, T[] enums, HashMap<String, T> map) {
        super(enumClass, enums, map);
    }
}


