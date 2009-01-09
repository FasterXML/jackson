package org.codehaus.jackson.map.deser;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.DeserializerFactory;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.ResolvableDeserializer;
import org.codehaus.jackson.map.type.*;

/**
 * Default {@link DeserializerProvider} implementation.
 * Handles low-level caching (non-root) aspects of deserializer
 * handling; all construction details are delegated to given
 *  {@link DeserializerFactory} instance.
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
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    public StdDeserializerProvider() { }

    /*
    ////////////////////////////////////////////////////
    // Abstract methods impls
    ////////////////////////////////////////////////////
     */

    @Override
    public JsonDeserializer<Object> findValueDeserializer(JavaType type,
                                                          DeserializerFactory f)
    {
        /* A simple type? (primitive/wrapper, other well-known fundamental
         * basic types
         */
        JsonDeserializer<Object> deser = _findSimpleDeserializer(type);
        if (deser != null) {
            return deser;
        }
        // If not, maybe First: maybe we have already resolved this type?
        deser = _findCachedDeserializer(type);
        if (deser != null) {
            return deser;
        }
        // If not, need to construct.
        deser = _createDeserializer(f, type);
        if (deser == null) {
            /* Should we let caller handle it? Let's have a helper method
             * decide it; can throw an exception, or return a valid
             * deserializer
             */
            deser = _handleUnknownValueDeserializer(type, f);
        }
        /* Finally: some deserializers want to do post-processing.
         * Those types also must be added to the lookup map, to prevent
         * problems due to cyclic dependencies (which are completely
         * legal).
         */
        if (deser instanceof ResolvableDeserializer) {
            _cachedDeserializers.put(type, deser);
            _resolveDeserializer((ResolvableDeserializer)deser, f);
        } else if (type.isEnumType()) {
            // Let's also cache enum type deserializers, they somewhat costly
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
	protected JsonDeserializer<Object> _createDeserializer(DeserializerFactory f, JavaType type)
    {
        if (type.isEnumType()) {
            return (JsonDeserializer<Object>) f.createEnumDeserializer((SimpleType) type, this);
        }
        if (type instanceof ArrayType) {
            return (JsonDeserializer<Object>)f.createArrayDeserializer((ArrayType) type, this);
        }
        if (type instanceof MapType) {
            return (JsonDeserializer<Object>)f.createMapDeserializer((MapType) type, this);
        }
        if (type instanceof CollectionType) {
            return (JsonDeserializer<Object>)f.createCollectionDeserializer((CollectionType) type, this);
        }
        return (JsonDeserializer<Object>)f.createBeanDeserializer(type, this);
    }

    protected void _resolveDeserializer(ResolvableDeserializer ser, DeserializerFactory f)
    {
        ser.resolve(this, f);
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Overridable error reporting methods
    ////////////////////////////////////////////////////////////////
     */

    protected JsonDeserializer<Object> _handleUnknownValueDeserializer(JavaType type, DeserializerFactory f)
    {
        throw new IllegalArgumentException("Can not find a Value deserializer for type "+type);
    }

    protected KeyDeserializer _handleUnknownKeyDeserializer(JavaType type)
    {
        throw new IllegalArgumentException("Can not find a (Map) Key deserializer for type "+type);
    }
}
