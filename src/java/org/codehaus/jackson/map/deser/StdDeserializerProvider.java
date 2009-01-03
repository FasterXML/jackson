package org.codehaus.jackson.map.deser;

import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonDeserializerFactory;
import org.codehaus.jackson.map.JsonDeserializerProvider;
import org.codehaus.jackson.map.ResolvableDeserializer;
import org.codehaus.jackson.map.type.JavaType;

/**
 * Default {@link JsonDeserializerProvider} implementation.
 * Handles low-level caching (non-root) aspects of deserializer
 * handling; all construction details are delegated to given
 *  {@link JsonDeserializerFactory} instance.
 */
public class StdDeserializerProvider
    extends JsonDeserializerProvider
{
    /*
    ////////////////////////////////////////////////////
    // Caching
    ////////////////////////////////////////////////////
     */

    /**
     * We will cache some deserializers; specifically, ones that
     * are expensive to construct. This currently means only bean
     * deserializers.
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
                                                          JsonDeserializerFactory f)
    {
        // First: maybe we have already resolved this type?
        JsonDeserializer<Object> ser = _cachedDeserializers.get(type);
        if (ser != null) {
            return ser;
        }
        // If not, need to construct.
        ser = _createDeserializer(f, type);
        if (ser == null) { // can't? let caller deal with it
            return null;
        }
        /* Finally: some deserializers want to do post-processing.
         * Those types also must be added to the lookup map, to prevent
         * problems due to cyclic dependencies (which are completely
         * legal).
         */
        if (ser instanceof ResolvableDeserializer) {
            _cachedDeserializers.put(type, ser);
            _resolveDeserializer((ResolvableDeserializer)ser);
        }
        return ser;
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Overridable helper methods
    ////////////////////////////////////////////////////////////////
     */

    /* Refactored so we can isolate the cast that requires this
     * annotation...
     */
    protected JsonDeserializer<Object> _createDeserializer(JsonDeserializerFactory f, JavaType type)
    {
        return (JsonDeserializer<Object>)f.createDeserializer(type, this);
    }

    protected void _resolveDeserializer(ResolvableDeserializer ser)
    {
        ser.resolve(this);
    }
}
