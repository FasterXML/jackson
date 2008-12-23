package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonDeserializerFactory;
import org.codehaus.jackson.map.JsonDeserializerProvider;
import org.codehaus.jackson.map.ResolvableDeserializer;
import org.codehaus.jackson.map.type.JavaType;

/**
 * Default {@link JsonDeserializerProvider} implementation. Handles
 * caching aspects of deserializer handling; all construction details
 * are delegated to {@link JsonDeserializerFactory} instance.
 *<p>
 * One note about implementation: the main instance constructed will
 * be so-called "blueprint" object, and will NOT be used during actual
 * deserialization. Rather, an "instance" instance is created so that
 * state can be carried along, as well as to avoid synchronization
 * during deserializer access. Because of this, if sub-classing, one
 * must override method {@link #createInstance}: if this is not done,
 * an exception will get thrown as base class verifies that the
 * instance has same class as the blueprint
 * (<code>instance.getClass() == blueprint.getClass()</code>).
 * Check is done to prevent weird bugs that would otherwise occur.
 */
public class StdDeserializerProvider
    extends JsonDeserializerProvider
{
    /*
    ////////////////////////////////////////////////////
    // Configuration, factories
    ////////////////////////////////////////////////////
     */

    final protected JsonDeserializerFactory _deserializerFactory;

    final protected DeserializerCache _deserializerCache;

    /*
    ////////////////////////////////////////////////////
    // State, for non-blueprint instances
    ////////////////////////////////////////////////////
     */

    /**
     * For fast lookups, we will have a local non-shared read-only
     * map that contains deserializers previously fetched.
     */
    protected final HashMap<JavaType, JsonDeserializer<Object>> _knownDeserializers;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////
     */

    /**
     * Constructor for creating master (or "blue-print") provider object,
     * which is only used as the template for constructing per-binding
     * instances.
     */
    public StdDeserializerProvider()
    {
        _deserializerFactory = null;
        _deserializerCache = new DeserializerCache();
        // Blueprints doesn't have access to any deserializers...
        _knownDeserializers = null;
    }

    /**
     * "Copy-constructor", used from {@link #createInstance} (or by
     * sub-classes)
     */
    protected StdDeserializerProvider(StdDeserializerProvider src,
                                      JsonDeserializerFactory f)
    {
        _deserializerFactory = f;

        _deserializerCache = src._deserializerCache;

        /* Non-blueprint instances do have a read-only map; one that doesn't need
         * synchronization for lookups.
         */
        _knownDeserializers = _deserializerCache.getReadOnlyLookupMap();
    }

    /**
     * Overridable method, used to create a non-blueprint instances from the blueprint.
     * This is needed to retain state during serialization.
     */
    protected StdDeserializerProvider createInstance(JsonDeserializerFactory jdf)
    {
        return new StdDeserializerProvider(this, jdf);
    }

    /*
    ////////////////////////////////////////////////////
    // Main entry method to be called by JavaTypeMapper
    ////////////////////////////////////////////////////
     */

    public final Object deserializeValue(JsonParser jp, JavaType type,
                                         JsonDeserializerFactory jdf)
        throws IOException, JsonParseException
    {
        if (jdf == null) {
            throw new IllegalArgumentException("Can not pass null deserializerFactory");
        }
        /* First: we need a separate instance, which will hold a copy of the
         * non-shared ("local") read-only lookup Map for fast
         * class-to-deserializer lookup
         */
        StdDeserializerProvider inst = createInstance(jdf);
        // sanity check to avoid weird errors; to ensure sub-classes do override createInstance
        if (inst.getClass() != getClass()) {
            throw new IllegalStateException("Broken deserializer provider: createInstance returned instance of type "+inst.getClass()+"; blueprint of type "+getClass());
        }
        // And then we can do actual serialization, through the instance
        return inst._deserializeValue(jp, type);
    }

    /**
     * Method called on the actual non-blueprint provider instance object, to kick off
     * the serialization.
     */
    protected final Object _deserializeValue(JsonParser jp, JavaType type)
        throws IOException, JsonParseException
    {
        return findValueDeserializer(type).deserialize(jp);
    }

    /*
    ////////////////////////////////////////////////////
    // Configuration methods
    ////////////////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////////////////
    // Abstract methods impls
    ////////////////////////////////////////////////////
     */

    @Override
    public JsonDeserializer<Object> findValueDeserializer(JavaType type)
    {
        // Fast lookup from local lookup thingy works?
        JsonDeserializer<Object> ser = _knownDeserializers.get(type);
        if (ser != null) {
            return ser;
        }
        // If not, maybe shared map already has it?
        ser = _deserializerCache.findDeserializer(type);
        if (ser != null) {
            return ser;
        }

        /* If neither, must create. So far so good: creation should be
         * safe...
         */
        ser = _createDeserializer(type);

        /* Couldn't create? What then? Error?
         */
        if (ser == null) {
            throw new IllegalStateException("No deserializer found for type "+type);
        }
        _deserializerCache.addDeserializer(type, ser);
        /* Finally: some deserializers want to do post-processing, after
         * getting registered (to handle cyclic deps).
         */
        if (ser instanceof ResolvableDeserializer) {
            _resolveDeserializer((ResolvableDeserializer)ser);
        }
        return ser;
    }

    /*
    ////////////////////////////////////////////////////////////////
    // Helper methods: can be overridden by sub-classes
    ////////////////////////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
        protected JsonDeserializer<Object> _createDeserializer(JavaType type)
    {
        return (JsonDeserializer<Object>)_deserializerFactory.createDeserializer(type);
    }

    protected void _resolveDeserializer(ResolvableDeserializer ser)
    {
        ser.resolve(this);
    }
}
