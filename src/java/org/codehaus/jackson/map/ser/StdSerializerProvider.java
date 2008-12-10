package org.codehaus.jackson.map.ser;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerFactory;
import org.codehaus.jackson.map.JsonSerializerProvider;
import org.codehaus.jackson.map.Resolvable;

/**
 * Default {@link JsonSerializerProvider} implementation.
 * By default will use {@link BeanSerializerFactory} as the underlying
 * serializer factory, but can be configured to use any other implementation.
 * Usually overrides are used to provide a sub-class of the standard
 * serializer factory that provides hand-written (or otherwise more
 * optimal) serializer classes.
 *<p>
 * One note about implementation: the main instance constructed will
 * be so-called "blueprint" object, and will NOT be used during actual
 * serialization. Rather, an "instance" instance is created so that
 * state can be carried along, as well as to avoid synchronization
 * during serializer access. Because of this, if sub-classing, one
 * must override method {@link #createInstance}: if this is not done,
 * an exception will get thrown as base class verifies that the
 * instance has same class as the blueprint
 * (<code>instance.getClass() == blueprint.getClass()</code>).
 * Check is done to prevent weird bugs that would otherwise occur.
 */
public class StdSerializerProvider
    extends JsonSerializerProvider
{
    /**
     * Setting for determining whether mappings for "unknown classes" should be
     * cached for faster resolution. Usually this isn't needed, but maybe it
     * is in some cases?
     *<p>
     * TODO: make configurable
     */
    final static boolean CACHE_UNKNOWN_MAPPINGS = false;

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

    final protected SerializerCache _serializerCache;

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
    // State, for non-blueprint instances
    ////////////////////////////////////////////////////
     */

    /**
     * For fast lookups, we will have a local non-shared read-only
     * map that contains serializers previously fetched.
     */
    private final ReadOnlyClassToSerializerMap _knownSerializers;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
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
        _serializerCache = new SerializerCache();
        // Blueprints doesn't have access to any serializers...
        _knownSerializers = null;
    }

    /**
     * "Copy-constructor", used from {@link #createInstance} (or by
     * sub-classes)
     */
    protected StdSerializerProvider(StdSerializerProvider src)
    {
        _serializerFactory = src._serializerFactory;
        _serializerCache = src._serializerCache;
        _unknownTypeSerializer = src._unknownTypeSerializer;
        _keySerializer = src._keySerializer;
        _nullValueSerializer = src._nullValueSerializer;
        _nullKeySerializer = src._nullKeySerializer;

        /* Non-blueprint instances do have a read-only map; one that doesn't need
         * synchronization for lookups.
         */
        _knownSerializers = _serializerCache.getReadOnlyLookupMap();
    }

    /**
     * Overridable method, used to create a non-blueprint instances from the blueprint.
     * This is needed to retain state during serialization.
     */
    protected StdSerializerProvider createInstance()
    {
        return new StdSerializerProvider(this);
    }

    /*
    ////////////////////////////////////////////////////
    // Main entry method to be called by JavaTypeMapper
    ////////////////////////////////////////////////////
     */

    public final void serializeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException
    {
        /* First: we need a separate instance, which will hold a distinct copy of the
         * non-shared ("local") read-only lookup Map for fast class-to-serializer
         * lookup
         */
        StdSerializerProvider inst = createInstance();
        // sanity check to avoid weird errors; to ensure sub-classes do override createInstance
        if (inst.getClass() != getClass()) {
            throw new IllegalStateException("Broken serializer provider: createInstance returned instance of type "+inst.getClass()+"; blueprint of type "+getClass());
        }
        // And then we can do actual serialization, through the instance
        inst._serializeValue(jgen, value);
    }

    /**
     * Method called on the actual non-blueprint provider instance object, to kick off
     * the serialization.
     */
    protected  void _serializeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException
    {
        JsonSerializer<Object> ser = (value == null) ?
            getNullValueSerializer() : findValueSerializer(value.getClass());
        ser.serialize(value, jgen, this);
    }

    /*
    ////////////////////////////////////////////////////
    // Configuration methods
    ////////////////////////////////////////////////////
     */

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
    public JsonSerializer<Object> findValueSerializer(Class<?> type)
    {
        // Fast lookup from local lookup thingy works?
        JsonSerializer<Object> ser = _knownSerializers.get(type);
        if (ser != null) {
            return ser;
        }
        // If not, maybe shared map already has it?
        ser = _serializerCache.findSerializer(type);
        if (ser != null) {
            return ser;
        }

        /* If neither, must create. So far so good: creation should be
         * safe...
         */
        ser = _createSerializer(type);

        /* Couldn't create? Need to return the fallback serializer, which
         * most likely will report an error: but one question is whether
         * we should cache it?
         */
        if (ser == null) {
            ser = _unknownTypeSerializer;
            // Should this be added to lookups?
            if (!CACHE_UNKNOWN_MAPPINGS) {
                return ser;
            }
        }
        _serializerCache.addSerializer(type, ser);
        /* Finally: some serializers want to do post-processing, after
         * getting registered (to handle cyclic deps).
         */
        if (ser instanceof Resolvable) {
            _resolveSerializer((Resolvable)ser);
        }
        return ser;
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

    @Override
    public final JsonSerializer<Object> getUnknownTypeSerializer() {
        return _unknownTypeSerializer;
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Helper methods: can be overridden by sub-classes
    ////////////////////////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
    protected JsonSerializer<Object> _createSerializer(Class<?> type)
    {
        /* 10-Dec-2008, tatu: Is there a possibility of infinite loops
         *   here? Shouldn't be, given that we do not pass back-reference
         *   to this provider. But if there is, we'd need to sync calls,
         *   and keep track of creation chain to look for loops -- fairly
         *   easy to do, but won't add yet since it seems unnecessary.
         */
        return (JsonSerializer<Object>)_serializerFactory.createSerializer(type);
    }

    protected void _resolveSerializer(Resolvable ser)
    {
        ser.resolve(this);
    }
}

