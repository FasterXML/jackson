package org.codehaus.jackson.map.ser;

import java.lang.reflect.Field;
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
     * Type to use for locating serializer; normally same as return
     * type of the accessor method, but may be overridden by annotations.
     */
    protected final Class<?> _cfgSerializationType;

    /**
     * Serializer to use for writing out the value: null if it can not
     * be known statically; non-null if it can.
     */
    protected final JsonSerializer<Object> _serializer;

    public BeanPropertyWriter(String name, JsonSerializer<Object> ser,
                              Class<?> serType)
    {
        _name = name;
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

    public abstract Class<?> getReturnType();

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
    public abstract Object get(Object bean) throws Exception;

    /*
    //////////////////////////////////////////////////////////////
    // Intermediate classes
    //////////////////////////////////////////////////////////////
     */

    /**
     * Intermediate base class that contains functionality shared
     * between concrete method-based property writers.
     */
    abstract static class MethodBasedWriter
        extends BeanPropertyWriter
    {
        /**
         * Accessor method used to get property value
         */
        protected final Method _accessorMethod;

        protected MethodBasedWriter(String name, JsonSerializer<Object> ser,
                                    Class<?> serType,
                                    Method acc)
        {
            super(name, ser, serType);
            _accessorMethod = acc;
        }

        public final Class<?> getReturnType() {
            return _accessorMethod.getReturnType();
        }

        public final Object get(Object bean)
            throws Exception
        {
            return _accessorMethod.invoke(bean);
        }
        
        @Override
            public String toString() {
            return "property '"+getName()+"' (via method "+_accessorMethod.getDeclaringClass().getName()+"#"+_accessorMethod.getName()+"))";
        }
    }

    /**
     * Intermediate base class that contains functionality shared
     * between concrete field-based property writers.
     */
    abstract static class FieldBasedWriter
        extends BeanPropertyWriter
    {
        /**
         * Field that contains the property value
         */
        protected final Field _field;

        protected FieldBasedWriter(String name, JsonSerializer<Object> ser,
                                   Class<?> serType,
                                   Field f)
        {
            super(name, ser, serType);
            _field = f;
        }

        public final Class<?> getReturnType() {
            return _field.getType();
        }

        public final Object get(Object bean)
            throws Exception
        {
            return _field.get(bean);
        }

        @Override
            public String toString() {
            return "property '"+getName()+"' (field "+_field.getDeclaringClass().getName()+"#"+_field.getName()+"))";
        }
    }

    /*
    //////////////////////////////////////////////////////////////
    // Concrete subclasses, method based
    //////////////////////////////////////////////////////////////
     */

    /**
     * Basic property writer that outputs property entry independent
     * of what value property has.
     */
    public final static class StdMethod
        extends MethodBasedWriter
    {
        public StdMethod(String name, JsonSerializer<Object> ser,
                         Class<?> serType,
                         Method acc)
        {
            super(name, ser, serType, acc);
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new StdMethod(_name, ser, _cfgSerializationType, _accessorMethod);
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
    public final static class NonNullMethod
        extends MethodBasedWriter
    {
        public NonNullMethod(String name, JsonSerializer<Object> ser,
                             Class<?> serType,
                             Method acc)
        {
            super(name, ser, serType, acc);
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new NonNullMethod(_name, ser, _cfgSerializationType, _accessorMethod);
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
    public final static class NonDefaultMethod
        extends MethodBasedWriter
    {
        final Object _defaultValue;

        public NonDefaultMethod(String name, JsonSerializer<Object> ser,
                                Class<?> serType,
                                Method acc,
                                Object defaultValue)
        {
            super(name, ser, serType, acc);
            if (defaultValue == null) { // sanity check
                // null not allowed here: must construct different type
                throw new IllegalArgumentException("Can not have null default value");
            }
            _defaultValue = defaultValue;
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new NonDefaultMethod(_name, ser, _cfgSerializationType, _accessorMethod, _defaultValue);
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

    /*
    //////////////////////////////////////////////////////////////
    // Concrete subclasses, field based
    //////////////////////////////////////////////////////////////
     */

    /**
     * Basic property writer that outputs property entry independent
     * of what value property has.
     */
    public final static class StdField
        extends FieldBasedWriter
    {
        public StdField(String name, JsonSerializer<Object> ser,
                         Class<?> serType,
                         Field acc)
        {
            super(name, ser, serType, acc);
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new StdField(_name, ser, _cfgSerializationType, _field);
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
    public final static class NonNullField
        extends FieldBasedWriter
    {
        public NonNullField(String name, JsonSerializer<Object> ser,
                             Class<?> serType,
                             Field acc)
        {
            super(name, ser, serType, acc);
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new NonNullField(_name, ser, _cfgSerializationType, _field);
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
    public final static class NonDefaultField
        extends FieldBasedWriter
    {
        final Object _defaultValue;

        public NonDefaultField(String name, JsonSerializer<Object> ser,
                                Class<?> serType,
                                Field acc,
                                Object defaultValue)
        {
            super(name, ser, serType, acc);
            if (defaultValue == null) { // sanity check
                // null not allowed here: must construct different type
                throw new IllegalArgumentException("Can not have null default value");
            }
            _defaultValue = defaultValue;
        }

        public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
        {
            return new NonDefaultField(_name, ser, _cfgSerializationType, _field, _defaultValue);
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

