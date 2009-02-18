package org.codehaus.jackson.map.deser;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.*;

/**
 * Default {@link DeserializerProvider} implementation.
 * Handles low-level caching (non-root) aspects of deserializer
 * handling; all construction details are delegated to configured
 *  {@link DeserializerFactory} instance that the provider owns.
 */
public class StdDeserializerProvider
    extends DeserializerProvider
{
    final static JavaType _typeObject = TypeFactory.instance.fromClass(Object.class);
    final static JavaType _typeString = TypeFactory.instance.fromClass(String.class);

    /*
    ////////////////////////////////////////////////////
    // Caching
    ////////////////////////////////////////////////////
     */

    /**
     * We will pre-create serializers for common non-structured
     * (that is things other than Collection, Map or array)
     * types. These need not go through factory.
     */
    final static HashMap<JavaType, JsonDeserializer<Object>> _simpleDeserializers = StdDeserializers.constructAll();

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
    public JsonDeserializer<Object> findValueDeserializer(JavaType type,
                                                          JavaType referrer, String refPropName)
        throws JsonMappingException
    {
        /* A simple type? (primitive/wrapper, other well-known fundamental
         * basic types
         */
        JsonDeserializer<Object> deser = _findSimpleDeserializer(type);
        if (deser != null) {
            return deser;
        }
        // If not, maybe we have already resolved this type?
        deser = _findCachedDeserializer(type);
        if (deser != null) {
            return deser;
        }
        // If not, need to construct.
        try {
            deser = _createDeserializer(type);
        } catch (IllegalArgumentException iae) {
            /* We better only expose checked exceptions, since those
             * are what caller is expected to handle
             */
            throw new JsonMappingException(iae.getMessage(), null, iae);
        }
        if (deser == null) {
            /* Should we let caller handle it? Let's have a helper method
             * decide it; can throw an exception, or return a valid
             * deserializer
             */
            deser = _handleUnknownValueDeserializer(type);
        }
        /* Finally: some deserializers want to do post-processing.
         * Those types also must be added to the lookup map, to prevent
         * problems due to cyclic dependencies (which are completely
         * legal).
         */
        if (deser instanceof ResolvableDeserializer) {
            _cachedDeserializers.put(type, deser);
            _resolveDeserializer((ResolvableDeserializer)deser);
        } else if (type.isEnumType()) {
            /* Let's also cache enum type deserializers,
             * they are somewhat costly as well.
             */
            _cachedDeserializers.put(type, deser);
        }
        return deser;
    }

    @Override
    public KeyDeserializer findKeyDeserializer(JavaType type)
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
            return StdKeyDeserializers.constructEnumKeyDeserializer(type);
        }
        // One more thing: can we find ctor(String) or valueOf(String)?
        kdes = StdKeyDeserializers.findStringBasedKeyDeserializer(type);
        if (kdes != null) {
            return kdes;
        }

        // otherwise, will probably fail:
        return _handleUnknownKeyDeserializer(type);
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Overridable helper methods
    ////////////////////////////////////////////////////////////////
     */

    protected JsonDeserializer<Object> _findSimpleDeserializer(JavaType type)
    {
        return _simpleDeserializers.get(type);
    }

    protected JsonDeserializer<Object> _findCachedDeserializer(JavaType type)
    {
        return _cachedDeserializers.get(type);
    }

    /* Refactored so we can isolate the casts that require suppression
     * of type-safety warnings.
     */
    @SuppressWarnings("unchecked")
	protected JsonDeserializer<Object> _createDeserializer(JavaType type)
        throws JsonMappingException
    {
        if (type.isEnumType()) {
            return (JsonDeserializer<Object>) _factory.createEnumDeserializer((SimpleType) type, this);
        }
        if (type instanceof ArrayType) {
            return (JsonDeserializer<Object>)_factory.createArrayDeserializer((ArrayType) type, this);
        }
        if (type instanceof MapType) {
            return (JsonDeserializer<Object>)_factory.createMapDeserializer((MapType) type, this);
        }
        if (type instanceof CollectionType) {
            return (JsonDeserializer<Object>)_factory.createCollectionDeserializer((CollectionType) type, this);
        }
        return (JsonDeserializer<Object>)_factory.createBeanDeserializer(type, this);
    }

    protected void _resolveDeserializer(ResolvableDeserializer ser)
        throws JsonMappingException
    {
        ser.resolve(this);
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Overridable error reporting methods
    ////////////////////////////////////////////////////////////////
     */

    protected JsonDeserializer<Object> _handleUnknownValueDeserializer(JavaType type)
    {
        throw new IllegalArgumentException("Can not find a Value deserializer for type "+type);
    }

    protected KeyDeserializer _handleUnknownKeyDeserializer(JavaType type)
    {
        throw new IllegalArgumentException("Can not find a (Map) Key deserializer for type "+type);
    }
}
