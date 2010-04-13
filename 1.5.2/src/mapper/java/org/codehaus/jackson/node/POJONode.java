package org.codehaus.jackson.node;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Value node that contains a wrapped POJO, to be serialized as
 * a Json constructed through data mapping (usually done by
 * calling {@link org.codehaus.jackson.map.ObjectMapper}).
 */
public final class POJONode
    extends ValueNode
{
    final Object _value;

    public POJONode(Object v) { _value = v; }

    /*
    ////////////////////////////////////////////////////
    // Base class overrides
    ////////////////////////////////////////////////////
     */

    @Override
    public String getValueAsText() {
        return null;
    }

    @Override public JsonToken asToken() { return JsonToken.VALUE_EMBEDDED_OBJECT; }

    @Override
    public boolean isPojo() { return true; }

    /*
    ////////////////////////////////////////////////////
    // Public API, serialization
    ////////////////////////////////////////////////////
     */

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        if (_value == null) {
            jg.writeNull();
        } else {
            jg.writeObject(_value);
        }
    }

    /*
    ////////////////////////////////////////////////////
    // Extended API
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to access the POJO this node wraps.
     */
    public Object getPojo() { return _value; }

    /*
    ////////////////////////////////////////////////////
    // Standard methods
    ////////////////////////////////////////////////////
     */

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        POJONode other = (POJONode) o;
        if (_value == null) {
            return other._value == null;
        }
        return _value.equals(other._value);
    }

    @Override
        public int hashCode() { return _value.hashCode(); }

    @Override
    public String toString()
    {
        return String.valueOf(_value);
    }
}
