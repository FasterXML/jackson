package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

/**
 * Base bean property handler class, which implements common parts of
 * reflection-based functionality for accessing a property value
 * and serializing it.
 */
public class BeanPropertyWriter
{
    /*
    //////////////////////////////////////////////////////
    // Settings for accessing property value to serialize
    //////////////////////////////////////////////////////
     */

    /**
     * Accessor method used to get property value, for
     * method-accessible properties.
     * Null if and only if {@link #_field} is null.
     */
    protected final Method _accessorMethod;
    
    /**
     * Field that contains the property value for field-accessible
     * properties.
     * Null if and only if {@link #_accessorMethod} is null.
     */
    protected final Field _field;

    /*
    //////////////////////////////////////////////////////
    // Serialization settings
    //////////////////////////////////////////////////////
     */

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

    protected final boolean _suppressNulls;

    protected final Object _suppressableValue;

    /*
    //////////////////////////////////////////////////////
    // Construction, configuration
    //////////////////////////////////////////////////////
     */

    /**
     *
     * @param suppressableValue Value to suppress
     */
    public BeanPropertyWriter(String name, JsonSerializer<Object> ser,
                              Class<?> serType,
                              Method acc, Field f,
                              boolean suppressNulls,
                              Object suppressableValue)
    {
        _name = name;
        _serializer = ser;
        _cfgSerializationType = serType;
        _accessorMethod = acc;
        _field = f;
        _suppressNulls = suppressNulls;
        _suppressableValue = suppressableValue;
    }

    /**
     * Method that will construct and return a new writer that has
     * same properties as this writer, but uses specified serializer
     * instead of currently configured one (if any).
     */
    public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
    {
        return new BeanPropertyWriter(_name, ser, _cfgSerializationType,
                                      _accessorMethod, _field,
                                      _suppressNulls, _suppressableValue);
    }

    /*
    //////////////////////////////////////////////////////
    // Accessors
    //////////////////////////////////////////////////////
     */

    public boolean hasSerializer() { return _serializer != null; }
    
    // Needed by BeanSerializer#getSchema
    protected JsonSerializer<Object> getSerializer() {
        return _serializer;
    }

    public Class<?> getSerializationType() {
        return _cfgSerializationType;
    }

    public Class<?> getReturnType() 
    {
        if (_accessorMethod != null) {
            return _accessorMethod.getReturnType();
        }
        return _field.getType();
    }

    public final String getName() { return _name; }

    /**
     * Get the generic property type of this property writer.
     *
     * @return The property type, or null if not found.
     */
    public Type getGenericPropertyType()
    {
        if (_accessorMethod != null) {
            return _accessorMethod.getGenericReturnType();
        }
        return _field.getGenericType();
    }

    /*
    //////////////////////////////////////////////////////
    // Serialization functionality
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to access property that this bean stands for, from
     * within given bean, and to serialize it as a Json Object field
     * using appropriate serializer.
     */
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        Object value = get(bean);
        // Null handling is bit different, check that first
        if (value == null) {
            if (!_suppressNulls) {
                jgen.writeFieldName(_name);
                prov.getNullValueSerializer().serialize(value, jgen, prov);
            }
            return;
        }
        // For non-nulls, first: simple check for direct cycles
        if (value == bean) {
            _reportSelfReference(bean);
        }
        if (_suppressableValue != null && _suppressableValue.equals(value)) {
            return;
        }
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            ser = prov.findValueSerializer(value.getClass());
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
    public final Object get(Object bean) throws Exception
    {
        if (_accessorMethod != null) {
            return _accessorMethod.invoke(bean);
        }
        return _field.get(bean);
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(40);
        sb.append("property '").append(getName()).append("' (");
        if (_accessorMethod != null) {
            sb.append("via method ").append(_accessorMethod.getDeclaringClass().getName()).append("#").append(_accessorMethod.getName());
               } else {
            sb.append("field \"").append(_field.getDeclaringClass().getName()).append("#").append(_field.getName());
        }
        sb.append(')');
        return sb.toString();
    }

    protected void _reportSelfReference(Object bean)
        throws JsonMappingException
    {
        throw new JsonMappingException("Direct self-reference leading to cycle");
    }
}

