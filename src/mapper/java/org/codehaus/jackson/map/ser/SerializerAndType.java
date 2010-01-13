package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.TypeSerializer;

/**
 * Simple value container class used for caching combination
 * of typed and untyped serializers and/or type serializer
 *
 * @since 1.5
 */
public class SerializerAndType
{ 
    /**
     * Serializer that may be typed or untyped; if untyped,
     * there is no associated type serializer.
     */
    public final JsonSerializer<Object> serializer;

    /**
     * Type serializer stored in this entry, if any (can be null).
     * If associated
     * serializer is not null this is also the type serializer
     * that the value serializer uses.
     */
    public final TypeSerializer typeSerializer;

    public SerializerAndType(JsonSerializer<Object> ser, TypeSerializer typeSer) {
        serializer = ser;
        typeSerializer = typeSer;
    }
}
