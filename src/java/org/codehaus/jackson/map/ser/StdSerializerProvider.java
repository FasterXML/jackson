package org.codehaus.jackson.map.ser;

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
    public final static JsonSerializer<Object> DEFAULT_NULL_KEY_SERIALIZER =
        new StdSerializerFactory.FailingSerializer("Null key for a Map not allower in Json (use a converting NullKeySerializer?)");

    public final static JsonSerializer<Object> DEFAULT_KEY_SERIALIZER = new StdKeySerializer();

    /*
    ////////////////////////////////////////////////////
    // Configuration, factories
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

    /*
    ////////////////////////////////////////////////////
    // Configuration, specialized serializers
    ////////////////////////////////////////////////////
     */

    /**
     * Serializer that gets called for values of types for which no
     * serializers can be constructed.
     *<p>
     * The default serializer will thrown an exception; a possible
     * alternative that can be used would be
     * {@link ToStringSerializer}.
     */
    protected JsonSerializer<Object> _unknownTypeSerializer;

    /**
     * Serializer used to output non-null keys of Maps (which will get
     * output as Json Objects).
     */
    protected JsonSerializer<Object> _keySerializer = DEFAULT_KEY_SERIALIZER;

    /**
     * Serializer used to output a null value. Default implementation
     * writes nulls using {@link JsonGenerator#writeNull}.
     */
    protected JsonSerializer<Object> _nullValueSerializer = StdSerializerFactory.NullSerializer.instance;

    /**
     * Serializer used to (try to) output a null key, due to an entry of
     * {@link java.util.Map} having null key.
     * The default implementation will throw an exception if this happens;
     * alternative implementation (like one that would write an Empty String)
     * can be defined.
     */
    protected JsonSerializer<Object> _nullKeySerializer = DEFAULT_NULL_KEY_SERIALIZER;

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

    public void setKeySerializer(JsonSerializer<Object> ks)
    {
        if (ks == null) {
            throw new IllegalArgumentException("Can not pass null JsonSerializer");
        }
        _keySerializer = ks;
    }

    public void setNullValueSerializer(JsonSerializer<Object> nvs)
    {
        if (nvs == null) {
            throw new IllegalArgumentException("Can not pass null JsonSerializer");
        }
        _nullValueSerializer = nvs;
    }

    public void setNullKeySerializer(JsonSerializer<Object> nks)
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
        if (_overrideSerializerFactory != null) {
            JsonSerializer<?> ser = _overrideSerializerFactory.createSerializer(type);
            if (ser != null) {
                return ser;
            }
        }
        JsonSerializer<?> ser = _serializerFactory.createSerializer(type);
        if (ser != null) {
            return ser;
        }
        /* No match yet? Need to return the fallback serializer, which
         * most likely will report an error
         */
        return _unknownTypeSerializer;
    }

    @Override
    public final JsonSerializer<Object> getKeySerializer()
    {
        return _keySerializer;
    }

    @Override
    public final JsonSerializer<Object> getNullKeySerializer() {
        return _nullKeySerializer;
    }

    @Override
    public final JsonSerializer<Object> getNullValueSerializer() {
        return _nullValueSerializer;
    }
}

