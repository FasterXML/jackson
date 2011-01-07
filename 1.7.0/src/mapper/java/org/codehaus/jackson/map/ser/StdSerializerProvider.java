package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.util.Date;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ser.impl.ReadOnlyClassToSerializerMap;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.RootNameLookup;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.schema.SchemaAware;
import org.codehaus.jackson.type.JavaType;

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
 *<p>
 * Starting with version 1.5, provider is also responsible for
 * some parts of type serialization; specifically for locating
 * proper type serializers to use for types.
 */
public class StdSerializerProvider
    extends SerializerProvider
{
    /**
     * Setting for determining whether mappings for "unknown classes" should be
     * cached for faster resolution. Usually this isn't needed, but maybe it
     * is in some cases?
     */
    final static boolean CACHE_UNKNOWN_MAPPINGS = false;

    public final static JsonSerializer<Object> DEFAULT_NULL_KEY_SERIALIZER =
        new FailingSerializer("Null key for a Map not allowed in Json (use a converting NullKeySerializer?)");

    public final static JsonSerializer<Object> DEFAULT_KEY_SERIALIZER = new StdKeySerializer();

    public final static JsonSerializer<Object> DEFAULT_UNKNOWN_SERIALIZER = new SerializerBase<Object>(Object.class)
    {
        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonMappingException
        {
            // 27-Nov-2009, tatu: As per [JACKSON-201] may or may not fail...
            if (provider.isEnabled(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS)) {
                failForEmpty(value);
            }
            // But if it's fine, we'll just output empty JSON Object:
            jgen.writeStartObject();
            jgen.writeEndObject();
        }

        // since 1.6.2; needed to retain type information
        @Override
        public final void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            if (provider.isEnabled(SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS)) {
                failForEmpty(value);
            }
            typeSer.writeTypePrefixForObject(value, jgen);
            typeSer.writeTypeSuffixForObject(value, jgen);
        }
        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint) throws JsonMappingException {
            return null;
        }

        protected void failForEmpty(Object value) throws JsonMappingException
        {
            throw new JsonMappingException("No serializer found for class "+value.getClass().getName()+" and no properties discovered to create BeanSerializer (to avoid exception, disable SerializationConfig.Feature.FAIL_ON_EMPTY_BEANS) )");
        }
    };

    /*
    /**********************************************************
    /* Configuration, factories
    /**********************************************************
     */

    final protected SerializerFactory _serializerFactory;

    final protected SerializerCache _serializerCache;

    final protected RootNameLookup _rootNames;
    
    /*
    /**********************************************************
    /* Configuration, specialized serializers
    /**********************************************************
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
    protected JsonSerializer<Object> _nullValueSerializer = NullSerializer.instance;

    /**
     * Serializer used to (try to) output a null key, due to an entry of
     * {@link java.util.Map} having null key.
     * The default implementation will throw an exception if this happens;
     * alternative implementation (like one that would write an Empty String)
     * can be defined.
     */
    protected JsonSerializer<Object> _nullKeySerializer = DEFAULT_NULL_KEY_SERIALIZER;

    /*
    /**********************************************************
    /* State, for non-blueprint instances
    /**********************************************************
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
    /**********************************************************
    /* Life-cycle
    /**********************************************************
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
        _rootNames = new RootNameLookup();
    }

    /**
     * "Copy-constructor", used from {@link #createInstance} (or by
     * sub-classes)
     *
     * @param src Blueprint object used as the baseline for this instance
     */
    protected StdSerializerProvider(SerializationConfig config,
            StdSerializerProvider src, SerializerFactory f)
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
        _rootNames = src._rootNames;

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
    /**********************************************************
    /* Methods to be called by ObjectMapper
    /**********************************************************
     */

    @Override
    public final void serializeValue(SerializationConfig config,
            JsonGenerator jgen, Object value, SerializerFactory jsf)
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
    public final void serializeValue(SerializationConfig config, JsonGenerator jgen,
            Object value, JavaType rootType, SerializerFactory jsf)
        throws IOException, JsonGenerationException
    {
        if (jsf == null) {
            throw new IllegalArgumentException("Can not pass null serializerFactory");
        }
        StdSerializerProvider inst = createInstance(config, jsf);
        if (inst.getClass() != getClass()) {
            throw new IllegalStateException("Broken serializer provider: createInstance returned instance of type "+inst.getClass()+"; blueprint of type "+getClass());
        }
        inst._serializeValue(jgen, value, rootType);
    }
    
    @Override
    public JsonSchema generateJsonSchema(Class<?> type, SerializationConfig config, SerializerFactory jsf)
            throws JsonMappingException
    {
        if (type == null) {
            throw new IllegalArgumentException("A class must be provided");
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
        /* no need for embedded type information for JSON schema generation (all
         * type information it needs is accessible via "untyped" serializer)
         */
        JsonSerializer<Object> ser = inst.findValueSerializer(type, null);
        JsonNode schemaNode = (ser instanceof SchemaAware) ?
                ((SchemaAware) ser).getSchema(inst, null) : 
                JsonSchema.getDefaultSchemaNode();
        if (!(schemaNode instanceof ObjectNode)) {
            throw new IllegalArgumentException("Class " + type.getName() +
                    " would not be serialized as a JSON object and therefore has no schema");
        }

        return new JsonSchema((ObjectNode) schemaNode);
    }

    @Override
    public boolean hasSerializerFor(SerializationConfig config,
                                    Class<?> cls, SerializerFactory jsf)
    {
        return createInstance(config, jsf)._findExplicitUntypedSerializer(cls, null) != null;
    }
    
    /*
    /**********************************************************
    /* Configuration methods
    /**********************************************************
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

    @Override
    public int cachedSerializersCount() {
        return _serializerCache.size();
    }

    @Override
    public void flushCachedSerializers() {
        _serializerCache.flush();
    }
    
    /*
    /**********************************************************
    /* Abstract method implementations, value/type serializers
    /**********************************************************
     */

    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> findValueSerializer(Class<?> valueType,
            BeanProperty property)
        throws JsonMappingException
    {
        // Fast lookup from local lookup thingy works?
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            // If not, maybe shared map already has it?
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                // ... possibly as fully typed?
                ser = _serializerCache.untypedValueSerializer(TypeFactory.type(valueType));
                if (ser == null) {
                    // If neither, must create
                    ser = _createAndCacheUntypedSerializer(valueType, property);
                    // Not found? Must use the unknown type serializer
                    /* Couldn't create? Need to return the fallback serializer, which
                     * most likely will report an error: but one question is whether
                     * we should cache it?
                     */
                    if (ser == null) {
                        ser = getUnknownTypeSerializer(valueType);
                        // Should this be added to lookups?
                        if (CACHE_UNKNOWN_MAPPINGS) {
                            _serializerCache.addNonTypedSerializer(valueType, ser);
                        }
                        return ser;
                    }
                }
            }
        }            
        if (ser instanceof ContextualSerializer<?>) {
            return ((ContextualSerializer<Object>) ser).createContextual(_config, property);
        }
        return ser;
    }

    /**
     * This variant was added in 1.5, to allow for efficient access using full
     * structured types, not just classes. This is necessary for accurate
     * handling of external type information, to handle polymorphic types.
     */    
    @SuppressWarnings("unchecked")
    @Override
    public JsonSerializer<Object> findValueSerializer(JavaType valueType, BeanProperty property)
        throws JsonMappingException
    {
        // Fast lookup from local lookup thingy works?
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(valueType);
        if (ser == null) {
            // If not, maybe shared map already has it?
            ser = _serializerCache.untypedValueSerializer(valueType);
            if (ser == null) {
                // If neither, must create
                ser = _createAndCacheUntypedSerializer(valueType, property);
                // Not found? Must use the unknown type serializer
                /* Couldn't create? Need to return the fallback serializer, which
                 * most likely will report an error: but one question is whether
                 * we should cache it?
                 */
                if (ser == null) {
                    ser = getUnknownTypeSerializer(valueType.getRawClass());
                    // Should this be added to lookups?
                    if (CACHE_UNKNOWN_MAPPINGS) {
                        _serializerCache.addNonTypedSerializer(valueType, ser);
                    }
                    return ser;
                }
            }
        }
        if (ser instanceof ContextualSerializer<?>) {
            return ((ContextualSerializer<Object>) ser).createContextual(_config, property);
        }
        return ser;
    }
    
    /**
     * @param cache Whether resulting value serializer should be cached or not; this is just
     *    a hint 
     */
    @Override
    public JsonSerializer<Object> findTypedValueSerializer(Class<?> valueType, boolean cache,
            BeanProperty property)
        throws JsonMappingException
    {
        // Two-phase lookups; local non-shared cache, then shared:
        JsonSerializer<Object> ser = _knownSerializers.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }
        // If not, maybe shared map already has it?
        ser = _serializerCache.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }

        // Well, let's just compose from pieces:
        ser = findValueSerializer(valueType, property);
        TypeSerializer typeSer = _serializerFactory.createTypeSerializer(_config,
                TypeFactory.type(valueType), property);
        if (typeSer != null) {
            ser = new WrappedSerializer(typeSer, ser);
        }
        if (cache) {
            _serializerCache.addTypedSerializer(valueType, ser);
        }
        return ser;
    }
    
    @Override
    public JsonSerializer<Object> findTypedValueSerializer(JavaType valueType, boolean cache,
            BeanProperty property)
        throws JsonMappingException
    {
        // Two-phase lookups; local non-shared cache, then shared:
        JsonSerializer<Object> ser = _knownSerializers.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }
        // If not, maybe shared map already has it?
        ser = _serializerCache.typedValueSerializer(valueType);
        if (ser != null) {
            return ser;
        }

        // Well, let's just compose from pieces:
        ser = findValueSerializer(valueType, property);
        TypeSerializer typeSer = _serializerFactory.createTypeSerializer(_config, valueType, property);
        if (typeSer != null) {
            ser = new WrappedSerializer(typeSer, ser);
        }
        if (cache) {
            _serializerCache.addTypedSerializer(valueType, ser);
        }
        return ser;
    }
    
    /*
    /**********************************************************
    /* Abstract method implementations, other serializers
    /**********************************************************
     */

    @Override
    public JsonSerializer<Object> getKeySerializer(JavaType valueType, BeanProperty property)
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
    /**********************************************************
    /* Abstract method impls, convenience methods
    /**********************************************************
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
    /**********************************************************
    /* Helper methods: can be overridden by sub-classes
    /**********************************************************
     */
    
    /**
     * Method called on the actual non-blueprint provider instance object,
     * to kick off the serialization.
     */
    protected  void _serializeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonProcessingException
    {
        JsonSerializer<Object> ser;
        boolean wrap;

        if (value == null) {
            ser = getNullValueSerializer();
            wrap = false; // no name to use for wrapping; can't do!
        } else {
            Class<?> cls = value.getClass();
            // true, since we do want to cache root-level typed serializers (ditto for null property)
            ser = findTypedValueSerializer(cls, true, null);
            // [JACKSON-163]
            wrap = _config.isEnabled(SerializationConfig.Feature.WRAP_ROOT_VALUE);
            if (wrap) {
                jgen.writeStartObject();
                jgen.writeFieldName(_rootNames.findRootName(value.getClass(), _config));
            }
        }
        try {
            ser.serialize(value, jgen, this);
            if (wrap) {
                jgen.writeEndObject();
            }
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

    /**
     * Method called on the actual non-blueprint provider instance object,
     * to kick off the serialization, when root type is explicitly
     * specified and not determined from value.
     */
    protected  void _serializeValue(JsonGenerator jgen, Object value, JavaType rootType)
        throws IOException, JsonProcessingException
    {
        // [JACKSON-163]
        boolean wrap;

        JsonSerializer<Object> ser;
        if (value == null) {
            ser = getNullValueSerializer();
            wrap = false;
        } else {
            // Let's ensure types are compatible at this point
            if (!rootType.getRawClass().isAssignableFrom(value.getClass())) {
                throw new JsonMappingException("Incompatible types: declared root type ("+rootType+") vs "
                        +value.getClass().getName());
            }
            // root value, not reached via property:
            ser = findTypedValueSerializer(rootType, true, null);
            // [JACKSON-163]
            wrap = _config.isEnabled(SerializationConfig.Feature.WRAP_ROOT_VALUE);
            if (wrap) {
                jgen.writeStartObject();
                jgen.writeFieldName(_rootNames.findRootName(rootType, _config));
            }
        }
        try {
            ser.serialize(value, jgen, this);
            if (wrap) {
                jgen.writeEndObject();
            }
        } catch (IOException ioe) { // no wrapping for IO (and derived)
            throw ioe;
        } catch (Exception e) { // but others do need to be, to get path etc
            String msg = e.getMessage();
            if (msg == null) {
                msg = "[no message for "+e.getClass().getName()+"]";
            }
            throw new JsonMappingException(msg, e);
        }
    }

    /**
     * Method that will try to find a serializer, either from cache
     * or by constructing one; but will not return an "unknown" serializer
     * if this can not be done but rather returns null.
     *
     * @return Serializer if one can be found, null if not.
     */
    protected JsonSerializer<Object> _findExplicitUntypedSerializer(Class<?> runtimeType,
            BeanProperty property)
    {        
        // Fast lookup from local lookup thingy works?
        JsonSerializer<Object> ser = _knownSerializers.untypedValueSerializer(runtimeType);
        if (ser != null) {
            return ser;
        }
        // If not, maybe shared map already has it?
        ser = _serializerCache.untypedValueSerializer(runtimeType);
        if (ser != null) {
            return ser;
        }
        try {
            return _createAndCacheUntypedSerializer(runtimeType, property);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Method that will try to construct a value serializer; and if
     * one is succesfully created, cache it for reuse.
     */
    protected JsonSerializer<Object> _createAndCacheUntypedSerializer(Class<?> type,
            BeanProperty property)
        throws JsonMappingException
    {        
        JsonSerializer<Object> ser;
        try {
            ser = _createUntypedSerializer(TypeFactory.type(type), property);
        } catch (IllegalArgumentException iae) {
            /* We better only expose checked exceptions, since those
             * are what caller is expected to handle
             */
            throw new JsonMappingException(iae.getMessage(), null, iae);
        }

        if (ser != null) {
            _serializerCache.addNonTypedSerializer(type, ser);
            /* Finally: some serializers want to do post-processing, after
             * getting registered (to handle cyclic deps).
             */
            if (ser instanceof ResolvableSerializer) {
                _resolveSerializer((ResolvableSerializer)ser);
            }
        }
        return ser;
    }

    /**
     * @since 1.5
]     */
    protected JsonSerializer<Object> _createAndCacheUntypedSerializer(JavaType type,
            BeanProperty property)
        throws JsonMappingException
    {        
        JsonSerializer<Object> ser;
        try {
            ser = _createUntypedSerializer(type, property);
        } catch (IllegalArgumentException iae) {
            /* We better only expose checked exceptions, since those
             * are what caller is expected to handle
             */
            throw new JsonMappingException(iae.getMessage(), null, iae);
        }
    
        if (ser != null) {
            _serializerCache.addNonTypedSerializer(type, ser);
            /* Finally: some serializers want to do post-processing, after
             * getting registered (to handle cyclic deps).
             */
            if (ser instanceof ResolvableSerializer) {
                _resolveSerializer((ResolvableSerializer)ser);
            }
        }
        return ser;
    }

    protected JsonSerializer<Object> _createUntypedSerializer(JavaType type,
            BeanProperty property)
        throws JsonMappingException
    {
        /* 10-Dec-2008, tatu: Is there a possibility of infinite loops
         *   here? Shouldn't be, given that we do not pass back-reference
         *   to this provider. But if there is, we'd need to sync calls,
         *   and keep track of creation chain to look for loops -- fairly
         *   easy to do, but won't add yet since it seems unnecessary.
         */
        return (JsonSerializer<Object>)_serializerFactory.createSerializer(_config, type, property);
    }

    protected void _resolveSerializer(ResolvableSerializer ser)
        throws JsonMappingException
    {
        ser.resolve(this);
    }
    
    /*
    /**********************************************************
    /*  Helper classes
    /**********************************************************
     */

    /**
     * Simple serializer that will call configured type serializer, passing
     * in configured data serializer, and exposing it all as a simple
     * serializer.
     */
    private final static class WrappedSerializer
        extends JsonSerializer<Object>
    {
        final protected TypeSerializer _typeSerializer;
        final protected JsonSerializer<Object> _serializer;

        public WrappedSerializer(TypeSerializer typeSer, JsonSerializer<Object> ser)
        {
            super();
            _typeSerializer = typeSer;
            _serializer = ser;
        }

        @Override
        public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
        {
            _serializer.serializeWithType(value, jgen, provider, _typeSerializer);
        }

        @Override
        public void serializeWithType(Object value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonProcessingException
        {
            /* Is this an erroneous call? For now, let's assume it is not, and
             * that type serializer is just overridden if so
             */
            _serializer.serializeWithType(value, jgen, provider, typeSer);
        }
        
        @Override
        public Class<Object> handledType() { return Object.class; }
    }
}
