package org.codehaus.jackson.map.deser;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.ThrowableDeserializer} instead.
 */
@Deprecated
public class ThrowableDeserializer
    extends org.codehaus.jackson.map.deser.std.ThrowableDeserializer
{
    public ThrowableDeserializer(BeanDeserializer baseDeserializer) {
        super(baseDeserializer);
    }
}
