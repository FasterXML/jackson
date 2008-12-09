package org.codehaus.jackson.map.impl.prov;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerFactory;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Default {@link JsonSerializerProvider} implementation. By default will use
 * {@link BeanSerializerFactory} as the underlying main serializer factory,
 * but can be configured to use any other implementation. Additionally,
 * an "override" serializer factory can be defined to specify explicit
 * overrides for custom classes, in cases where default serialization
 * introspection would not work, or where additional efficiency is needed.
 */
public class StdSerializerProvider
    extends SerializerProviderBase
{
    public final static JsonSerializer<?> DEFAULT_NULL_KEY_SERIALIZER =
        new StdSerializerFactory.FailingSerializer("Null key for a Map not allower in Json (use a converting NullKeySerializer?)");

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /**
     * The main serializer factory used for finding serializers.
     */
    final protected JsonSerializerFactory _serializerFactory;

    /**
     * Optional serializer factory that can be defined to provide overrides;
     * typically used to specify custom-written serializers for custom
     * classes.
     */
    protected JsonSerializerFactory _overrideSerializerFactory = null;

    /**
     * Serializer used to output a null value. Default implementation
     * writes nulls using {@link JsonGenerator#writeNull}.
     */
    protected JsonSerializer<?> _nullValueSerializer = StdSerializerFactory.NullSerializer.instance;

    /**
     * Serializer used to (try to) output a null key, due to an entry of
     * {@link Map} having null key.
     * The default implementation will throw an exception if this happens;
     * alternative implementation (like one that would write an Empty String)
     * can be defined.
     */
    protected JsonSerializer<?> _nullKeySerializer = DEFAULT_NULL_KEY_SERIALIZER;

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    public StdSerializerProvider()
    {
        this(BeanSerializerFactory.instance);
    }

    public StdSerializerProvider(JsonSerializerFactory serializerFactory)
    {
        if (serializerFactory == null) {
            throw new IllegalArgumentException("Can not pass null serializerFactory");
        }
        _serializerFactory = serializerFactory;
    }

    /*
    ////////////////////////////////////////////////////
    // Configuration methods
    ////////////////////////////////////////////////////
     */

    public void setOverrideSerializerFactory(JsonSerializerFactory jf)
    {
        _overrideSerializerFactory = jf;
    }

    public void setNullValueSerializer(JsonSerializer<?> nvs)
    {
        if (nvs == null) {
            throw new IllegalArgumentException("Can not pass null JsonSerializer");
        }
        _nullValueSerializer = nvs;
    }

    public void setNullKeySerializer(JsonSerializer<?> nks)
    {
        if (nks == null) {
            throw new IllegalArgumentException("Can not pass null JsonSerializer");
        }
        _nullKeySerializer = nks;
    }

    /*
    ////////////////////////////////////////////////////
    // Abstract methods impls
    ////////////////////////////////////////////////////
     */

    @Override
    protected JsonSerializer<?> constructValueSerializer(Class<?> type)
    {
        // !!! TBI
        return null;
    }

    @Override
    public final JsonSerializer<?> findNonNullKeySerializer(Class<?> type)
    {
        // !!! TBI
        return null;
    }

    @Override
    public final JsonSerializer<?> getNullKeySerializer() {
        return _nullKeySerializer;
    }

    @Override
    public final JsonSerializer<?> getNullValueSerializer() {
        return _nullValueSerializer;
    }
}

