package org.codehaus.jackson.map.ser;

import java.lang.reflect.Method;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Base bean property handler class, which implements commong parts of
 * reflection-based functionality for accessing a property value
 * and serializing it.
 */
public final class BeanPropertyWriter
{
    /**
     * Logical name of the property; will be used as the field name
     * under which value for the property is written.
     */
    final String _name;

    /**
     * Accessor method used to get property value
     */
    final Method _accessorMethod;

    /**
     * Serializer to use for writing out the value: null if it can not
     * be known statically; non-null if it can.
     */
    final JsonSerializer<Object> _serializer;

    public BeanPropertyWriter(String name, Method acc,
                              JsonSerializer<Object> ser)
    {
        _name = name;
        _accessorMethod = acc;
        _serializer = ser;
    }

    /**
     * Method that will construct and return a new writer that has
     * same properties as this writer, but uses specified serializer
     * instead of currently configured one (if any).
     */
    public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
    {
        return new BeanPropertyWriter(_name, _accessorMethod, ser);
    }

    public boolean hasSerializer() { return _serializer != null; }

    public final Class<?> getReturnType() {
        return _accessorMethod.getReturnType();
    }

    public final String getName() { return _name; }

    @Override
    public String toString() {
        return "property '"+getName()+"' (via method "+_accessorMethod.getDeclaringClass()+"#"+_accessorMethod.getName()+"))";
    }

    /**
     * Method called to access property that this bean stands for, from
     * within given bean, and to serialize it as a Json Object field
     * using appropriate serializer.
     */
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        Object value = _accessorMethod.invoke(bean);
        JsonSerializer<Object> ser;

        if (value == null) {
            ser = prov.getNullValueSerializer();
        } else {
            ser = _serializer;
            if (ser == null) {
                ser = prov.findValueSerializer(value.getClass());
            }
        }
        jgen.writeFieldName(_name);
        ser.serialize(value, jgen, prov);
    }
}

