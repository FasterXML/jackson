package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.type.JavaType;

/**
 * Abstract class that defines API used by {@link JavaTypeMapper} and
 * {@link JsonDeserializer}s to obtain deserializers capable of
 * re-constructing instances of handled type from Json content.
 *<p>
 * Note about usage: for {@link JsonDeserializer} instances, only accessors
 * for locating other (sub-)deserializers are to be used.
 * {@link JavaTypeMapper},
 * on the other hand, is to initialize recursive serialization process by
 * calling {@link #deserializeValue}.
 */
public abstract class JsonDeserializerProvider
{
    protected JsonDeserializerProvider() { }

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
    public abstract JsonDeserializer<Object> findValueDeserializer(JavaType type, JsonDeserializerFactory f);
}
