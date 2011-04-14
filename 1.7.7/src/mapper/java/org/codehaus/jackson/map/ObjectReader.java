package org.codehaus.jackson.map;

import java.io.*;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.deser.StdDeserializationContext;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.SubtypeResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.VersionUtil;

/**
 * Builder object that can be used for per-serialization configuration of
 * deserialization parameters, such as root type to use or object
 * to update (instead of constructing new instance).
 * Uses "fluid" (aka builder) pattern so that instances are immutable
 * (and thus fully thread-safe with no external synchronization);
 * new instances are constructed for different configurations.
 * Instances are initially constructed by {@link ObjectMapper} and can be
 * reused.
 * 
 * @author tatu
 * @since 1.6
 */
public class ObjectReader
    implements Versioned
{
    private final static JavaType JSON_NODE_TYPE = TypeFactory.type(JsonNode.class);

    /*
    /**********************************************************
    /* Immutable configuration from ObjectMapper
    /**********************************************************
     */

    /**
     * Root-level cached deserializers
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers;

    /**
     * General serialization configuration settings
     */
    protected final DeserializationConfig _config;
   
    protected final DeserializerProvider _provider;

    /**
     * Factory used for constructing {@link JsonGenerator}s
     */
    protected final JsonFactory _jsonFactory;
    
    // Support for polymorphic types:
    protected TypeResolverBuilder<?> _defaultTyper;

    // Configurable visibility limits
    protected VisibilityChecker<?> _visibilityChecker;

    /**
     * Registered concrete subtypes that can be used instead of (or
     * in addition to) ones declared using annotations.
     * 
     * @since 1.6
     */
    protected final SubtypeResolver _subtypeResolver;
    
    /*
    /**********************************************************
    /* Configuration that can be changed during building
    /**********************************************************
     */   

    /**
     * Declared type of value to instantiate during deserialization.
     * Defines which deserializer to use; as well as base type of instance
     * to construct if an updatable value is not configured to be used
     * (subject to changes by embedded type information, for polymorphic
     * types). If {@link #_valueToUpdate} is non-null, only used for
     * locating deserializer.
     */
    protected final JavaType _valueType;

    /**
     * Instance to update with data binding; if any. If null,
     * a new instance is created, if non-null, properties of
     * this value object will be updated instead.
     * Note that value can be of almost any type, except not
     * {@link org.codehaus.jackson.map.type.ArrayType}; array
     * types can not be modified because array size is immutable.
     */
    protected final Object _valueToUpdate;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     */
    protected ObjectReader(ObjectMapper mapper,  JavaType valueType, Object valueToUpdate)
    {
        _rootDeserializers = mapper._rootDeserializers;
        _defaultTyper = mapper._defaultTyper;
        _visibilityChecker = mapper._visibilityChecker;
        _subtypeResolver = mapper._subtypeResolver;
        _provider = mapper._deserializerProvider;
        _jsonFactory = mapper._jsonFactory;

        // must make a copy at this point, to prevent further changes from trickling down
        _config = mapper._deserializationConfig.createUnshared(_defaultTyper, _visibilityChecker,
                _subtypeResolver);
        
        _valueType = valueType;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectReader(ObjectReader base, DeserializationConfig config,
            JavaType valueType, Object valueToUpdate)
    {
        _rootDeserializers = base._rootDeserializers;
        _defaultTyper = base._defaultTyper;
        _visibilityChecker = base._visibilityChecker;
        _provider = base._provider;
        _jsonFactory = base._jsonFactory;
        _subtypeResolver = base._subtypeResolver;

        _config = config;
        
        _valueType = valueType;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
    }

    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     * 
     * @since 1.6
     */
    public Version version() {
        return VersionUtil.versionFor(getClass());
    }
    
    public ObjectReader withType(JavaType valueType)
    {
        if (valueType == _valueType) return this;
        // type is stored here, no need to make a copy of config
        return new ObjectReader(this, _config, valueType, _valueToUpdate);
    }    

    public ObjectReader withType(Class<?> valueType)
    {
        return withType(TypeFactory.type(valueType));
    }    

    public ObjectReader withType(java.lang.reflect.Type valueType)
    {
        return withType(TypeFactory.type(valueType));
    }    

    public ObjectReader withNodeFactory(JsonNodeFactory f)
    {
        // node factory is stored within config, so need to copy that first
        if (f == _config.getNodeFactory()) return this;
        DeserializationConfig cfg = _config.createUnshared(f);
        return new ObjectReader(this, cfg, _valueType, _valueToUpdate);
    }
    
    public ObjectReader withValueToUpdate(Object value)
    {
        if (value == _valueToUpdate) return this;
        if (value == null) {
            throw new IllegalArgumentException("cat not update null value");
        }
        JavaType t = TypeFactory.type(value.getClass());
        return new ObjectReader(this, _config, t, value);
    }    

    /*
    /**********************************************************
    /* Deserialization methods; basic ones to support ObjectCodec first
    /* (ones that take JsonParser)
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        return (T) _bind(jp);
    }

    public JsonNode readTree(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        return _bindAsTree(jp);
    }
    
    /*
    /**********************************************************
    /* Deserialization methods; others similar to what ObjectMapper has
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(String src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int length)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src, offset, length));
    }
    
    @SuppressWarnings("unchecked")
    public <T> T readValue(File src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
    }

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(_jsonFactory.createJsonParser(src));
    }

    /**
     * Convenience method for converting results from given JSON tree into given
     * value type. Basically short-cut for:
     *<pre>
     *   objectReader.readValue(src.traverse())
     *</pre>
     *
     * @since 1.6
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonNode src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(src.traverse());
    }
    
    public JsonNode readTree(InputStream in)
        throws IOException, JsonProcessingException
    {
        return _bindAndCloseAsTree(_jsonFactory.createJsonParser(in));
    }
    
    public JsonNode readTree(Reader r)
        throws IOException, JsonProcessingException
    {
        return _bindAndCloseAsTree(_jsonFactory.createJsonParser(r));
    }

    public JsonNode readTree(String content)
        throws IOException, JsonProcessingException
    {
        return _bindAndCloseAsTree(_jsonFactory.createJsonParser(content));
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _bind(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: may need to read the next token, to initialize state (either
         * before first read from parser, or after previous token has been cleared)
         */
        Object result;
        JsonToken t = _initForReading(jp);
        if (t == JsonToken.VALUE_NULL || t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = _valueToUpdate;
        } else { // pointing to event other than null
            DeserializationContext ctxt = _createDeserializationContext(jp, _config);
            if (_valueToUpdate == null) {
                result = _findRootDeserializer(_config, _valueType).deserialize(jp, ctxt);
            } else {
                _findRootDeserializer(_config, _valueType).deserialize(jp, ctxt, _valueToUpdate);
                result = _valueToUpdate;
            }
        }
        // Need to consume the token too
        jp.clearCurrentToken();
        return result;
    }
    
    protected Object _bindAndClose(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        try {
            Object result;
            JsonToken t = _initForReading(jp);
            if (t == JsonToken.VALUE_NULL || t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
                result = _valueToUpdate;
            } else {
                DeserializationContext ctxt = _createDeserializationContext(jp, _config);
                if (_valueToUpdate == null) {
                    result = _findRootDeserializer(_config, _valueType).deserialize(jp, ctxt);
                } else {
                    _findRootDeserializer(_config, _valueType).deserialize(jp, ctxt, _valueToUpdate);
                    result = _valueToUpdate;                    
                }
            }
            return result;
        } finally {
            try {
                jp.close();
            } catch (IOException ioe) { }
        }
    }

    protected JsonNode _bindAsTree(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        JsonNode result;
        JsonToken t = _initForReading(jp);
        if (t == JsonToken.VALUE_NULL || t == JsonToken.END_ARRAY || t == JsonToken.END_OBJECT) {
            result = NullNode.instance;
        } else {
            DeserializationContext ctxt = _createDeserializationContext(jp, _config);
            // Bit more complicated, since we may need to override node factory:
            result = (JsonNode) _findRootDeserializer(_config, JSON_NODE_TYPE).deserialize(jp, ctxt);
        }
        // Need to consume the token too
        jp.clearCurrentToken();
        return result;
    }
    
    protected JsonNode _bindAndCloseAsTree(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        try {
            return _bindAsTree(jp);
        } finally {
            try {
                jp.close();
            } catch (IOException ioe) { }
        }
    }
    
    protected static JsonToken _initForReading(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: must point to a token; if not pointing to one, advance.
         * This occurs before first read from JsonParser, as well as
         * after clearing of current token.
         */
        JsonToken t = jp.getCurrentToken();
        if (t == null) { // and then we must get something...
            t = jp.nextToken();
            if (t == null) { // [JACKSON-99] Should throw EOFException?
                throw new EOFException("No content to map to Object due to end of input");
            }
        }
        return t;
    }

    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected JsonDeserializer<Object> _findRootDeserializer(DeserializationConfig cfg, JavaType valueType)
        throws JsonMappingException
    {
        // First: have we already seen it?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser != null) {
            return deser;
        }

        // Nope: need to ask provider to resolve it
        deser = _provider.findTypedValueDeserializer(cfg, valueType, null);
        if (deser == null) { // can this happen?
            throw new JsonMappingException("Can not find a deserializer for type "+valueType);
        }
        _rootDeserializers.put(valueType, deser);
        return deser;
    }
    
    protected DeserializationContext _createDeserializationContext(JsonParser jp, DeserializationConfig cfg) {
        // 04-Jan-2010, tatu: we do actually need the provider too... (for polymorphic deser)
        return new StdDeserializationContext(cfg, jp, _provider);
    }    
}
