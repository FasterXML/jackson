package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.util.EnumResolver;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.EnumDeserializer} instead.
 */
@Deprecated
public final class EnumSetDeserializer
    extends org.codehaus.jackson.map.deser.std.EnumSetDeserializer
{
    @SuppressWarnings("rawtypes")
    public EnumSetDeserializer(EnumResolver enumRes)
    {
        super(enumRes);
    }
}