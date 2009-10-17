package org.codehaus.jackson.map;

import java.io.*;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.SegmentedStringWriter;
import org.codehaus.jackson.map.deser.StdDeserializationContext;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;
import org.codehaus.jackson.map.introspect.BasicClassIntrospector;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.TreeTraversingParser;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

/**
 * This mapper (or, data binder, or codec) provides functionality for
 * conversting between Java objects (instances of JDK provided core classes,
 * beans), and matching JSON constructs.
 * It will use instances of {@link JsonParser} and {@link JsonGenerator}
 * for implementing actual reading/writing of JSON.
 *<p>
 * The main conversion API is defined in {@link ObjectCodec}, so that
 * implementation details of this class need not be exposed to
 * streaming parser and generator classes.
 *<p>
 * Note on caching: root-level deserializers are always cached, and accessed
 * using full (generics-aware) type information. This is different from
 * caching of referenced types, which is more limited and is done only
 * for a subset of all deserializer types. The main reason for difference
 * is that at root-level there is no incoming reference (and hence no
 * referencing property, no referral information or annotations to
 * produce differing deserializers), and that the performance impact
 * greatest at root level (since it'll essentially cache the full
 * graph of deserializers involved).
 */
public class ObjectMapper
    extends ObjectCodec
{
    final static JavaType JSON_NODE_TYPE = TypeFactory.fromClass(JsonNode.class);

    /* !!! 03-Apr-2009, tatu: Should try to avoid direct reference... but not
     *   sure what'd be simple and elegant way. So until then:
     */
    protected final static ClassIntrospector<? extends BeanDescription> DEFAULT_INTROSPECTOR = BasicClassIntrospector.instance;

    // 16-May-2009, tatu: Ditto ^^^
    protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

    /*
    ////////////////////////////////////////////////////
    // Configuration settings, shared
    ////////////////////////////////////////////////////
     */

    /**
     * Factory used to create {@link JsonParser} and {@link JsonGenerator}
     * instances as necessary.
     */
    protected final JsonFactory _jsonFactory;

    /*
    ////////////////////////////////////////////////////
    // Configuration settings, serialization
    ////////////////////////////////////////////////////
     */

    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected SerializationConfig _serializationConfig;

    /**
     * Object that manages access to serializers used for serialization,
     * including caching.
     * It is configured with {@link #_serializerFactory} to allow
     * for constructing custom serializers.
     */
    protected SerializerProvider _serializerProvider;

    /**
     * Serializer factory used for constructing serializers.
     */
    protected SerializerFactory _serializerFactory;

    /*
    ////////////////////////////////////////////////////
    // Configuration settings, deserialization
    ////////////////////////////////////////////////////
     */

    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected DeserializationConfig _deserializationConfig;

    /**
     * Object that manages access to deserializers used for deserializing
     * Json content into Java objects, including possible caching
     * of the deserializers. It contains a reference to
     * {@link DeserializerFactory} to use for constructing acutal deserializers.
     */
    protected DeserializerProvider _deserializerProvider;

    /*
    ////////////////////////////////////////////////////
    // Configuration settings, other
    ////////////////////////////////////////////////////
     */

    /**
     * Node factory to use for creating 
     *
     * @since 1.2
     */
    protected JsonNodeFactory _nodeFactory;

    /*
    ////////////////////////////////////////////////////
    // Caching
    ////////////////////////////////////////////////////
     */

    /* Note: handling of serializers and deserializers is not symmetric;
     * and as a result, only root-level deserializers can be cached here.
     * This is mostly because typing and resolution for deserializers is
     * fully static; whereas it is quite dynamic for serialization.
     */

    /**
     * We will use a separate main-level Map for keeping track
     * of root-level deserializers. This is where most succesful
     * cache lookups get resolved.
     * Map will contain resolvers for all kinds of types, including
     * container types: this is different from the component cache
     * which will only cache bean deserializers.
     *<p>
     * Given that we don't expect much concurrency for additions
     * (should very quickly converge to zero after startup), let's
     * explicitly define a low concurrency setting.
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.6f, 2);

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    /**
     * Default constructor, which will construct the default
     * {@link JsonFactory} as necessary, use
     * {@link StdSerializerProvider} as its
     * {@link SerializerProvider}, and
     * {@link BeanSerializerFactory} as its
     * {@link SerializerFactory}.
     * This means that it
     * can serialize all standard JDK types, as well as regular
     * Java Beans (based on method names and Jackson-specific annotations),
     * but does not support JAXB annotations.
     */
    public ObjectMapper()
    {
        this(null, null, null);
    }

    public ObjectMapper(JsonFactory jf)
    {
        this(jf, null, null);
    }

    public ObjectMapper(SerializerFactory sf)
    {
        this(null, null, null);
        setSerializerFactory(sf);
    }

    public ObjectMapper(JsonFactory jf,
                        SerializerProvider sp, DeserializerProvider dp)
    {
    	this(jf, sp, dp, null, null);
    }

    /**
     * 
     * @param jf JsonFactory to use: if null, a new {@link MappingJsonFactory} will be constructed
     * @param sp SerializerProvider to use: if null, a {@link StdSerializerProvider} will be constructed
     * @param dp DeserializerProvider to use: if null, a {@link StdDeserializerProvider} will be constructed
     * @param sconfig Serialization configuration to use; if null, basic {@link SerializationConfig}
     * 	will be constructed
     * @param dconfig Deserialization configuration to use; if null, basic {@link DeserializationConfig}
     * 	will be constructed
     */
    public ObjectMapper(JsonFactory jf,
                        SerializerProvider sp, DeserializerProvider dp,
                       SerializationConfig sconfig, DeserializationConfig dconfig)
    {
        /* 02-Mar-2009, tatu: Important: we MUST default to using
         *   the mapping factory, otherwise tree serialization will
         *   have problems with POJONodes.
         */
        _jsonFactory = (jf == null) ? new MappingJsonFactory() : jf;
        _serializationConfig = (sconfig == null) ? new SerializationConfig(DEFAULT_INTROSPECTOR, DEFAULT_ANNOTATION_INTROSPECTOR) : sconfig;
        _deserializationConfig = (dconfig == null) ? new DeserializationConfig(DEFAULT_INTROSPECTOR, DEFAULT_ANNOTATION_INTROSPECTOR) : dconfig;
        _serializerProvider = (sp == null) ? new StdSerializerProvider() : sp;
        _deserializerProvider = (dp == null) ? new StdDeserializerProvider() : dp;

        // Default serializer factory is stateless, can just assign
        _serializerFactory = BeanSerializerFactory.instance;

        // and use standard JsonNodeFactory initially
        _nodeFactory = JsonNodeFactory.instance;
    }

    /**
     * Method for setting specific {@link SerializerFactory} to use
     * for constructing (bean) serializers.
     */
    public ObjectMapper setSerializerFactory(SerializerFactory f) {
        _serializerFactory = f;
        return this;
    }

    /**
     * Method for setting specific {@link SerializerProvider} to use
     * for handling caching of {@link JsonSerializer} instances.
     */
    public ObjectMapper setSerializerProvider(SerializerProvider p) {
        _serializerProvider = p;
        return this;
    }

    /**
     * Method for setting specific {@link DeserializerProvider} to use
     * for handling caching of {@link JsonDeserializer} instances.
     */
    public ObjectMapper setDeserializerProvider(DeserializerProvider p) {
        _deserializerProvider = p;
        return this;
    }

    /**
     * Method for specifying {@link JsonNodeFactory} to use for
     * constructing root level tree nodes (via method
     * {@link #createObjectNode}
     *
     * @since 1.2
     */
    public ObjectMapper setNodeFactory(JsonNodeFactory f) {
        _nodeFactory = f;
        return this;
    }

    /*
    ////////////////////////////////////////////////////
    // Access to configuration settings
    ////////////////////////////////////////////////////
     */

    /**
     * Method that returns
     * the shared default {@link SerializationConfig} object
     * that defines configuration settings for serialization.
     * Returned object is "live" meaning that changes will be used
     * for future serialization operations for this mapper when using
     * mapper's default configuration
     */
    public SerializationConfig getSerializationConfig() {
        return _serializationConfig;
    }

    /**
     * Method that creates a copy of
     * the shared default {@link SerializationConfig} object
     * that defines configuration settings for serialization.
     * Since it is a copy, any changes made to the configuration
     * object will NOT directly affect serialization done using
     * basic serialization methods that use the shared object (that is,
     * ones that do not take separate {@link SerializationConfig}
     * argument.
     *<p>
     * The use case is that of changing object settings of the configuration
     * (like date format being used, see {@link SerializationConfig#setDateFormat}).
     */
    public SerializationConfig copySerializationConfig() {
        return _serializationConfig.createUnshared();
    }

    /**
     * Method for replacing the shared default serialization configuration
     * object.
     */
    public ObjectMapper setSerializationConfig(SerializationConfig cfg) {
        _serializationConfig = cfg;
        return this;
    }

    /**
     * Method for changing state of an on/off serialization feature for
     * this object mapper.
     *<p>
     * This is method is basically a shortcut method for calling
     * {@link SerializationConfig#set} on the shared {@link SerializationConfig}
     * object with given arguments.
     */
    public ObjectMapper configure(SerializationConfig.Feature f, boolean state) {
        _serializationConfig.set(f, state);
        return this;
    }

    /**
     * Method that returns
     * the shared default {@link DeserializationConfig} object
     * that defines configuration settings for deserialization.
     * Returned object is "live" meaning that changes will be used
     * for future deserialization operations for this mapper when using
     * mapper's default configuration
     */
    public DeserializationConfig getDeserializationConfig() {
        return _deserializationConfig;
    }

    /**
     * Method that creates a copy of
     * the shared default {@link DeserializationConfig} object
     * that defines configuration settings for deserialization.
     * Since it is a copy, any changes made to the configuration
     * object will NOT directly affect deserialization done using
     * basic deserialization methods that use the shared object (that is,
     * ones that do not take separate {@link DeserializationConfig}
     * argument.
     *<p>
     * The use case is that of changing object settings of the configuration
     * (like deserialization problem handler,
     * see {@link DeserializationConfig#addHandler})
     */
    public DeserializationConfig copyDeserializationConfig() {
        return _deserializationConfig.createUnshared();
    }

    /**
     * Method for replacing the shared default deserialization configuration
     * object.
     */
    public ObjectMapper setDeserializationConfig(DeserializationConfig cfg) {
        _deserializationConfig = cfg;
        return this;
    }

    /**
     * Method for changing state of an on/off deserialization feature for
     * this object mapper.
     *<p>
     * This is method is basically a shortcut method for calling
     * {@link DeserializationConfig#set} on the shared {@link DeserializationConfig}
     * object with given arguments.
     */
    public ObjectMapper configure(DeserializationConfig.Feature f, boolean state) {
        _deserializationConfig.set(f, state);
        return this;
    }

    /**
     * Method that can be used to get hold of Json factory that this
     * mapper uses if it needs to construct Json parsers and/or generators.
     *
     * @return Json factory that this mapper uses when it needs to
     *   construct Json parser and generators
     */
    public JsonFactory getJsonFactory() { return _jsonFactory; }

    /**
     * Method for changing state of an on/off {@link JsonParser} feature for
     * {@link JsonFactory} instance this object mapper uses.
     *<p>
     * This is method is basically a shortcut method for calling
     * {@link JsonFactory#setParserFeature} on the shared
     * {@link JsonFactory} this mapper uses (which is accessible
     * using {@link #getJsonFactory}).
     *
     * @since 1.2
     */
    public ObjectMapper configure(JsonParser.Feature f, boolean state) {
        _jsonFactory.configure(f, state);
        return this;
    }

    /**
     * Method for changing state of an on/off {@link JsonGenerator} feature for
     * {@link JsonFactory} instance this object mapper uses.
     *<p>
     * This is method is basically a shortcut method for calling
     * {@link JsonFactory#setGeneratorFeature} on the shared
     * {@link JsonFactory} this mapper uses (which is accessible
     * using {@link #getJsonFactory}).
     *
     * @since 1.2
     */
    public ObjectMapper configure(JsonGenerator.Feature f, boolean state) {
        _jsonFactory.configure(f, state);
        return this;
    }

    /**
     * Method that can be used to get hold of {@link JsonNodeFactory}
     * that this mapper will use when directly constructing
     * root {@link JsonNode} instances for Trees.
     *
     * @since 1.2
     */
    public JsonNodeFactory getNodeFactory() {
        return _nodeFactory;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API (from ObjectCodec): deserialization
    // (mapping from Json to Java types);
    // main methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method to deserialize Json content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(jp, TypeFactory.fromClass(valueType), copyDeserializationConfig());
    } 

    /**
     * Method to deserialize Json content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     * @since 1.1
     *
     * @param cfg Specific deserialization configuration to use for
     *   this operation. Note that not all config settings can
     *   be changed on per-operation basis: some changeds only take effect
     *   before calling the operation for the first time (for the mapper
     *   instance)
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, Class<T> valueType, 
                           DeserializationConfig cfg)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(jp, TypeFactory.fromClass(valueType), cfg);
    } 

    /**
     * Method to deserialize Json content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(jp, TypeFactory.fromTypeReference(valueTypeRef),
                              copyDeserializationConfig());
    } 

    /**
     * Method to deserialize Json content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     *
     * @param cfg Specific deserialization configuration to use for
     *   this operation. Note that not all config settings can
     *   be changed on per-operation basis: some changeds only take effect
     *   before calling the operation for the first time (for the mapper
     *   instance)
     *
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef,
                           DeserializationConfig cfg)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(jp, TypeFactory.fromTypeReference(valueTypeRef), cfg);
    } 

    /**
     * Method to deserialize Json content into a Java type, reference
     * to which is passed as argument. Type is passed using 
     * Jackson specific type; instance of which can be constructed using
     * {@link TypeFactory}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(jp, valueType, copyDeserializationConfig());
    } 

    /**
     * Method to deserialize Json content into a Java type, reference
     * to which is passed as argument. Type is passed using 
     * Jackson specific type; instance of which can be constructed using
     * {@link TypeFactory}.
     *
     * @param cfg Specific deserialization configuration to use for
     *   this operation. Note that not all config settings can
     *   be changed on per-operation basis: some changeds only take effect
     *   before calling the operation for the first time (for the mapper
     *   instance)
     *
     * @since 1.1
     */
    @SuppressWarnings("unchecked")
    public <T> T readValue(JsonParser jp, JavaType valueType,
                           DeserializationConfig cfg)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readValue(jp, valueType, cfg);
    } 

    /**
     * Method to deserialize Json content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     */
    @Override
    public JsonNode readTree(JsonParser jp)
        throws IOException, JsonProcessingException
    {
        return readTree(jp, copyDeserializationConfig());
    }

    /**
     * Method to deserialize Json content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *
     * @param cfg Specific deserialization configuration to use for
     *   this operation. Note that not all config settings can
     *   be changed on per-operation basis: some changeds only take effect
     *   before calling the operation for the first time (for the mapper
     *   instance)
     *
     * @since 1.1
     */
    public JsonNode readTree(JsonParser jp, DeserializationConfig cfg)
        throws IOException, JsonProcessingException
    {
        /* 02-Mar-2009, tatu: One twist; deserialization provider
         *   will map Json null straight into Java null. But what
         *   we want to return is the "null node" instead.
         */
        JsonNode n = (JsonNode) _readValue(jp, JSON_NODE_TYPE, cfg);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * @since 1.3
     */
    public JsonNode readTree(InputStream in)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) readValue(in, JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * @since 1.3
     */
    public JsonNode readTree(Reader r)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) readValue(r, JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * @since 1.3
     */
    public JsonNode readTree(String content)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) readValue(content, JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API (from ObjectCodec): serialization
    // (mapping from Java types to Json)
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using provided {@link JsonGenerator}.
     */
    @Override
    public void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _serializerProvider.serializeValue(copySerializationConfig(), jgen, value, _serializerFactory);
        jgen.flush();
    }

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using provided {@link JsonGenerator},
     * configured as per passed configuration object.
     *
     * @since 1.1
     */
    public void writeValue(JsonGenerator jgen, Object value, SerializationConfig config)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _serializerProvider.serializeValue(config, jgen, value, _serializerFactory);
        jgen.flush();
    }

    /**
     * Method to serialize given Json Tree, using generator
     * provided.
     */
    public void writeTree(JsonGenerator jgen, JsonNode rootNode)
        throws IOException, JsonProcessingException
    {
        _serializerProvider.serializeValue(copySerializationConfig(), jgen, rootNode, _serializerFactory);
        jgen.flush();
    }

    /**
     * Method to serialize given Json Tree, using generator
     * provided.
     *
     * @since 1.1
     */
    public void writeTree(JsonGenerator jgen, JsonNode rootNode,
                          SerializationConfig cfg)
        throws IOException, JsonProcessingException
    {
        _serializerProvider.serializeValue(cfg, jgen, rootNode, _serializerFactory);
        jgen.flush();
    }

    /*
    ////////////////////////////////////////////////////
    // Public API (from ObjectCodec): Tree Model support
    ////////////////////////////////////////////////////
     */

    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction can not refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     *
     * @since 1.2
     */
    public ObjectNode createObjectNode() {
        return _nodeFactory.objectNode();
    }

    /**
     *<p>
     * Note: return type is co-variant, as basic ObjectCodec
     * abstraction can not refer to concrete node types (as it's
     * part of core package, whereas impls are part of mapper
     * package)
     *
     * @since 1.2
     */
    public ArrayNode createArrayNode() {
        return _nodeFactory.arrayNode();
    }

    /**
     * @since 1.3
     */
    public JsonParser treeAsTokens(JsonNode n)
    {
        return new TreeTraversingParser(n, this);
    }

    /*
     * @since 1.3
     *
    public JsonGenerator treeFromTokens(JsonNode n)
    {
        if (!n.isContainerNode()) {
            throw new IllegalArgumentException("JsonNode passed in is not a container node (type "+n.getClass().getName()+")");
        }
        return new TreeAppendingGenerator((ContainerNode) n, this);
    }
    */

    public <T> T treeToValue(JsonNode n, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        JsonParser jp = treeAsTokens(n);
        return readValue(jp, valueType);
    }

    /*
    ////////////////////////////////////////////////////
    // Extended Public API, accessors
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be called to check whether mapper thinks
     * it could serialize an instance of given Class.
     * Check is done
     * by checking whether a serializer can be found for the type.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canSerialize(Class<?> type)
    {
        return _serializerProvider.hasSerializerFor(_serializationConfig, type, _serializerFactory);
    }

    /**
     * Method that can be called to check whether mapper thinks
     * it could deserialize an Object of given type.
     * Check is done
     * by checking whether a deserializer can be found for the type.
     *
     * @return True if mapper can find a serializer for instances of
     *  given class (potentially serializable), false otherwise (not
     *  serializable)
     */
    public boolean canDeserialize(JavaType type)
    {
        return _deserializerProvider.hasValueDeserializerFor(_deserializationConfig, type);
    }

    /*
    ////////////////////////////////////////////////////
    // Extended Public API, deserialization,
    // convenience methods
    ////////////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromClass(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromTypeReference(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromClass(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromTypeReference(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), TypeFactory.fromClass(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), TypeFactory.fromTypeReference(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromClass(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromTypeReference(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromClass(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.fromTypeReference(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), valueType);
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len, 
                               Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), TypeFactory.fromClass(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len,
                           TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), TypeFactory.fromTypeReference(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len,
                           JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), valueType);
    } 

    /*
    ////////////////////////////////////////////////////
    // Extended Public API: serialization
    // (mapping from Java types to Json)
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to serialize any Java value as
     * Json output, written to File provided.
     */
    public void writeValue(File resultFile, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(resultFile, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using output stream provided (using encoding
     * {@link JsonEncoding#UTF8}).
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(OutputStream out, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(Writer w, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(w), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * a String. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.StringWriter}
     * and constructing String, but more efficient.
     *
     * @since 1.3
     */
    public String writeValueAsString(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        // alas, we have to pull the recycler directly here...
        SegmentedStringWriter sw = new SegmentedStringWriter(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(sw), value);
        return sw.getAndClear();
    }

    /*
    ////////////////////////////////////////////////////
    // Extended Public API: JSON Schema generation
    ////////////////////////////////////////////////////
     */

    /**
     * Generate <a href="http://json-schema.org/">Json-schema</a>
     * instance for specified class.
     *
     * @param t The class to generate schema for
     * @return Constructed JSON schema.
     */
    public JsonSchema generateJsonSchema(Class<?> t)
            throws JsonMappingException
    {
        return generateJsonSchema(t, copySerializationConfig());
    }

    /**
     * Generate <a href="http://json-schema.org/">Json-schema</a>
     * instance for specified class, using specific
     * serialization configuration
     *
     * @param t The class to generate schema for
     * @return Constructed JSON schema.
     */
    public JsonSchema generateJsonSchema(Class<?> t, SerializationConfig cfg)
            throws JsonMappingException
    {
        return _serializerProvider.generateJsonSchema(t, cfg, _serializerFactory);
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, overridable
    ////////////////////////////////////////////////////
     */

    /**
     * Method called to configure the generator as necessary and then
     * call write functionality
     */
    protected final void _configAndWriteValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        SerializationConfig cfg = copySerializationConfig();
        // [JACKSON-96]: allow enabling pretty printing for ObjectMapper directly
        if (cfg.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
            jgen.useDefaultPrettyPrinter();
        }
        boolean closed = false;
        try {
            _serializerProvider.serializeValue(cfg, jgen, value, _serializerFactory);
            closed = true;
            jgen.close();
        } finally {
            /* won't try to close twice; also, must catch exception (so it 
             * will not mask exception that is pending)
             */
            if (!closed) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Actual implementation of value reading+binding operation.
     */
    protected Object _readValue(JsonParser jp, JavaType valueType,
                                DeserializationConfig cfg)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: may need to read the next token, to initialize
         * state (either before first read from parser, or after
         * previous token has been cleared)
         */
        Object result;
        JsonToken t = _initForReading(jp);
        if (t == JsonToken.VALUE_NULL
            || t == JsonToken.END_ARRAY
            || t == JsonToken.END_OBJECT) {
            result = null;
        } else { // pointing to event other than null
            DeserializationContext ctxt = _createDeserializationContext(jp, cfg);
            // ok, let's get the value
            result = _findRootDeserializer(valueType).deserialize(jp, ctxt);
        }
        // Need to consume the token too
        jp.clearCurrentToken();
        return result;
    }

    protected Object _readMapAndClose(JsonParser jp, JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        try {
            Object result;
            JsonToken t = _initForReading(jp);
            if (t == JsonToken.VALUE_NULL
                || t == JsonToken.END_ARRAY
                || t == JsonToken.END_OBJECT) {
                result = null;
            } else {
                DeserializationContext ctxt = _createDeserializationContext(jp, copyDeserializationConfig());
                result = _findRootDeserializer(valueType).deserialize(jp, ctxt);
            }
            // Need to consume the token too
            jp.clearCurrentToken();
            return result;
        } finally {
            try {
                jp.close();
            } catch (IOException ioe) { }
        }
    }

    /**
     * Method called to ensure that given parser is ready for reading
     * content for data binding.
     *
     * @return First token to be used for data binding after this call:
     *  can never be null as exception will be thrown if parser can not
     *  provide more tokens.
     *
     * @throws IOException if the underlying input source has problems during
     *   parsing
     * @throws JsonParseException if parser has problems parsing content
     * @throws JsonMappingException if the parser does not have any more
     *   content to map (note: Json "null" value is considered content;
     *   enf-of-stream not)
     */
    protected JsonToken _initForReading(JsonParser jp)
        throws IOException, JsonParseException, JsonMappingException
    {
        /* First: must point to a token; if not pointing to one, advance.
         * This occurs before first read from JsonParser, as well as
         * after clearing of current token.
         */
        JsonToken t = jp.getCurrentToken();
        if (t == null) {
            // and then we must get something...
            t = jp.nextToken();
            if (t == null) {
                /* [JACKSON-99] Should throw EOFException, closed thing
                 *   semantically
                 */
                throw new EOFException("No content to map to Object due to end of input");
            }
        }
        return t;
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, other
    ////////////////////////////////////////////////////
     */

    /**
     * Method called to locate deserializer for the passed root-level value.
     */
    protected JsonDeserializer<Object> _findRootDeserializer(JavaType valueType)
        throws JsonMappingException
    {
        // First: have we already seen it?
        JsonDeserializer<Object> deser = _rootDeserializers.get(valueType);
        if (deser != null) {
            return deser;
        }

        // Nope: need to ask provider to resolve it
        deser = _deserializerProvider.findValueDeserializer(_deserializationConfig, valueType, null, null);
        if (deser == null) { // can this happen?
            throw new JsonMappingException("Can not find a deserializer for type "+valueType);
        }
        _rootDeserializers.put(valueType, deser);
        return deser;
    }

    protected DeserializationContext _createDeserializationContext(JsonParser jp, DeserializationConfig cfg)
    {
        return new StdDeserializationContext(cfg, jp);
    }
}
