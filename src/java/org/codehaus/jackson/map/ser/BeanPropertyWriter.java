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
public abstract class BeanPropertyWriter
{
    /**
     * Logical name of the property; will be used as the field name
     * under which value for the property is written.
     */
    protected final String _name;

    /**
     * Accessor method used to get property value
     */
    protected final Method _accessorMethod;

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
                              Class<?> serType)
    {
        _name = name;
        _accessorMethod = acc;
        _serializer = ser;
        _cfgSerializationType = serType;
    }

    /**
     * Method that will construct and return a new writer that has
     * same properties as this writer, but uses specified serializer
     * instead of currently configured one (if any).
     */
    public abstract BeanPropertyWriter withSerializer(JsonSerializer<Object> ser);

    public final boolean hasSerializer() { return _serializer != null; }

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
    public abstract void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception;

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

    /*
    //////////////////////////////////////////////////////////////
    // Concrete subclasses
    //////////////////////////////////////////////////////////////
     */

    /**
     * Basic property writer that outputs property entry independent
     * of what value property has.
     */
    public final static class Std
        extends BeanPropertyWriter
    {
        public Std(String name, Method acc, JsonSerializer<Object> ser,
                   Class<?> serType)
        {
            super(name, acc, ser, serType);
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new Std(_name, _accessorMethod, ser, _cfgSerializationType);
        }

        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            Object value = get(bean);
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

    /**
     * Property writer that outputs all property if and only if it
     * has non-null value.
     */
    public final static class NonNull
        extends BeanPropertyWriter
    {
        public NonNull(String name, Method acc, JsonSerializer<Object> ser,
                       Class<?> serType)
        {
            super(name, acc, ser, serType);
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new NonNull(_name, _accessorMethod, ser, _cfgSerializationType);
        }

        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            Object value = get(bean);
            if (value != null) { // only non-null entries to be written
                JsonSerializer<Object> ser = _serializer;
                if (ser == null) {
                    ser = prov.findValueSerializer(value.getClass());
                }
                jgen.writeFieldName(_name);
                ser.serialize(value, jgen, prov);
            }
        }
    }

    /**
     * Property writer that outputs all property if and only if it
     * has non-null value.
     */
    public final static class NonDefault
        extends BeanPropertyWriter
    {
        final Object _defaultValue;

        public NonDefault(String name, Method acc, JsonSerializer<Object> ser,
                       Class<?> serType,
                       Object defaultValue)
        {
            super(name, acc, ser, serType);
            if (defaultValue == null) { // sanity check
                // null not allowed here: must construct different type
                throw new IllegalArgumentException("Can not have null default value");
            }
            _defaultValue = defaultValue;
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new NonDefault(_name, _accessorMethod, ser, _cfgSerializationType, _defaultValue);
        }

        public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
            throws Exception
        {
            Object value = get(bean);

            JsonSerializer<Object> ser;
            if (value == null) {
                ser = prov.getNullValueSerializer();
            } else {
                if (_defaultValue.equals(value)) {
                    return;
                }
                ser = _serializer;
                if (ser == null) {
                    ser = prov.findValueSerializer(value.getClass());
                }
            }
            jgen.writeFieldName(_name);
            ser.serialize(value, jgen, prov);
        }
    }
}

