package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.type.JavaType;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.StdDeserializer} instead.
 */
@Deprecated
public abstract class StdDeserializer<T>
    extends org.codehaus.jackson.map.deser.std.StdDeserializer<T>
{
    protected StdDeserializer(Class<?> vc) {
        super(vc);
    }

    protected StdDeserializer(JavaType valueType) {
        super(valueType);
    }
}
