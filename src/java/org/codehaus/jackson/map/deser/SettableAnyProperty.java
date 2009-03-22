package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.type.JavaType;

/**
 * Class that represents a "wildcard" set method which can be used
 * to generically set values of otherwise unmapped (aka "unknown")
 * properties read from Json content.
 *<p>
 * !!! Note: might make sense to refactor to share some code
 * with {@link SettableBeanProperty}?
 */
public final class SettableAnyProperty
{
    final Method _setter;

    final JavaType _type;

    JsonDeserializer<Object> _valueDeserializer;

    public SettableAnyProperty(JavaType type, Method setter)
    {
        _type = type;
        _setter = setter;
    }

    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    public void setValueDeserializer(JsonDeserializer<Object> deser)
    {
        if (_valueDeserializer != null) { // sanity check
            throw new IllegalStateException("Already had assigned deserializer for SettableAnyProperty");
        }
        _valueDeserializer = deser;
    }

    public JsonDeserializer<Object> getValueDeserializer() { return _valueDeserializer; }

    /**
     * @param propName Name of property (from Json input) to set
     * @param instance Bean to set property on
     * @param value Value of the property
     */
    public void set(String propName, Object instance, Object value)
        throws JsonMappingException
    {
        try {
            _setter.invoke(instance, value);
        } catch (IllegalArgumentException iae) {
            String actType = (value == null) ? "[NULL]" : value.getClass().getName();
            StringBuilder msg = new StringBuilder("Problem deserializing \"any\" property '").append(propName);
            msg.append("' of class "+getClassName()+" (expected type: ").append(_type);
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = iae.getMessage();
            if (origMsg != null) {
                msg.append(", problem: ").append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
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

    @Override public String toString() { return "[any property on class "+getClassName()+"]"; }

    String getClassName() { return _setter.getDeclaringClass().getName(); }
}
