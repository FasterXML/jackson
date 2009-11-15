package org.codehaus.jackson.map.deser;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;

/**
 * Default {@link DeserializerProvider} implementation.
 * Handles low-level caching (non-root) aspects of deserializer
 * handling; all construction details are delegated to configured
 *  {@link DeserializerFactory} instance that the provider owns.
 */
public class StdDeserializerProvider
    extends DeserializerProvider
{
    private final static JavaType _typeObject = TypeFactory.type(Object.class);
    private final static JavaType _typeString = TypeFactory.type(String.class);

    /*
    ////////////////////////////////////////////////////
    // Caching
    ////////////////////////////////////////////////////
     */

    /**
     * Similarly, key deserializers are only for simple types.
     */
    final static HashMap<JavaType, KeyDeserializer> _keyDeserializers = StdKeyDeserializers.constructAll();

    /**
     * We will also cache some dynamically constructed deserializers;
     * specifically, ones that are expensive to construct.
     * This currently means bean and Enum deserializers; array, List and Map
     * deserializers will not be cached.
     *<p>
     * Given that we don't expect much concurrency for additions
     * (should very quickly converge to zero after startup), let's
     * explicitly define a low concurrency setting.
     */
    final protected ConcurrentHashMap<JavaType, JsonDeserializer<Object>> _cachedDeserializers
        = new ConcurrentHashMap<JavaType, JsonDeserializer<Object>>(64, 0.75f, 2);

    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Factory responsible for constructing actual deserializers, if not
     * one of pre-configured types.
     */
    DeserializerFactory _factory;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    /**
     * Default constructor. Equivalent to calling
     *<pre>
     *   new StdDeserializerProvider(BeanDeserializerFactory.instance);
     *</pre>
     */
    public StdDeserializerProvider() { this(BeanDeserializerFactory.instance); }

    public StdDeserializerProvider(DeserializerFactory f)
    {
        _factory = f;
    }

    /*
    ////////////////////////////////////////////////////
    // Abstract methods impls
    ////////////////////////////////////////////////////
     */

    @Override
    public JsonDeserializer<Object> findValueDeserializer(DeserializationConfig config,
                                                          JavaType type,
                                                          JavaType referrer, String refPropName)
        throws JsonMappingException
    {
        /* Maybe we have already resolved and cached this type?
         * (not true for simple(st) types actually, just beans)
         */
        JsonDeserializer<Object> deser = _findCachedDeserializer(type);
        if (deser != null) {
            return deser;
        }
        // If not, need to request factory to construct (or recycle)
        deser = _createAndCacheValueDeserializer(config, type, referrer, refPropName);
        if (deser == null) {
            /* Should we let caller handle it? Let's have a helper method
             * decide it; can throw an exception, or return a valid
             * deserializer
             */
            deser = _handleUnknownValueDeserializer(type);
        }
        return deser;
    }

    @Override
    public KeyDeserializer findKeyDeserializer(DeserializationConfig config,
                                               JavaType type)
        throws JsonMappingException
    {
        // No serializer needed if it's plain old String, or Object/untyped
        if (_typeString.equals(type) || _typeObject.equals(type)) {
            return null;
        }
        // Most other keys are of limited number of static types
        KeyDeserializer kdes = _keyDeserializers.get(type);
        if (kdes != null) {
            return kdes;
        }
        // And then other one-offs; first, Enum:
        if (type.isEnumType()) {
            return StdKeyDeserializers.constructEnumKeyDeserializer(config, type);
        }
        // One more thing: can we find ctor(String) or valueOf(String)?
        kdes = StdKeyDeserializers.findStringBasedKeyDeserializer(config, type);
        if (kdes != null) {
            return kdes;
        }

        // otherwise, will probably fail:
        return _handleUnknownKeyDeserializer(type);
    }

    /**
     * Method that can be called to find out whether a deserializer can
     * be found for given type
     */
    public boolean hasValueDeserializerFor(DeserializationConfig config,
                                           JavaType type)
    {
        /* Note: mostly copied from findValueDeserializer, except for
         * handling of unknown types
         */
        JsonDeserializer<Object> deser = _findCachedDeserializer(type);
        if (deser == null) {
            try {
                deser = _createAndCacheValueDeserializer(config, type, null, null);
            } catch (Exception e) {
                return false;
            }
        }
        return (deser != null);
    }

    public int cachedDeserializersCount() {
        return _cachedDeserializers.size();
    }

    /**
     * Method that will drop all dynamically constructed deserializers (ones that
     * are counted as result value for {@link #cachedDeserializersCount}).
     * This can be used to remove memory usage (in case some deserializers are
     * only used once or so), or to force re-construction of deserializers after
     * configuration changes for mapper than owns the provider.
     * 
     * @since 1.4
     */
    public void flushCachedDeserializers() {
        _cachedDeserializers.clear();       
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Overridable helper methods
    ////////////////////////////////////////////////////////////////
     */

    protected JsonDeserializer<Object> _findCachedDeserializer(JavaType type)
    {
        return _cachedDeserializers.get(type);
    }

    /**
     * Method that will try to create a deserializer for given type,
     * and resolve and cache it if necessary
     */
    protected JsonDeserializer<Object>_createAndCacheValueDeserializer(DeserializationConfig config, JavaType type,
                                                                       JavaType referrer, String refPropName)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser;
        try {
            deser = _createDeserializer(config, type, referrer, refPropName);
        } catch (IllegalArgumentException iae) {
            /* We better only expose checked exceptions, since those
             * are what caller is expected to handle
             */
            throw new JsonMappingException(iae.getMessage(), null, iae);
        }
        /* Finally: some deserializers want to do post-processing.
         * Those types also must be added to the lookup map, to prevent
         * problems due to cyclic dependencies (which are completely
         * legal).
         */
        if (deser != null) {
            AnnotationIntrospector aintr = config.getAnnotationIntrospector();
            // note: pass 'null' to prevent mix-ins from being used
            AnnotatedClass ac = AnnotatedClass.construct(deser.getClass(), aintr, null);
            boolean isResolvable = (deser instanceof ResolvableDeserializer);
            /* Caching? (yes for bean and enum deserializers)
             /* Also: since caching also serves to prevent infinite recursion
             * for self-referential types, we really need to cache if
             * deserializer is resolvable
             * (see [JACKSON-171] for details on why)
             */
            Boolean needToCache = isResolvable ? null : aintr.findCachability(ac);
            if (isResolvable || (needToCache != null && needToCache.booleanValue())) {
                _cachedDeserializers.put(type, deser);
            }
            /* Need to resolve? Mostly done for bean deserializers; allows
             * resolving of cyclic types, which can not yet be done during
             * construction:
             */
            if (isResolvable) {
                _resolveDeserializer(config, (ResolvableDeserializer)deser);
            }
        }
        return deser;
    }

    /* Refactored so we can isolate the casts that require suppression
     * of type-safety warnings.
     */
    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> _createDeserializer(DeserializationConfig config, 
                                                           JavaType type,
                                                           JavaType referrer, String refPropName)
        throws JsonMappingException
    {
        if (type.isEnumType()) {
            return (JsonDeserializer<Object>) _factory.createEnumDeserializer(config, type.getRawClass(), this);
        }
        if (type.isContainerType()) {
            if (type instanceof ArrayType) {
                return (JsonDeserializer<Object>)_factory.createArrayDeserializer(config, (ArrayType) type, this);
            }
            if (type instanceof MapType) {
                return (JsonDeserializer<Object>)_factory.createMapDeserializer(config, (MapType) type, this);
            }
            if (type instanceof CollectionType) {
                return (JsonDeserializer<Object>)_factory.createCollectionDeserializer(config, (CollectionType) type, this);
            }
        }

        // 02-Mar-2009, tatu: Let's consider JsonNode to be a type of its own
        Class<?> rawClass = type.getRawClass();
        if (JsonNode.class.isAssignableFrom(rawClass)) {
            Class<? extends JsonNode> nodeClass = (Class<? extends JsonNode>) rawClass;
            return (JsonDeserializer<Object>)_factory.createTreeDeserializer(config, nodeClass, this);
        }
        return (JsonDeserializer<Object>)_factory.createBeanDeserializer(config, type, this);
    }

    protected void _resolveDeserializer(DeserializationConfig config, ResolvableDeserializer ser)
        throws JsonMappingException
    {
        ser.resolve(config, this);
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Overridable error reporting methods
    ////////////////////////////////////////////////////////////////
     */

    protected JsonDeserializer<Object> _handleUnknownValueDeserializer(JavaType type)
        throws JsonMappingException
    {
        /* Let's try to figure out the reason, to give better error
         * messages
         */
        Class<?> rawClass = type.getRawClass();
        if (!ClassUtil.isConcrete(rawClass)) {
            throw new JsonMappingException("Can not find a Value deserializer for abstract type "+type);
        }
        throw new JsonMappingException("Can not find a Value deserializer for type "+type);
    }

    protected KeyDeserializer _handleUnknownKeyDeserializer(JavaType type)
        throws JsonMappingException
    {
        throw new JsonMappingException("Can not find a (Map) Key deserializer for type "+type);
    }
}
