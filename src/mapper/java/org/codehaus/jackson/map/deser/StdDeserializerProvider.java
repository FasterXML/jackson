package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
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
    /*
    /***************************************************
    /* Caching
    /***************************************************
     */

    /**
     * Set of available key deserializers is currently limited
     * to standard types; and all known instances are storing
     * in this map.
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

    /**
     * During deserializer construction process we may need to keep track of partially
     * completed deserializers, to resolve cyclic dependencies. This is the
     * map used for storing deserializers before they are fully complete.
     */
    final protected HashMap<JavaType, JsonDeserializer<Object>> _incompleteDeserializers
        = new HashMap<JavaType, JsonDeserializer<Object>>(8);
    
    /*
    /***************************************************
    /* Configuration
    /***************************************************
     */

    /**
     * Factory responsible for constructing actual deserializers, if not
     * one of pre-configured types.
     */
    protected DeserializerFactory _factory;

    /*
    /***************************************************
    /* Life-cycle
    /***************************************************
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
    /***************************************************
    /* Abstract methods impls
    /***************************************************
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
    public JsonDeserializer<Object> findTypedValueDeserializer(DeserializationConfig config,
            JavaType type)
        throws JsonMappingException
    {
        JsonDeserializer<Object> deser = findValueDeserializer(config, type, null, null);
        TypeDeserializer typeDeser = _factory.findTypeDeserializer(config, type);
        if (typeDeser != null) {
            return new WrappedDeserializer(typeDeser, deser);
        }
        return deser;
    }
    
    
    @Override
    public KeyDeserializer findKeyDeserializer(DeserializationConfig config,
                                               JavaType type)
        throws JsonMappingException
    {
        // No serializer needed if it's plain old String, or Object/untyped
        Class<?> raw = type.getRawClass();
        if (raw == String.class || raw == Object.class) {
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
    /***************************************************
    /* Overridable helper methods
    /***************************************************
     */

    protected JsonDeserializer<Object> _findCachedDeserializer(JavaType type)
    {
        return _cachedDeserializers.get(type);
    }

    /**
     * Method that will try to create a deserializer for given type,
     * and resolve and cache it if necessary
     */
    protected JsonDeserializer<Object>_createAndCacheValueDeserializer(DeserializationConfig config,
            JavaType type, JavaType referrer, String refPropName)
        throws JsonMappingException
    {
        /* Only one thread to construct deserializers at any given point in time;
         * limitations necessary to ensure that only completely initialized ones
         * are visible and used.
         */
        synchronized (_incompleteDeserializers) {
            // Ok, then: could it be that due to a race condition, deserializer can now be found?
            JsonDeserializer<Object> deser = _findCachedDeserializer(type);
            if (deser != null) {
                return deser;
            }
            int count = _incompleteDeserializers.size();
            // Or perhaps being resolved right now?
            if (count > 0) {
                deser = _incompleteDeserializers.get(type);
                if (deser != null) {
                    return deser;
                }
            }
            // Nope: need to create and possibly cache
            try {
                return _createAndCache2(config, type, referrer, refPropName);
            } finally {
                // also: any deserializers that have been created are complete by now
                if (count == 0 && _incompleteDeserializers.size() > 0) {
                    _incompleteDeserializers.clear();
                }
            }
        }
    }

    /**
     * Method that handles actual construction (via factory) and caching (both
     * intermediate and eventual)
     */
    protected JsonDeserializer<Object> _createAndCache2(DeserializationConfig config, JavaType type,
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
        if (deser == null) {
            return null;
        }
        boolean isResolvable = (deser instanceof ResolvableDeserializer);
        // cache resulting deserializer? always true for resolvables (beans)
        boolean addToCache = isResolvable;
        if (!addToCache) {
            AnnotationIntrospector aintr = config.getAnnotationIntrospector();
            // note: pass 'null' to prevent mix-ins from being used
            AnnotatedClass ac = AnnotatedClass.construct(deser.getClass(), aintr, null);
            Boolean cacheAnn = aintr.findCachability(ac);
            if (cacheAnn != null) {
                addToCache = cacheAnn.booleanValue();
            }
        }
        /* we will temporarily hold on to all created deserializers (to
         * handle cyclic references, and possibly reuse non-cached
         * deserializers (list, map))
         */
        _incompleteDeserializers.put(type, deser);
        /* Need to resolve? Mostly done for bean deserializers; required for
         * resolving cyclic references.
         */
        if (isResolvable) {
            _resolveDeserializer(config, (ResolvableDeserializer)deser);
        }
        if (addToCache) {
            _cachedDeserializers.put(type, deser);
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
    /***************************************************
    /* Overridable error reporting methods
    /***************************************************
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

    /*
     ***************************************************************
     *  Helper classes
     ***************************************************************
     */

    /**
     * Simple deserializer that will call configured type deserializer, passing
     * in configured data deserializer, and exposing it all as a simple
     * deserializer.
     */
    protected final static class WrappedDeserializer
        extends JsonDeserializer<Object>
    {
        final TypeDeserializer _typeDeserializer;
        final JsonDeserializer<Object> _deserializer;

        public WrappedDeserializer(TypeDeserializer typeDeser, JsonDeserializer<Object> deser)
        {
            super();
            _typeDeserializer = typeDeser;
            _deserializer = deser;
        }

        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return _deserializer.deserializeWithType(jp, ctxt, _typeDeserializer);
        }

        @Override
        public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
                throws IOException, JsonProcessingException
        {
            // should never happen? (if it can, could call on that object)
            throw new IllegalStateException("Type-wrapped deserializer's deserializeWithType should never get called");
        }
    }

}
