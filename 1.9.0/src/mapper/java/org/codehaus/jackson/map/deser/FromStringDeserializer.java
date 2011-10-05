package org.codehaus.jackson.map.deser;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.FromStringDeserializer} instead.
 */
@Deprecated
public abstract class FromStringDeserializer<T>
    extends org.codehaus.jackson.map.deser.std.FromStringDeserializer<T>
{
    protected FromStringDeserializer(Class<?> vc) {
        super(vc);
    }
}
