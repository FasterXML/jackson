package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;

/**
 * This interface defines the core API that some mappers expose, as
 * well as what they expect helper classes to expose.
 * Only subset of all possible output methods are included, mostly
 * because some simple/primitive types (Strings, numbers) are easy
 * to directly output using a {@link JsonGenerator} instance; and
 * for others because of limitations on overloading (List being
 * an instance of Collection, can not overload). Serializing of
 * any type not directly or indirectly supported can be serialized
 * via call to generic {@link #writeAny} method.
 *<p>
 * Note about <code>defaultSerializer</code> argument: this is meant to allow
 * specialized serializers to only handle immediate container objects,
 * but to dispatch contents to the default handler.
 */
public interface JavaTypeSerializer
{
    /**
     * Method that can be called to ask implementation to serialize
     * a given value of unknown type. Implementation should either
     * handle serialization of the value (including its members as
     * necessary, some or all of which can be dispatched to
     * <b>defaultSerializer</b> serializer) and return true; or return false
     * to indicate it does not know how to serialize the value.
     *<p>
     * Note: implementations of these methods are not required to
     * flush the underlying generator after writing output.
     *
     * @param defaultSerializer Default serializer that child serializer can
     *    call to handle contained types. It is NOT to be called for
     *    handling <b>value</b> itself, as that could lead to infinite
     *    recursion.
     */
    public boolean writeAny(JavaTypeSerializer defaultSerializer, JsonGenerator jgen, Object value)
        throws IOException, JsonParseException;

    public boolean writeValue(JavaTypeSerializer defaultSerializer, JsonGenerator jgen, Map<Object,Object> value)
        throws IOException, JsonParseException;

    public boolean writeValue(JavaTypeSerializer defaultSerializer, JsonGenerator jgen, Collection<Object> value)
        throws IOException, JsonParseException;

    public boolean writeValue(JavaTypeSerializer defaultSerializer, JsonGenerator jgen, Object[] value)
        throws IOException, JsonParseException;
}
