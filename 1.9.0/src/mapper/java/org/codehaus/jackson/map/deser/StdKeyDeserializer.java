package org.codehaus.jackson.map.deser;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.StdKeyDeserializer} instead.
 */
@Deprecated
public abstract class StdKeyDeserializer
    extends org.codehaus.jackson.map.deser.std.StdKeyDeserializer
{
    protected StdKeyDeserializer(Class<?> cls) { super(cls); }
}

