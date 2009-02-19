package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Simple container class, used to store information about a single
 * property, matching to a single accessor method, and to be serializer
 * as a field value in json output.
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
     * Serializer to use for writing out the value; if known statically.
     * Will be null when property's static type is non-final, since
     * then the dynamic type may be different.
     */
    JsonSerializer<Object> _serializer = null;

    public BeanPropertyWriter(String name, Method acc)
    {
        _name = name;
        _accessorMethod = acc;
    }

    /**
     * Method that be called to assign serializer to use for this
     * property, if it one can be reliable detected only given static
     * information: usually this is possible for final types.
     */
    public void assignSerializer(JsonSerializer<Object> ser)
    {
        if (_serializer != null) {
            throw new IllegalStateException("Already had a serializer assigned");
        }
        _serializer = ser;
    }

    public boolean hasSerializer() { return _serializer != null; }

    public Class<?> getReturnType() {
        return _accessorMethod.getReturnType();
    }

    public String getName() { return _name; }

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
        throws IOException, JsonGenerationException,
               IllegalAccessException, InvocationTargetException
    {
        Object value = _accessorMethod.invoke(bean);
        JsonSerializer<Object> ser;

        if (value == null) {
            ser = prov.getNullValueSerializer();
        } else {
            ser = _serializer;
            /* We will only have serializer if the type is final, and
             * hence known. Otherwise, we will have to dynamically detect
             * it now that we have object instance.
             */
            if (ser == null) {
                ser = prov.findValueSerializer(value.getClass());
            }
        }
        jgen.writeFieldName(_name);
        ser.serialize(value, jgen, prov);
    }
}

