package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Abstract class that defines API used by {@link ObjectMapper} (and
 * other chained {@link JsonDeserializer}s too) to deserialize Objects of
 * arbitrary types from JSON, using provided {@link JsonParser}.
 */
public abstract class JsonDeserializer<T>
{
    /**
     * Method that can be called to ask implementation to deserialize
     * json content into the value type this serializer handles.
     * Returned instance is to be constructed by method itself.
     *<p>
     * Pre-condition for this method is that the parser points to the
     * first event that is part of value to deserializer (and which 
     * is never Json 'null' literal, more on this below): for simple
     * types it may be the only value; and for structured types the
     * Object start marker.
     * Post-condition is that the parser will point to the last
     * event that is part of deserialized value (or in case deserialization
     * fails, event that was not recognized or usable, which may be
     * the same event as the one it pointed to upon call).
     *<p>
     * Note that this method is never called for JSON null literal,
     * and thus deserializers need (and should) not check for it.
     *
     * @param jp Parsed used for reading Json content
     * @param ctxt Context that can be used to access information about
     *   this deserialization activity.
     *
     * @return Deserializer value
     */
    public abstract T deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException;

    /**
     * Alternate deserialization method (compared to the most commonly
     * used, {@link #deserialize(JsonParser, DeserializationContext)}),
     * which takes in initialized value instance, which is to be
     * configured and/or populated by deserializer. Method is only
     * used for subset of all supported types; most commonly just
     * for Collections and Maps, but potentially also for beans/POJOs.
     *<p>
     * Default implementation just throws
     * {@link UnsupportedOperationException}, to indicate that types
     * that do not explicitly add support do not expect to get the call.
     */
    public T deserialize(JsonParser jp, DeserializationContext ctxt,
                                  T intoValue)
        throws IOException, JsonProcessingException
    {
        throw new UnsupportedOperationException();
    }

    /**
     * Method that can be called to determine value to be used for
     * representing null values (values deserialized when Json token
     * is {@link JsonToken#VALUE_NULL}). Usually this is simply
     * Java null, but for some types (primitives) it may be
     * necessary to use actual values.
     *<p>
     * Note that deserializers are allowed to call this just once and
     * then reuse returned value; that is, method is not guaranteed to
     * be called once for each conversion.
     *<p>
     * Default implementation simply returns null.
     */
    public T getNullValue() { return null; }

    /**
     * Method called to determine if this deserializer can and should be
     * cached by deserializer provider. This means that
     * deserializer must be reusable for
     * deserializing other instances of the type it was created to
     * deserialize, as well as that it is somewhat costly to construct.
     * Usually this only applies to bean type deserializers.
     *<p>
     * Default implementation returns false, which means that no
     * caching will be done by deserializer provider. Without caching
     * deserializer factory will be called every time a property of
     * matching type is encountered during construction of root-level
     * deserializers.
     *<p>
     * NOTE: this method will NOT be called when accessing "root"
     * deserializers (type passed to {@link ObjectMapper}), but
     * only when resolving dependant deserializers. As such it
     * is acceptable to return false without worrying about
     * performance impacts. Root-level caching should usually be
     * enough for all non-bean types.
     *
     * @since 0.9.9-4
     */
    public boolean shouldBeCached() { return false; }

    /*
    //////////////////////////////////////////////////////
    // Helper class(es)
    //////////////////////////////////////////////////////
     */

    /**
     * This marker class is only to be used with annotations, to
     * indicate that <b>no deserializer is configured</b>.
     *<p>
     * Specifically, this class is to be used as the marker for
     * annotation {@link org.codehaus.jackson.annotate.map.JsonDeserialize}
     * (and deprecated
     * {@link org.codehaus.jackson.annotate.JsonUseDeserializer}).
     */
    public abstract static class None
        extends JsonDeserializer<Object> { }
}
