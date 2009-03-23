package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
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

    JsonDeserializer<Object> _valueDeserializer;

    public SettableBeanProperty(String propName, JavaType type, Method setter)
    {
        /* 09-Jan-2009, tatu: Intern()ing makes sense since Jackson parsed
         *   field names are interned too, hence lookups will be faster.
         */
        _propName = propName.intern();
        _type = type;
        _setter = setter;
    }

    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    public void setValueDeserializer(JsonDeserializer<Object> deser)
    {
        if (_valueDeserializer != null) { // sanity check
            throw new IllegalStateException("Already had assigned deserializer for property '"+_propName+"' (class "+_setter.getDeclaringClass().getName()+")");
        }
        _valueDeserializer = deser;
    }

    public String getPropertyName() { return _propName; }
    public JavaType getType() { return _type; }

    public JsonDeserializer<Object> getValueDeserializer() { return _valueDeserializer; }

    public void set(Object instance, Object value)
        throws JsonMappingException
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
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            throw new JsonMappingException(t.getMessage(), null, t);
        }
    }

    @Override public String toString() { return "[property '"+_propName+"]"; }
}
