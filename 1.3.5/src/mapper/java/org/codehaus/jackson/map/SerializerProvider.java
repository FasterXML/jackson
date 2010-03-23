package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.Date;

import org.codehaus.jackson.*;
import org.codehaus.jackson.schema.JsonSchema;

/**
 * Abstract class that defines API used by {@link ObjectMapper} and
 * {@link JsonSerializer}s to obtain serializers capable of serializing
 * instances of specific types.
 *<p>
 * Note about usage: for {@link JsonSerializer} instances, only accessors
 * for locating other (sub-)serializers are to be used. {@link ObjectMapper},
 * on the other hand, is to initialize recursive serialization process by
 * calling {@link #serializeValue}.
 */
public abstract class SerializerProvider
{
    protected final SerializationConfig _config;

    protected SerializerProvider(SerializationConfig config)
    {
        _config = config;
    }

    /*
    //////////////////////////////////////////////////////
    // Methods that ObjectMapper will call
    //////////////////////////////////////////////////////
     */

    /**
     * The method to be called by {@link ObjectMapper} to
     * execute recursive serialization, using serializers that
     * this provider has access to.
     *
     * @param jsf Underlying factory object used for creating serializers
     *    as needed
     */
    public abstract void serializeValue(SerializationConfig cfg,
                                        JsonGenerator jgen, Object value,
                                        SerializerFactory jsf)
        throws IOException, JsonGenerationException;

    /**
     * Generate <a href="http://json-schema.org/">Json-schema</a> for
     * given type.
     *
     * @param type The type for which to generate schema
     */
    public abstract JsonSchema generateJsonSchema(Class<?> type, SerializationConfig config, SerializerFactory jsf)
        throws JsonMappingException;

    /**
     * Method that can be called to see if this serializer provider
     * can find a serializer for an instance of given class.
     *<p>
     * Note that no Exceptions are thrown, including unchecked ones:
     * implementations are to swallow exceptions if necessary.
     */
    public abstract boolean hasSerializerFor(SerializationConfig cfg,
                                             Class<?> cls, SerializerFactory jsf);

    /*
    //////////////////////////////////////////////////////
    // Access to configuration
    //////////////////////////////////////////////////////
     */

    public final SerializationConfig getConfig() { return _config; }

    public final boolean isEnabled(SerializationConfig.Feature f) {
        return _config.isEnabled(f);
    }

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
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable serializer; including that of not
     *   finding any serializer
     */
    public abstract JsonSerializer<Object> findValueSerializer(Class<?> type)
        throws JsonMappingException;

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
     * to use; typically when none of {@link SerializerFactory}
     * instances are able to construct a serializer.
     *<p>
     * Typically, returned serializer will throw an exception,
     * although alternatively {@link org.codehaus.jackson.map.ser.ToStringSerializer} could
     * be returned as well.
     *
     * @param unknownType Type for which no serializer is found
     */
    public abstract JsonSerializer<Object> getUnknownTypeSerializer(Class<?> unknownType);

    /*
    //////////////////////////////////////////////////////
    // Convenience methods
    //////////////////////////////////////////////////////
     */

    /**
     * Convenience method that will serialize given value (which can be
     * null) using standard serializer locating functionality. It can
     * be called for all values including field and Map values, but usually
     * field values are best handled calling
     * {@link #defaultSerializeField} instead.
     */
    public final void defaultSerializeValue(Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        if (value == null) {
            getNullValueSerializer().serialize(null, jgen, this);
        } else {
            findValueSerializer(value.getClass()).serialize(value, jgen, this);
        }
    }

    /**
     * Convenience method that will serialize given field with specified
     * value. Value may be null. Serializer is done using the usual
     * null) using standard serializer locating functionality.
     */
    public final void defaultSerializeField(String fieldName, Object value, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        jgen.writeFieldName(fieldName);
        if (value == null) {
            /* Note: can't easily check for suppression at this point
             * any more; caller must check it.
             */
            getNullValueSerializer().serialize(null, jgen, this);
        } else {
            findValueSerializer(value.getClass()).serialize(value, jgen, this);
        }
    }

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     */
    public abstract void defaultSerializeDateValue(long timestamp, JsonGenerator jgen)
        throws IOException, JsonProcessingException;

    /**
     * Method that will handle serialization of Date(-like) values, using
     * {@link SerializationConfig} settings to determine expected serialization
     * behavior.
     */
    public abstract void defaultSerializeDateValue(Date date, JsonGenerator jgen)
        throws IOException, JsonProcessingException;
}
