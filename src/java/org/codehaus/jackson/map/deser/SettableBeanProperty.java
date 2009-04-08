package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Class that represents a single settable property of a bean: contains
 * both type and name definitions, and reflection-based set functionality
 */
public final class SettableBeanProperty
{
    final String _propName;

    final Method _setter;

    final JavaType _type;

    protected JsonDeserializer<Object> _valueDeserializer;

    /**
     * Value to be used when 'null' literal is encountered in Json.
     * For most types simply Java null, but for primitive types must
     * be a non-null value (like Integer.valueOf(0) for int).
     */
    protected Object _nullValue;

    /*
    ////////////////////////////////////////////////////////
    // Life-cycle (construct & configure)
    ////////////////////////////////////////////////////////
     */

    public SettableBeanProperty(String propName, JavaType type, Method setter)
    {
        /* 09-Jan-2009, tatu: Intern()ing makes sense since Jackson parsed
         *   field names are interned too, hence lookups will be faster.
         */
        _propName = propName.intern();
        _type = type;
        _setter = setter;
    }

    public void setValueDeserializer(JsonDeserializer<Object> deser)
    {
        if (_valueDeserializer != null) { // sanity check
            throw new IllegalStateException("Already had assigned deserializer for property '"+_propName+"' (class "+_setter.getDeclaringClass().getName()+")");
        }
        _valueDeserializer = deser;
        _nullValue = _valueDeserializer.getNullValue();
    }

    /*
    ////////////////////////////////////////////////////////
    // Accessors
    ////////////////////////////////////////////////////////
     */

    public String getPropertyName() { return _propName; }
    public JavaType getType() { return _type; }

    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    /*
    ////////////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////////////
     */

    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate method (a setter method).
     */
    public final void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                        Object instance)
        throws IOException, JsonProcessingException
    {
        Object value = deserialize(jp, ctxt);
        set(instance, value);
    }

    public final Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.nextToken();
        Object value;

        if (t == JsonToken.VALUE_NULL) {
            return _nullValue;
        }
        return _valueDeserializer.deserialize(jp, ctxt);
    }

    public final void set(Object instance, Object value)
        throws JsonProcessingException
    {
        try {
            _setter.invoke(instance, value);
        } catch (IllegalArgumentException iae) {
            String actType = (value == null) ? "[NULL]" : value.getClass().getName();
            StringBuilder msg = new StringBuilder("Problem deserializing property '").append(getPropertyName());
            msg.append("' (expected type: ").append(getType());
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = iae.getMessage();
            if (origMsg != null) {
                msg.append(", problem: ").append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
            throw new JsonMappingException(msg.toString(), null, iae);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            // let's wrap the innermost problem
            Throwable th = e;
            while (th.getCause() != null) {
                th = th.getCause();
            }
            throw new JsonMappingException(th.getMessage(), null, th);
        }
    }

    @Override public String toString() { return "[property '"+_propName+"]"; }
}
