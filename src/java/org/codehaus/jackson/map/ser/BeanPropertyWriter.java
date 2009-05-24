package org.codehaus.jackson.map.ser;

import java.lang.reflect.Method;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.OutputProperties;

/**
 * Base bean property handler class, which implements common parts of
 * reflection-based functionality for accessing a property value
 * and serializing it.
 */
public class BeanPropertyWriter
{
    /**
     * Logical name of the property; will be used as the field name
     * under which value for the property is written.
     */
    private final String _name;

    /**
     * Accessor method used to get property value
     */
    protected final Method _accessorMethod;

    /**
     * Should we suppress outputting of some properties?
     */
    protected final OutputProperties _cfgOutputProperties;

    /**
     * Whether this property will be written out if its value is null
     * or not: if true, property is always written; if false, only
     * if its value is not null.
     */
    protected final boolean _cfgWriteIfNull;

    /**
     * Type to use for locating serializer; normally same as return
     * type of the accessor method, but may be overridden by annotations.
     */
    protected final Class<?> _cfgSerializationType;

    /**
     * Serializer to use for writing out the value: null if it can not
     * be known statically; non-null if it can.
     */
    protected final JsonSerializer<Object> _serializer;

    public BeanPropertyWriter(String name, Method acc,
                              JsonSerializer<Object> ser,
                              OutputProperties outputProps,
                              Class<?> serType)
    {
        _name = name;
        _accessorMethod = acc;
        _serializer = ser;
        _cfgOutputProperties = outputProps;
        _cfgWriteIfNull = (outputProps != OutputProperties.NON_NULL);
        _cfgSerializationType = serType;
    }

    /**
     * Method that will construct and return a new writer that has
     * same properties as this writer, but uses specified serializer
     * instead of currently configured one (if any).
     */
    public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
    {
        return new BeanPropertyWriter(_name, _accessorMethod, ser, _cfgOutputProperties, _cfgSerializationType);
    }

    public boolean hasSerializer() { return _serializer != null; }

    public final Class<?> getSerializationType() {
        return _cfgSerializationType;
    }

    public final Class<?> getReturnType() {
        return _accessorMethod.getReturnType();
    }

    public final String getName() { return _name; }

    /**
     * Method called to access property that this bean stands for, from
     * within given bean, and to serialize it as a Json Object field
     * using appropriate serializer.
     */
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        Object value = get(bean);

        JsonSerializer<Object> ser;
        if (value == null) {
            if (!_cfgWriteIfNull) {
                return;
            }
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

    /**
     * Method that can be used to access value of the property this
     * Object describes, from given bean instance.
     *<p>
     * Note: method is final as it should not need to be overridden -- rather,
     * calling method(s) ({@link #serializeAsField}) should be overridden
     * to change the behavior
     */
    public final Object get(Object bean)
        throws Exception
    {
        return _accessorMethod.invoke(bean);
    }

    @Override
    public String toString() {
        return "property '"+getName()+"' (via method "+_accessorMethod.getDeclaringClass()+"#"+_accessorMethod.getName()+"))";
    }
}

