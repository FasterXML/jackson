package org.codehaus.jackson.map;

import org.codehaus.jackson.map.type.JavaType;

/**
 * Abstract class that defines API used by {@link ObjectMapper} and
 * {@link JsonDeserializer}s to obtain deserializers capable of
 * re-constructing instances of handled type from Json content.
 */
public abstract class DeserializerProvider
{
    protected DeserializerProvider() { }

    /*
    //////////////////////////////////////////////////////
    // General deserializer locating method
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get hold of a deserializer for a value of given type;
     * or if no such deserializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     *<p>
     * Note: this method is only called for non-null values; not for keys
     * or null values. For these, check out other accessor methods.
     */
    public abstract JsonDeserializer<Object> findValueDeserializer(JavaType type, DeserializerFactory f);

    /**
     * Method called to get hold of a deserializer to use for deserializing
     * keys for {@link java.util.Map}.
     */
    public abstract KeyDeserializer findKeyDeserializer(JavaType type, DeserializerFactory f);
}
