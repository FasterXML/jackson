package org.codehaus.jackson.map;

import java.io.*;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.deser.StdDeserializationContext;
import org.codehaus.jackson.map.type.SimpleType;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.TreeTraversingParser;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;
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
    extends ObjectCodec
    implements Versioned
{
    private final static JavaType JSON_NODE_TYPE = SimpleType.constructUnsafe(JsonNode.class);

    /*
    /**********************************************************
    /* Immutable configuration from ObjectMapper
    /**********************************************************
     */

    /**
     * General serialization configuration settings; while immutable,
     * can use copy-constructor to create modified instances as necessary.
     */
    protected final DeserializationConfig _config;
    
    /**
     * Root-level cached deserializers
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers;
   
    protected final DeserializerProvider _provider;

    /**
     * Factory used for constructing {@link JsonGenerator}s
     */
    protected final JsonFactory _jsonFactory;
    
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

    /**
     * When using data format that uses a schema, schema is passed
     * to parser.
     * 
     * @since 1.8
     */
    protected final FormatSchema _schema;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * Constructor used by {@link ObjectMapper} for initial instantiation
     * 
     * @since 1.8
     */
    protected ObjectReader(ObjectMapper mapper, DeserializationConfig config)
    {
        this(mapper, config, null, null, null);
    }

    protected ObjectReader(ObjectMapper mapper, DeserializationConfig config,
            JavaType valueType, Object valueToUpdate, FormatSchema schema)
    {
        _config = config;
        _rootDeserializers = mapper._rootDeserializers;
        _provider = mapper._deserializerProvider;
        _jsonFactory = mapper._jsonFactory;
        _valueType = valueType;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
        _schema = schema;
    }
    
    /**
     * Copy constructor used for building variations.
     */
    protected ObjectReader(ObjectReader base, DeserializationConfig config,
            JavaType valueType, Object valueToUpdate, FormatSchema schema)
    {
        _config = config;

        _rootDeserializers = base._rootDeserializers;
        _provider = base._provider;
        _jsonFactory = base._jsonFactory;

        _valueType = valueType;
        _valueToUpdate = valueToUpdate;
        if (valueToUpdate != null && valueType.isArrayType()) {
            throw new IllegalArgumentException("Can not update an array value");
        }
        _schema = schema;
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
        return new ObjectReader(this, _config, valueType, _valueToUpdate, _schema);
    }    

    public ObjectReader withType(Class<?> valueType)
    {
        return withType(_config.constructType(valueType));
    }    

    public ObjectReader withType(java.lang.reflect.Type valueType)
    {
        return withType(_config.getTypeFactory().constructType(valueType));
    }    

    /**
     * @since 1.8
     */
    public ObjectReader withType(TypeReference<?> valueTypeRef)
    {
        return withType(_config.getTypeFactory().constructType(valueTypeRef.getType()));
    }    
    
    public ObjectReader withNodeFactory(JsonNodeFactory f)
    {
        // node factory is stored within config, so need to copy that first
        if (f == _config.getNodeFactory()) return this;
        return new ObjectReader(this, _config.withNodeFactory(f), _valueType, _valueToUpdate, _schema);
    }
    
    public ObjectReader withValueToUpdate(Object value)
    {
        if (value == _valueToUpdate) return this;
        if (value == null) {
            throw new IllegalArgumentException("cat not update null value");
        }
        JavaType t = _config.constructType(value.getClass());
        return new ObjectReader(this, _config, t, value, _schema);
    }    

    /**
     * @since 1.8
     */
    public ObjectReader withSchema(FormatSchema schema)
    {
        if (_schema == schema) {
            return this;
        }
        return new ObjectReader(this, _config, _valueType, _valueToUpdate, schema);
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

    @Override // from ObjectCodec
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
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonNode src)
        throws IOException, JsonProcessingException
    {
        return (T) _bindAndClose(treeAsTokens(src));
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
    /* Deserialization methods; reading sequence of values
    /**********************************************************
     */
    
    /**
     * Method for reading sequence of Objects from parser stream.
     * 
     * @since 1.8
     */
    public <T> MappingIterator<T> readValues(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, _findRootDeserializer(_config, _valueType));
    }
    
    /**
     * Method for reading sequence of Objects from parser stream.
     * 
     * @since 1.8
     */
    public <T> MappingIterator<T> readValues(InputStream src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, _findRootDeserializer(_config, _valueType));
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     * 
     * @since 1.8
     */
    public <T> MappingIterator<T> readValues(Reader src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, _findRootDeserializer(_config, _valueType));
    }
    
    /**
     * Method for reading sequence of Objects from parser stream.
     * 
     * @since 1.8
     */
    public <T> MappingIterator<T> readValues(String json)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(json);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, _findRootDeserializer(_config, _valueType));
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     * 
     * @since 1.8
     */
    public <T> MappingIterator<T> readValues(byte[] src, int offset, int length)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src, offset, length);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, _findRootDeserializer(_config, _valueType));
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     * 
     * @since 1.8
     */
    public <T> MappingIterator<T> readValues(File src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, _findRootDeserializer(_config, _valueType));
    }

    /**
     * Method for reading sequence of Objects from parser stream.
     * 
     * @since 1.8
     */
    public <T> MappingIterator<T> readValues(URL src)
        throws IOException, JsonProcessingException
    {
        JsonParser jp = _jsonFactory.createJsonParser(src);
        if (_schema != null) {
            jp.setSchema(_schema);
        }
        DeserializationContext ctxt = _createDeserializationContext(jp, _config);
        return new MappingIterator<T>(_valueType, jp, ctxt, _findRootDeserializer(_config, _valueType));
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
        if (_schema != null) {
            jp.setSchema(_schema);
        }
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
        if (_schema != null) {
            jp.setSchema(_schema);
        }
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

    /*
    /**********************************************************
    /* Implementation of rest of ObjectCodec methods
    /**********************************************************
     */
    
    @Override
    public JsonNode createArrayNode() {
        return _config.getNodeFactory().arrayNode();
    }

    @Override
    public JsonNode createObjectNode() {
        return _config.getNodeFactory().objectNode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(JsonParser jp, Class<T> valueType)
            throws IOException, JsonProcessingException
    {
        return (T) withType(valueType).readValue(jp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef) throws IOException, JsonProcessingException
    {            
        return (T) withType(valueTypeRef).readValue(jp);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T readValue(JsonParser jp, JavaType valueType) throws IOException, JsonProcessingException {
        return (T) withType(valueType).readValue(jp);
    }

    @Override
    public JsonParser treeAsTokens(JsonNode n) {
        return new TreeTraversingParser(n, this);
    }

    @Override
    public <T> T treeToValue(JsonNode n, Class<T> valueType)
            throws IOException, JsonProcessingException
    {
        return readValue(treeAsTokens(n), valueType);
    }

    /**
     * NOTE: NOT implemented for {@link ObjectReader}.
     */
    @Override
    public void writeTree(JsonGenerator jgen, JsonNode rootNode) throws IOException, JsonProcessingException
    {
        throw new UnsupportedOperationException("Not implemented for ObjectReader");
    }

    @Override
    public void writeValue(JsonGenerator jgen, Object value) throws IOException, JsonProcessingException
    {
        throw new UnsupportedOperationException("Not implemented for ObjectReader");
    }
}
