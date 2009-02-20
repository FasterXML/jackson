package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * {@link BeanPropertyWriter} sub-class that has to dynamically determine
 * serializer to use for all given values. Constructed when full type can
 * not be statically determined.
 */
public final class DynamicBeanPropertyWriter
    extends BeanPropertyWriter
{
    public DynamicBeanPropertyWriter(String name, Method acc)
    {
        super(name, acc);
    }

    public final boolean hasSerializer() { return false; }

    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        Object value = _accessorMethod.invoke(bean);
        JsonSerializer<Object> ser;

        if (value == null) {
            ser = prov.getNullValueSerializer();
        } else {
            ser = prov.findValueSerializer(value.getClass());
        }
        jgen.writeFieldName(_name);
        ser.serialize(value, jgen, prov);
    }

}
