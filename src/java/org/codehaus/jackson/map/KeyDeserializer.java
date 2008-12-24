package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Abstract class that defines API used for deserializing Json content
 * field names into Java Map keys. These deserializers are only used
 * if the Map key class is not <code>String</code> or <code>Object</code>.
 */
public abstract class KeyDeserializer
{
    public abstract Object deserializeKey(String key, JsonDeserializationContext ctxt)
        throws IOException, JsonProcessingException;
}
