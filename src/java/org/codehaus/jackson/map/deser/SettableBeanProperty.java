package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;

import org.codehaus.jackson.map.JsonDeserializer;

/**
 * Value class used to set bean property values
 */
public final class SettableBeanProperty
{
    final String _propName;

    final Method _setter;

    final Class<?> _valueClass;

    JsonDeserializer<Object> _valueDeserializer;

    public SettableBeanProperty(String propName, Class<?> valueClass, Method setter)
    {
        /* 09-Jan-2009, tatu: Intern()ing makes sense since Jackson parsed
         *   field names are interned too, hence lookups will be faster.
         */
        _propName = propName.intern();
        _valueClass = valueClass;
        _setter = setter;
    }

    public void setValueDeserializer(JsonDeserializer<Object> deser)
    {
        if (_valueDeserializer != null) { // sanity check
            throw new IllegalStateException("Alread had assigned deserializer for property '"+_propName+"' (class "+_setter.getDeclaringClass().getName()+")");
        }
        _valueDeserializer = deser;
    }

    public String getPropertyName() { return _propName; }
    public Class<?> getValueClass() { return _valueClass; }
    public JsonDeserializer<Object> getValueDeserializer() { return _valueDeserializer; }

    public void set(Object instance, Object value)
    {
        try {
            _setter.invoke(instance, value);
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            // let's wrap the innermost problem
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            throw new IllegalArgumentException(t.getMessage(), t);
        }
    }

    @Override public String toString() { return "[property '"+_propName+"]"; }
}
