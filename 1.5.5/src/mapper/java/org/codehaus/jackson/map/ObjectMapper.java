package org.codehaus.jackson.map;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.io.SegmentedStringWriter;
import org.codehaus.jackson.map.deser.StdDeserializationContext;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;
import org.codehaus.jackson.map.introspect.BasicClassIntrospector;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdTypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.TreeTraversingParser;
import org.codehaus.jackson.node.NullNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;
import org.codehaus.jackson.util.ByteArrayBuilder;
import org.codehaus.jackson.util.TokenBuffer;

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
    /*
    /************************************************* 
    /* Helper classes, enums
    /************************************************* 
     */

    /**
     * Enumeration used with {@link ObjectMapper#enableDefaultTyping()}
     * to specify what kind of types (classes) default typing should
     * be used for. It will only be used if no explicit type information
     * is found, but this enumeration further limits subset of those
     * types.
     * 
     * @since 1.5
     */
    public enum DefaultTyping {
        /**
         * This value means that only properties that have
         * {@link java.lang.Object} as declared type (including
         * generic types without explicit type) will use default
         * typing.
         */
        JAVA_LANG_OBJECT,
        
        /**
         * Value that means that default typing will be used for
         * properties with declared type of {@link java.lang.Object}
         * or an abstract type (abstract class or interface).
         * Note that this does <b>not</b> include array types.
         */
        OBJECT_AND_NON_CONCRETE,

        /**
         * Value that means that default typing will be used for
         * all types covered by {@link #OBJECT_AND_NON_CONCRETE}
         * plus all array types for them.
         */
        NON_CONCRETE_AND_ARRAYS,
        
        /**
         * Value that means that default typing will be used for
         * all non-final types, with exception of small number of
         * "natural" types (String, Boolean, Integer, Double), which
         * can be correctly inferred from JSON; as well as for
         * all arrays of non-final types.
         */
        NON_FINAL
    }

    /**
     * Customized {@link TypeResolverBuilder} that provides
     * resolver builders based on its configuration. It is used
     * when default typing is enabled (see
     * {@link ObjectMapper#enableDefaultTyping()} for details).
     */
    public static class DefaultTypeResolverBuilder
        extends StdTypeResolverBuilder
    {
        /**
         * Definition of what types is this default typer valid for.
         */
        protected final DefaultTyping _appliesFor;

        public DefaultTypeResolverBuilder(DefaultTyping t) {
            _appliesFor = t;
        }

        @Override
        public TypeDeserializer buildTypeDeserializer(JavaType baseType,
                Collection<NamedType> subtypes)
        {
            return useForType(baseType) ? super.buildTypeDeserializer(baseType, subtypes) : null;
        }

        @Override
        public TypeSerializer buildTypeSerializer(JavaType baseType,
                Collection<NamedType> subtypes)
        {
            return useForType(baseType) ? super.buildTypeSerializer(baseType, subtypes) : null;            
        }

        /**
         * Method called to check if the default type handler should be
         * used for given type.
         * Note: "natural types" (String, Boolean, Integer, Double) will never
         * use typing; that is both due to them being concrete and final,
         * and since actual serializers and deserializers will also ignore any
         * attempts to enforce typing.
         */
        public boolean useForType(JavaType t)
        {
            switch (_appliesFor) {
            case NON_CONCRETE_AND_ARRAYS:
                if (t.isArrayType()) {
                    t = t.getContentType();
                }
                // fall through
            case OBJECT_AND_NON_CONCRETE:
                return (t.getRawClass() == Object.class) || !t.isConcrete();
            case NON_FINAL:
                if (t.isArrayType()) {
                    t = t.getContentType();
                }
                return !t.isFinal(); // includes Object.class
            default:
            //case JAVA_LANG_OBJECT:
                return (t.getRawClass() == Object.class);
            }
        }
    }

    /*
    /************************************************* 
    /* Internal constants, singletons
    /************************************************* 
     */
    
    private final static JavaType JSON_NODE_TYPE = TypeFactory.type(JsonNode.class);

    /* !!! 03-Apr-2009, tatu: Should try to avoid direct reference... but not
     *   sure what'd be simple and elegant way. So until then:
     */
    protected final static ClassIntrospector<? extends BeanDescription> DEFAULT_INTROSPECTOR = BasicClassIntrospector.instance;

    // 16-May-2009, tatu: Ditto ^^^
    protected final static AnnotationIntrospector DEFAULT_ANNOTATION_INTROSPECTOR = new JacksonAnnotationIntrospector();

    // @since 1.5
    protected final static VisibilityChecker<?> STD_VISIBILITY_CHECKER = VisibilityChecker.Std.defaultInstance();
    
    /*
    /************************************************* 
    /* Configuration settings, shared
    /************************************************* 
     */

    /**
     * Factory used to create {@link JsonParser} and {@link JsonGenerator}
     * instances as necessary.
     */
    protected final JsonFactory _jsonFactory;

    /**
     * Object that defines how to add type information for types that do not
     * have explicit type information settings (which are usually
     * indicated by {@link org.codehaus.jackson.annotate.JsonTypeInfo})
     * If set to null, no type information will be added unless annotations
     * are used; if set to non-null, resolver builder is used to check
     * which type serializers and deserializers are to be used (if any)
     * 
     * @since 1.5
     */
    protected TypeResolverBuilder<?> _defaultTyper;

    /**
     * Object used for determining whether specific property elements
     * (method, constructors, fields) can be auto-detected based on
     * their visibility (access modifiers). Can be changed to allow
     * different minimum visibility levels for auto-detection. Note
     * that this is the global handler; individual types (classes)
     * can further override active checker used (using
     * {@link JsonAutoDetect} annotation)
     * 
     * @since 1.5
     */
    protected VisibilityChecker<?> _visibilityChecker;
    
    /*
    /************************************************* 
    /* Configuration settings, serialization
    /************************************************* 
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
    /************************************************* 
    /* Configuration settings, deserialization
    /************************************************* 
     */

    /**
     * Configuration object that defines basic global
     * settings for the serialization process
     */
    protected DeserializationConfig _deserializationConfig;

    /**
     * Object that manages access to deserializers used for deserializing
     * JSON content into Java objects, including possible caching
     * of the deserializers. It contains a reference to
     * {@link DeserializerFactory} to use for constructing acutal deserializers.
     */
    protected DeserializerProvider _deserializerProvider;

    /*
    /************************************************* 
    /* Configuration settings, other
    /************************************************* 
     */

    /**
     * Node factory to use for creating {@link JsonNode}s
     * for tree model instances when binding JSON content
     * as JSON trees.
     *
     * @since 1.2
     */
    protected JsonNodeFactory _nodeFactory;

    /*
    /************************************************* 
    /* Caching
    /************************************************* 
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
     *<p>
     * Since version 1.5, these may are either "raw" deserializers (when
     * no type information is needed for base type), or type-wrapped
     * deserializers (if it is needed)
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _rootDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.6f, 2);

    /*
    /************************************************* 
    /* Life-cycle (construction, configuration)
    /************************************************* 
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

    /**
     * Construct mapper that uses specified {@link JsonFactory}
     * for constructing necessary {@link JsonParser}s and/or
     * {@link JsonGenerator}s.
     */
    public ObjectMapper(JsonFactory jf)
    {
        this(jf, null, null);
    }

    /**
     * Construct mapper that uses specified {@link SerializerFactory}
     * for constructing necessary serializers.
     *
     * @deprecated Use other constructors instead; note that
     *   you can just set serializer factory with {@link #setSerializerFactory}
     */
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
         * 03-Jan-2010, tatu: and obviously we also must pass 'this',
         *    to create actual linking.
         */
        _jsonFactory = (jf == null) ? new MappingJsonFactory(this) : jf;
        // visibility checker; usually default
        _visibilityChecker = STD_VISIBILITY_CHECKER;
        _serializationConfig = (sconfig != null) ? sconfig :
            new SerializationConfig(DEFAULT_INTROSPECTOR, DEFAULT_ANNOTATION_INTROSPECTOR, _visibilityChecker);
        _deserializationConfig = (dconfig != null) ? dconfig :
            new DeserializationConfig(DEFAULT_INTROSPECTOR, DEFAULT_ANNOTATION_INTROSPECTOR, _visibilityChecker);
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
     * @since 1.4
     */   
    public SerializerProvider getSerializerProvider() {
        return _serializerProvider;
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
     * @since 1.4
     */   
    public DeserializerProvider getDeserializerProvider() {
        return _deserializerProvider;
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

    /**
     * Method for accessing currently configured visibility checker;
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     * 
     * @since 1.5
     */
    public VisibilityChecker<?> getVisibilityChecker() {
    	return _visibilityChecker;
    }

    /**
     * Method for setting currently configured visibility checker;
     * object used for determining whether given property element
     * (method, field, constructor) can be auto-detected or not.
     * This default checker is used if no per-class overrides
     * are defined.
     * 
     * @since 1.5
     */    
    public void setVisibilityChecker(VisibilityChecker<?> vc) {
        _visibilityChecker = vc;
    }
    
    /*
    /************************************************* 
    /* Access to configuration settings
    /************************************************* 
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
        return _serializationConfig.createUnshared(_defaultTyper, _visibilityChecker);
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
        return _deserializationConfig.createUnshared(_defaultTyper, _visibilityChecker);
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
     * Method that can be used to get hold of {@link JsonFactory} that this
     * mapper uses if it needs to construct {@link JsonParser}s
     * and/or {@link JsonGenerator}s.
     *
     * @return {@link JsonFactory} that this mapper uses when it needs to
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
    /************************************************* 
    /* Type information configuration (1.5+)
    /************************************************* 
     */

    /**
     * Convenience method that is equivalant to calling
     *<pre>
     *  enableObjectTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE);
     *</pre>
     */
    public ObjectMapper enableDefaultTyping() {
        return enableDefaultTyping(DefaultTyping.OBJECT_AND_NON_CONCRETE);
    }

    /**
     * Convenience method that is equivalant to calling
     *<pre>
     *  enableObjectTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
     *</pre>
     */
    public ObjectMapper enableDefaultTyping(DefaultTyping dti) {
        return enableDefaultTyping(dti, JsonTypeInfo.As.WRAPPER_ARRAY);
    }

    public ObjectMapper enableDefaultTyping(DefaultTyping dti, JsonTypeInfo.As includeAs)
    {
        TypeResolverBuilder<?> typer = new DefaultTypeResolverBuilder(dti);
        // we'll always use full class name, when using defaulting
        typer = typer.init(JsonTypeInfo.Id.CLASS, null);
        typer = typer.inclusion(includeAs);
        return setDefaltTyping(typer);
    }
    
    public ObjectMapper disableDefaultTyping() {
        return setDefaltTyping(null);
    }

    public ObjectMapper setDefaltTyping(TypeResolverBuilder<?> typer) {
        _defaultTyper = typer;
        return this;
    }
    
    /*
    /************************************************* 
    /* Public API (from ObjectCodec): deserialization
    /* (mapping from Json to Java types);
    /* main methods
    /************************************************* 
     */

    /**
     * Method to deserialize JSON content into a non-container
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
        return (T) _readValue(copyDeserializationConfig(), jp, TypeFactory.type(valueType));
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
        return (T) _readValue(cfg, jp, TypeFactory.type(valueType));
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
        return (T) _readValue(copyDeserializationConfig(), jp, TypeFactory.type(valueTypeRef));
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
        return (T) _readValue(cfg, jp, TypeFactory.type(valueTypeRef));
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
        return (T) _readValue(copyDeserializationConfig(), jp, valueType);
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
        return (T) _readValue(cfg, jp, valueType);
    } 

    /**
     * Method to deserialize JSON content as tree expressed
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
     * Method to deserialize JSON content as tree expressed
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
        JsonNode n = (JsonNode) _readValue(cfg, jp, JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *
     * @param in Input stream used to read JSON content
     *   for building the JSON tree.
     *
     * @since 1.3
     */
    public JsonNode readTree(InputStream in)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) readValue(in, JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *
     * @param r Reader used to read JSON content
     *   for building the JSON tree.
     *
     * @since 1.3
     */
    public JsonNode readTree(Reader r)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) readValue(r, JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances.
     * Returns root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     *
     * @param content JSON content to parse
     *   for building the JSON tree.
     *
     * @since 1.3
     */
    public JsonNode readTree(String content)
        throws IOException, JsonProcessingException
    {
        JsonNode n = (JsonNode) readValue(content, JSON_NODE_TYPE);
        return (n == null) ? NullNode.instance : n;
    }

    /*
    /************************************************* 
    /* Public API (from ObjectCodec): serialization
    /* (mapping from Java types to Json)
    /************************************************* 
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using provided {@link JsonGenerator}.
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
     * JSON output, using provided {@link JsonGenerator},
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
     * Method to serialize given JSON Tree, using generator
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
    /************************************************* 
    /* Public API (from ObjectCodec): Tree Model support
    /************************************************* 
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
    /************************************************* 
    /* Extended Public API, accessors
    /************************************************* 
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
    /************************************************* 
    /* Extended Public API, deserialization,
    /* convenience methods
    /************************************************* 
     */

    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, Class<T> valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(File src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueTypeRef));
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
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(URL src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueTypeRef));
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
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), TypeFactory.type(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(String content, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(content), TypeFactory.type(valueTypeRef));
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
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(Reader src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueTypeRef));
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
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(InputStream src, TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src), TypeFactory.type(valueTypeRef));
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
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), TypeFactory.type(valueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len,
                           TypeReference valueTypeRef)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), TypeFactory.type(valueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T readValue(byte[] src, int offset, int len,
                           JavaType valueType)
        throws IOException, JsonParseException, JsonMappingException
    {
        return (T) _readMapAndClose(_jsonFactory.createJsonParser(src, offset, len), valueType);
    } 

    /*
    /************************************************* 
    /* Extended Public API: serialization
    /* (mapping from Java types to Json)
    /************************************************* 
     */

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, written to File provided.
     */
    public void writeValue(File resultFile, Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(resultFile, JsonEncoding.UTF8), value);
    }

    /**
     * Method that can be used to serialize any Java value as
     * JSON output, using output stream provided (using encoding
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
     * JSON output, using Writer provided.
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
    
    /**
     * Method that can be used to serialize any Java value as
     * a byte array. Functionally equivalent to calling
     * {@link #writeValue(Writer,Object)} with {@link java.io.ByteArrayOutputStream}
     * and getting bytes, but more efficient.
     * Encoding used will be UTF-8.
     *
     * @since 1.5
     */
    public byte[] writeValueAsBytes(Object value)
        throws IOException, JsonGenerationException, JsonMappingException
    {        
        ByteArrayBuilder bb = new ByteArrayBuilder(_jsonFactory._getBufferRecycler());
        _configAndWriteValue(_jsonFactory.createJsonGenerator(bb, JsonEncoding.UTF8), value);
        byte[] result = bb.toByteArray();
        bb.release();
        return result;
    }

    /*
    /*******************************************************
    /* Extended Public API: serialization using JSON Views
    /* (since version 1.4)
    /* 
    /* NOTE: as of version 1.5, should use ObjectWriter
    /* instead 
    /*******************************************************
     */

    /**
     * Method for serializing given object using specified view.
     * Note that this method is essentially just a shortcut: view to use is
     * by {@link SerializationConfig} and so this method just assigns
     * given view to a copy of default configuration of this
     * mapper. So to use other kinds of destinations, just call
     * {@link #copySerializationConfig}, call
     * {@link SerializationConfig#setSerializationView} method on it,
     * and pass that configuration to other methods.
     *
     * @param jgen Generator to use for writing JSON content
     * @param value Value to serialize
     * @param viewClass (optional) Identifier for View to use. If null,
     *   equivalent to passing <code>Object.class</code>; both of which
     *   mean "default view" (all properties always included)
     *   
     * @deprecated Use {@link #viewWriter} instead
     */
    public void writeValueUsingView(JsonGenerator jgen, Object value, Class<?> viewClass)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(jgen, value, viewClass);
    }

    /**
     * Method for serializing given object using specified view.
     * As with {@link #writeValueUsingView(JsonGenerator,Object,Class)},
     * this is a short-cut method.
     *
     * @param w Writer used for writing JSON content
     * @param value Value to serialize
     * @param viewClass (optional) Identifier for View to use. If null,
     *   equivalent to passing <code>Object.class</code>; both of which
     *   mean "default view" (all properties always included)
     *   
     * @deprecated Use {@link #viewWriter} instead
     */
    public void writeValueUsingView(Writer w, Object value, Class<?> viewClass)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(w), value, viewClass);
    }
    
    /**
     * Method for serializing given object using specified view.
     * As with {@link #writeValueUsingView(JsonGenerator,Object,Class)},
     * this is a short-cut method.
     *
     * @param out Output stream used for writing JSON content
     * @param value Value to serialize
     * @param viewClass (optional) Identifier for View to use. If null,
     *   equivalent to passing <code>Object.class</code>; both of which
     *   mean "default view" (all properties always included)
     *   
     * @deprecated Use {@link #viewWriter} instead
     */
    public void writeValueUsingView(OutputStream out, Object value, Class<?> viewClass)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        _configAndWriteValue(_jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8), value, viewClass);
    }

    /*
    /************************************************* 
    /* Extended Public API: constructing ObjectWriters
    /* for more advanced configuration
    /************************************************* 
     */

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified JSON View (filter).
     * 
     * @since 1.5
     */
    public ObjectWriter viewWriter(Class<?> serializationView) {
        return new ObjectWriter(this, serializationView, /*type*/ null);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime
     * type.
     * 
     * @since 1.5
     */
    public ObjectWriter typedWriter(Class<?> rootType) {
        JavaType t = (rootType == null) ? null : TypeFactory.type(rootType);
        return new ObjectWriter(this, null, t);
    }

    /**
     * Factory method for constructing {@link ObjectWriter} that will
     * serialize objects using specified root type, instead of actual
     * runtime type of value. Type must be a super-type of runtime
     * type.
     * 
     * @since 1.5
     */
    public ObjectWriter typedWriter(JavaType rootType) {
        return new ObjectWriter(this, null, rootType);
    }
    
    /*
    /************************************************* 
    /* Extended Public API: convenience type conversion
    /************************************************* 
     */
   
    /**
     * Convenience method for doing two-step conversion from given value, into
     * instance of given value type. This is functionality equivalent to first
     * serializing given value into JSON, then binding JSON data into value
     * of given type, but may be executed without fully serializing into
     * JSON. Same converters (serializers, deserializers) will be used as for
     * data binding, meaning same object mapper configuration works.
     *      
     * @throws IllegalArgumentException If conversion fails due to incompatible type;
     *    if so, root cause will contain underlying checked exception data binding
     *    functionality threw
     */
    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, Class<T> toValueType)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, TypeFactory.type(toValueType));
    } 

    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, TypeReference toValueTypeRef)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, TypeFactory.type(toValueTypeRef));
    } 

    @SuppressWarnings("unchecked")
    public <T> T convertValue(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        return (T) _convert(fromValue, toValueType);
    } 

    protected Object _convert(Object fromValue, JavaType toValueType)
        throws IllegalArgumentException
    {
        // sanity check for null first:
        if (fromValue == null) return null;
        /* Then use TokenBuffer, which is a JsonGenerator:
         * (see [JACKSON-175])
         */
        TokenBuffer buf = new TokenBuffer(this);
        try {
            writeValue(buf, fromValue);
            // and provide as with a JsonParser for contents as well!
            JsonParser jp = buf.asParser();
            Object result = readValue(jp, toValueType);
            jp.close();
            return result;
        } catch (IOException e) { // should not occur, no real i/o...
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }
        
    /*
    /************************************************* 
    /* Extended Public API: JSON Schema generation
    /************************************************* 
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
    /************************************************* 
    /* Internal methods, overridable
    /************************************************* 
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

    protected final void _configAndWriteValue(JsonGenerator jgen, Object value, Class<?> viewClass)
        throws IOException, JsonGenerationException, JsonMappingException
    {
        SerializationConfig cfg = copySerializationConfig();
        if (cfg.isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
            jgen.useDefaultPrettyPrinter();
        }
        cfg.setSerializationView(viewClass);
        boolean closed = false;
        try {
            _serializerProvider.serializeValue(cfg, jgen, value, _serializerFactory);
            closed = true;
            jgen.close();
        } finally {
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
    protected Object _readValue(DeserializationConfig cfg, JsonParser jp, JavaType valueType)
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
            result = _findRootDeserializer(cfg, valueType).deserialize(jp, ctxt);
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
                DeserializationConfig cfg = copyDeserializationConfig();
                DeserializationContext ctxt = _createDeserializationContext(jp, cfg);
                result = _findRootDeserializer(cfg, valueType).deserialize(jp, ctxt);
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
    /************************************************* 
    /* Internal methods, other
    /************************************************* 
     */

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
        deser = _deserializerProvider.findTypedValueDeserializer(cfg, valueType);
        if (deser == null) { // can this happen?
            throw new JsonMappingException("Can not find a deserializer for type "+valueType);
        }
        _rootDeserializers.put(valueType, deser);
        return deser;
    }

    protected DeserializationContext _createDeserializationContext(JsonParser jp, DeserializationConfig cfg)
    {
        // 04-Jan-2010, tatu: we do actually need the provider too... (for polymorphic deser)
        return new StdDeserializationContext(cfg, jp, _deserializerProvider);
    }
}
