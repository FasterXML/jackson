package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.util.EnumResolver;

/**
 * 
 * <p>
 * Note: casting within this class is all messed up -- just could not figure out a way
 * to properly deal with recursive definition of "EnumMap<K extends Enum<K>, V>
 * 
 * @author tsaloranta
 */
public final class EnumMapDeserializer
    extends org.codehaus.jackson.map.deser.std.EnumMapDeserializer
{
    public EnumMapDeserializer(EnumResolver<?> enumRes, JsonDeserializer<Object> valueDes)
    {
        super(enumRes, valueDes);
    }
}

