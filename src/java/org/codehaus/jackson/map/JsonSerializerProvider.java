package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Abstract class that defines API used by {@link JavaTypeMapper} and
 * {@link JsonSerializer}s to obtain serializers capable of serializing
 * instances of specific types.
 *<p>
 * Note about usage: for {@link JsonSerializer} instances, only accessors
 * for locating other (sub-)serializers are to be used. {@link JavaTypeMapper},
 * on the other hand, is to initialize recursive serialization process by
 * calling {@link #serializeValue}.
 */
public abstract class JsonSerializerProvider
{
    /*
    //////////////////////////////////////////////////////
    // Entry point for JavaTypeMapper
    //////////////////////////////////////////////////////
     */

    /**
     * The method to be called by {@link JavaTypeMapper} to
     * execute recursive serialization, using serializers that
     * this provider has access to.
     *
     * @param jsf Underlying factory object used for creating serializers
     *    as needed
     */
    public abstract void serializeValue(JsonGenerator jgen, Object value,
                                        JsonSerializerFactory jsf)
        throws IOException, JsonGenerationException;

    /*
    //////////////////////////////////////////////////////
    // General serializer locating method
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get hold of a serializer for a value of given type;
     * or if no such serializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     *<p>
     * Note: this method is only called for non-null values; not for keys
     * or null values. For these, check out other accessor methods.
     */
    public abstract JsonSerializer<Object> findValueSerializer(Class<?> type);

    /*
    //////////////////////////////////////////////////////
    // Accessors for specialized serializers
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get the serializer to use for serializing
     * non-null Map keys. Separation from regular
     * {@link #findValueSerializer} method is because actual write
     * method must be different (@link JsonGenerator#writeFieldName};
     * but also since behavior for some key types may differ.
     *<p>
     * Note that the serializer itself can be called with instances
     * of any Java object, but not nulls.
     */
    public abstract JsonSerializer<Object> getKeySerializer();

    /**
     * Method called to get the serializer to use for serializing
     * Map keys that are nulls: this is needed since Json does not allow
     * any non-String value as key, including null.
     *<p>
     * Typically, returned serializer
     * will either throw an exception, or use an empty String; but
     * other behaviors are possible.
     */
    public abstract JsonSerializer<Object> getNullKeySerializer();

    /**
     * Method called to get the serializer to use for serializing
     * values (root level, Array members or List field values)
     * that are nulls. Specific accessor is needed because nulls
     * in Java do not contain type information.
     *<p>
     * Typically returned serializer just writes out Json literal
     * null value.
     */
    public abstract JsonSerializer<Object> getNullValueSerializer();

    /**
     * Method called to get the serializer to use if provider
     * can not determine an actual type-specific serializer
     * to use; typically when none of {@link JsonSerializerFactory}
     * instances are able to construct a serializer.
     *<p>
     * Typically, returned serializer will throw an exception,
     * although alternatively {@link org.codehaus.jackson.map.ser.ToStringSerializer} could
     * be returned as well.
     *
     * @param unknownType Type for which no serializer is found
     */
    public abstract JsonSerializer<Object> getUnknownTypeSerializer(Class<?> unknownType);
}
