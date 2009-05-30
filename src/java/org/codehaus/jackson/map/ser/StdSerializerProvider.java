package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.lang.reflect.Type;

import org.codehaus.jackson.*;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.schema.SchemaAware;

import org.codehaus.jackson.map.*;

/**
 * Default {@link SerializerProvider} implementation. Handles
 * caching aspects of serializer handling; all construction details
 * are delegated to {@link SerializerFactory} instance.
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
    extends SerializerProvider
{
    /**
     * Setting for determining whether mappings for "unknown classes" should be
     * cached for faster resolution. Usually this isn't needed, but maybe it
     * is in some cases?
     *<p>
     * TODO: make configurable?
     */
    final static boolean CACHE_UNKNOWN_MAPPINGS = false;

    public final static JsonSerializer<Object> DEFAULT_NULL_KEY_SERIALIZER =
        new FailingSerializer("Null key for a Map not allower in Json (use a converting NullKeySerializer?)");

    public final static JsonSerializer<Object> DEFAULT_KEY_SERIALIZER = new StdKeySerializer();

    public final static JsonSerializer<Object> DEFAULT_UNKNOWN_SERIALIZER = new JsonSerializer<Object>()
    {
        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws JsonMappingException
        {
            /* 18-Feb-2009, tatu: Let's suggest the most likely reason
             *   for failure to find a serializer
             */
            throw new JsonMappingException("No serializer found for class "+value.getClass().getName()+" (and no bean properties discovered to create bean serializer)");
        }
    };

    /*
    ////////////////////////////////////////////////////
    // Configuration, factories
    ////////////////////////////////////////////////////
     */

    final protected SerializerFactory _serializerFactory;

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
     * The default serializer will simply thrown an exception; a possible
     * alternative that can be used would be
     * {@link ToStringSerializer}.
     */
    protected JsonSerializer<Object> _unknownTypeSerializer = DEFAULT_UNKNOWN_SERIALIZER;

    /**
     * Serializer used to output non-null keys of Maps (which will get
     * output as Json Objects).
     */
    protected JsonSerializer<Object> _keySerializer = DEFAULT_KEY_SERIALIZER;

    /**
     * Serializer used to output a null value. Default implementation
     * writes nulls using {@link JsonGenerator#writeNull}.
     */
    protected JsonSerializer<Object> _nullValueSerializer = BasicSerializerFactory.NullSerializer.instance;

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
    protected final ReadOnlyClassToSerializerMap _knownSerializers;

    /**
     * Lazily acquired and instantiated formatter object: initialized
     * first time it is needed, reused afterwards. Used via instances
     * (not blueprints), so that access need not be thread-safe.
     */
    protected DateFormat _dateFormat;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    /**
     * Constructor for creating master (or "blue-print") provider object,
     * which is only used as the template for constructing per-binding
     * instances.
     */
    public StdSerializerProvider()
    {
        super(null);
        _serializerFactory = null;
        _serializerCache = new SerializerCache();
        // Blueprints doesn't have access to any serializers...
        _knownSerializers = null;
    }

    /**
     * "Copy-constructor", used from {@link #createInstance} (or by
     * sub-classes)
     *
     * @param src Blueprint object used as the baseline for this instance
     */
    protected StdSerializerProvider(SerializationConfig config,
                                    StdSerializerProvider src,
                                    SerializerFactory f)
    {
        super(config);
        if (config == null) {
            throw new NullPointerException();
        }
        _serializerFactory = f;

        _serializerCache = src._serializerCache;
        _unknownTypeSerializer = src._unknownTypeSerializer;
        _keySerializer = src._keySerializer;
        _nullValueSerializer = src._nullValueSerializer;
        _nullKeySerializer = src._nullKeySerializer;

        /* Non-blueprint instances do have a read-only map; one that doesn't
         * need synchronization for lookups.
         */
        _knownSerializers = _serializerCache.getReadOnlyLookupMap();
    }

    /**
     * Overridable method, used to create a non-blueprint instances from the blueprint.
     * This is needed to retain state during serialization.
     */
    protected StdSerializerProvider createInstance(SerializationConfig config, SerializerFactory jsf)
    {
        return new StdSerializerProvider(config, this, jsf);
    }

    /*
    ////////////////////////////////////////////////////
    // Methods to be called by JavaTypeMapper
    ////////////////////////////////////////////////////
     */

    @Override
    public final void serializeValue(SerializationConfig config,
                                     JsonGenerator jgen, Object value,
                                     SerializerFactory jsf)
        throws IOException, JsonGenerationException
    {
        if (jsf == null) {
            throw new IllegalArgumentException("Can not pass null serializerFactory");
        }

        /* First: we need a separate instance, which will hold a copy of the
         * non-shared ("local") read-only lookup Map for fast
         * class-to-serializer lookup
         */
        StdSerializerProvider inst = createInstance(config, jsf);
        // sanity check to avoid weird errors; to ensure sub-classes do override createInstance
        if (inst.getClass() != getClass()) {
            throw new IllegalStateException("Broken serializer provider: createInstance returned instance of type "+inst.getClass()+"; blueprint of type "+getClass());
        }
        // And then we can do actual serialization, through the instance
        inst._serializeValue(jgen, value);
    }

    @Override
    public JsonSchema generateJsonSchema(Class<?> type, SerializationConfig config, SerializerFactory jsf)
            throws JsonMappingException
    {
        if (type == null) {
            throw new IllegalArgumentException("A class must be provided.");
        }

        /* First: we need a separate instance, which will hold a copy of the
         * non-shared ("local") read-only lookup Map for fast
         * class-to-serializer lookup
         */
        StdSerializerProvider inst = createInstance(config, jsf);
        // sanity check to avoid weird errors; to ensure sub-classes do override createInstance
        if (inst.getClass() != getClass()) {
            throw new IllegalStateException("Broken serializer provider: createInstance returned instance of type "+inst.getClass()+"; blueprint of type "+getClass());
        }
        JsonSerializer<Object> ser = inst.findValueSerializer(type);
        JsonNode schemaNode = (ser instanceof SchemaAware) ?
                ((SchemaAware) ser).getSchema(inst, null) : 
                JsonSchema.getDefaultSchemaNode();
        if (!(schemaNode instanceof ObjectNode)) {
            throw new IllegalArgumentException("Class " + type.getName() +
                    " would not be serialized as a JSON object and therefore has no schema.");
        }

        return new JsonSchema((ObjectNode) schemaNode);
    }

    public boolean hasSerializerFor(SerializationConfig config,
                                    Class<?> cls, SerializerFactory jsf)
    {
        return createInstance(config, jsf)._findExplicitSerializer(cls) != null;
    }

    /**
     * Method called on the actual non-blueprint provider instance object,
     * to kick off the serialization.
     */
    protected  void _serializeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonProcessingException
    {
        JsonSerializer<Object> ser = (value == null) ?
            getNullValueSerializer() : findValueSerializer(value.getClass());
        try {
            ser.serialize(value, jgen, this);
        } catch (IOException ioe) {
            /* As per [JACKSON-99], should not wrap IOException or its
             * sub-classes (like JsonProcessingException, JsonMappingException)
             */
            throw ioe;
        } catch (Exception e) {
            // but others are wrapped
            String msg = e.getMessage();
            if (msg == null) {
                msg = "[no message for "+e.getClass().getName()+"]";
            }
            throw new JsonMappingException(msg, e);
        }
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
    // Abstract method impls, locating serializers
    ////////////////////////////////////////////////////
     */

    @Override
    public JsonSerializer<Object> findValueSerializer(Class<?> type)
        throws JsonMappingException
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

        // If neither, must create
        ser = _createAndCacheSerializer(type);
        // Not found? Must use the unknown type serializer
        /* Couldn't create? Need to return the fallback serializer, which
         * most likely will report an error: but one question is whether
         * we should cache it?
         */
        if (ser == null) {
            ser = getUnknownTypeSerializer(type);
            // Should this be added to lookups?
            if (CACHE_UNKNOWN_MAPPINGS) {
                _serializerCache.addSerializer(type, ser);
            }
        }
        return ser;
    }

    @Override
    public JsonSerializer<Object> getKeySerializer()
    {
        return _keySerializer;
    }

    @Override
    public JsonSerializer<Object> getNullKeySerializer() {
        return _nullKeySerializer;
    }

    @Override
    public JsonSerializer<Object> getNullValueSerializer() {
        return _nullValueSerializer;
    }

    @Override
    public JsonSerializer<Object> getUnknownTypeSerializer(Class<?> unknownType) {
        return _unknownTypeSerializer;
    }

    /*
    ////////////////////////////////////////////////////
    // Abstract method impls, convenience methods
    ////////////////////////////////////////////////////
     */

    /**
     * @param timestamp Millisecond timestamp that defines date, if available;
     */
    @Override
    public final void defaultSerializeDateValue(long timestamp, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // [JACKSON-87]: Support both numeric timestamps and textual
        if (isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)) {
            jgen.writeNumber(timestamp);
        } else {
            if (_dateFormat == null) {
                // must create a clone since Formats are not thread-safe:
                _dateFormat = (DateFormat)_config.getDateFormat().clone();
            }
            jgen.writeString(_dateFormat.format(new Date(timestamp)));
        }
    }

    @Override
    public final void defaultSerializeDateValue(Date date, JsonGenerator jgen)
        throws IOException, JsonProcessingException
    {
        // [JACKSON-87]: Support both numeric timestamps and textual
        if (isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)) {
            jgen.writeNumber(date.getTime());
        } else {
            if (_dateFormat == null) {
                DateFormat blueprint = _config.getDateFormat();
                // must create a clone since Formats are not thread-safe:
                _dateFormat = (DateFormat)blueprint.clone();
            }
            jgen.writeString(_dateFormat.format(date));
        }
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Helper methods: can be overridden by sub-classes
    ////////////////////////////////////////////////////////////////
     */

    /**
     * Method that will try to find a serializer, either from cache
     * or by constructing one; but will not return an "unknown" serializer
     * if this can not be done but rather returns null.
     *
     * @return Serializer if one can be found, null if not.
     */
    protected JsonSerializer<Object> _findExplicitSerializer(Class<?> type)
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
        try {
            return _createAndCacheSerializer(type);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Method that will try to construct a value aerializer; and if
     * one is succesfully created, cache it for reuse.
     */
    protected JsonSerializer<Object> _createAndCacheSerializer(Class<?> type)
        throws JsonMappingException
    {        
        JsonSerializer<Object> ser;
        try {
            ser = _createSerializer(type);
        } catch (IllegalArgumentException iae) {
            /* We better only expose checked exceptions, since those
             * are what caller is expected to handle
             */
            throw new JsonMappingException(iae.getMessage(), null, iae);
        }

        if (ser != null) {
            _serializerCache.addSerializer(type, ser);
            /* Finally: some serializers want to do post-processing, after
             * getting registered (to handle cyclic deps).
             */
            if (ser instanceof ResolvableSerializer) {
                _resolveSerializer((ResolvableSerializer)ser);
            }
        }
        return ser;
    }

    @SuppressWarnings("unchecked")
    protected JsonSerializer<Object> _createSerializer(Class<?> type)
        throws JsonMappingException
    {
        /* 10-Dec-2008, tatu: Is there a possibility of infinite loops
         *   here? Shouldn't be, given that we do not pass back-reference
         *   to this provider. But if there is, we'd need to sync calls,
         *   and keep track of creation chain to look for loops -- fairly
         *   easy to do, but won't add yet since it seems unnecessary.
         */
        return (JsonSerializer<Object>)_serializerFactory.createSerializer(type, _config);
    }

    protected void _resolveSerializer(ResolvableSerializer ser)
        throws JsonMappingException
    {
        ser.resolve(this);
    }
}

