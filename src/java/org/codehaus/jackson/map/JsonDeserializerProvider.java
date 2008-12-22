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
    /*
    //////////////////////////////////////////////////////
    // Entry points for JavaTypeMapper
    //////////////////////////////////////////////////////
     */

    /**
     * The method to be called by {@link JavaTypeMapper} to
     * execute recursive serialization, using deserializers that
     * this provider has access to.
     *
     * @param jp Parser to use to read  Json content
     * @param type Type of the root value object to deserialize
     */
    public abstract void deserializeValue(JsonParser jp, JavaType type)
        throws IOException, JsonParseException;

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
    public abstract JsonDeserializer<Object> findValueDeserializer(JavaType type);

    /*
    //////////////////////////////////////////////////////
    // Accessors for specialized deserializers
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get the deserializer to use if provider
     * can not determine an actual type-specific deserializer
     * to use; typically when none of {@link JsonDeserializerFactory}
     * instances are able to construct a deserializer.
     *<p>
     * Usually returned deserializer will throw an exception,
     * although alternative strategies (like maybe returning null)
     * could be used as well.
     */
    public abstract JsonDeserializer<Object> getUnknownTypeDeserializer();
}
