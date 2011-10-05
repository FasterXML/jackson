package org.codehaus.jackson.map.deser;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.ContainerDeserializerBase} instead.
 */
@Deprecated
public abstract class ContainerDeserializer<T>
    extends org.codehaus.jackson.map.deser.std.ContainerDeserializerBase<T>
{
    protected ContainerDeserializer(Class<?> selfType)
    {
        super(selfType);
    }
}
