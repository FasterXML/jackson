package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.util.EnumResolver;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.EnumDeserializer} instead.
 */
@Deprecated
public class EnumDeserializer
    extends org.codehaus.jackson.map.deser.std.EnumDeserializer
{
    public EnumDeserializer(EnumResolver<?> res) {
        super(res);
    }
}
