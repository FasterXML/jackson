package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * {@link BeanPropertyWriter} sub-class used for properties where exact
 * type is known statically (via annotations, or declared type being
 * final).
 */
public final class StaticBeanPropertyWriter
    extends BeanPropertyWriter
{
    /**
     * Serializer to use for writing out the value: for this sub-class,
     * it is known statically either via annotations, or from knowing
     * that the type involved is final
     */
    final JsonSerializer<Object> _serializer;

    public StaticBeanPropertyWriter(String name, Method acc,
                                    JsonSerializer<Object> ser)
    {
        super(name, acc);
        if (ser == null) {
            throw new NullPointerException("'ser' argument can not be null");
        }
        _serializer = ser;
    }

    public StaticBeanPropertyWriter(BeanPropertyWriter src,
                                    JsonSerializer<Object> ser)
    {
        this(src._name, src._accessorMethod, ser);
    }

    public boolean hasSerializer() { return true; }

    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        Object value = _accessorMethod.invoke(bean);
        JsonSerializer<Object> ser;

        if (value == null) {
            ser = prov.getNullValueSerializer();
        } else {
            ser = _serializer;
        }
        jgen.writeFieldName(_name);
        ser.serialize(value, jgen, prov);
    }
}
