package org.codehaus.jackson.map.deser;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.StdScalarDeserializer} instead.
 */
@Deprecated
public abstract class StdScalarDeserializer<T>
    extends org.codehaus.jackson.map.deser.std.StdDeserializer<T>
{
    protected StdScalarDeserializer(Class<?> vc) {
        super(vc);
    } 
}
