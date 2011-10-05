package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.ArrayType;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.ObjectArrayDeserializer} instead.
 */
@Deprecated
public class ArrayDeserializer
    extends org.codehaus.jackson.map.deser.std.ObjectArrayDeserializer
{
    /**
     * @deprecated
     */
    @Deprecated
    public ArrayDeserializer(ArrayType arrayType, JsonDeserializer<Object> elemDeser)
    {
        this(arrayType, elemDeser, null);
    }

    public ArrayDeserializer(ArrayType arrayType, JsonDeserializer<Object> elemDeser,
            TypeDeserializer elemTypeDeser)
    {
        super(arrayType, elemDeser, elemTypeDeser);
    }
}

