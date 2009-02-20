package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Base bean property handler class, which implements commong parts of
 * reflection-based functionality for accessing a property value
 * and serializing it.
 */
public abstract class BeanPropertyWriter
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

    protected BeanPropertyWriter(String name, Method acc)
    {
        _name = name;
        _accessorMethod = acc;
    }

    public abstract boolean hasSerializer();

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
    public abstract void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception;
}

