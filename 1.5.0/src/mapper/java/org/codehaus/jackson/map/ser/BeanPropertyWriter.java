package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.type.JavaType;

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
    /*****************************************************
    /* Settings for accessing property value to serialize
    /*****************************************************
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
    /*****************************************************
    /* Serialization settings
    /*****************************************************
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
    protected final JavaType _cfgSerializationType;

    /**
     * Serializer to use for writing out the value: null if it can not
     * be known statically; non-null if it can.
     */
    protected final JsonSerializer<Object> _serializer;

    /**
     * Flag to indicate that null values for this property are not
     * to be written out. That is, if property has value null,
     * no entry will be written
     */
    protected final boolean _suppressNulls;

    /**
     * Value that is considered default value of the property; used for
     * default-value-suppression if enabled.
     */
    protected final Object _suppressableValue;

    /**
     * Alternate set of property writers used when view-based filtering
     * is available for the Bean.
     * 
     * @since 1.4
     */
    protected Class<?>[] _includeInViews;

    /**
     * If property being serialized needs type information to be
     * included this is the type serializer to use.
     * Declared type (possibly augmented with annotations) of property
     * is used for determining exact mechanism to use (compared to
     * actual runtime type used for serializing actual state).
     */
    protected TypeSerializer _typeSerializer;
    
    /**
     * Base type of the property, if the declared type is "non-trivial";
     * meaning it is either a structured type (collection, map, array),
     * or parametrized. Used to retain type information about contained
     * type, which is mostly necessary if type metadata is to be
     * included.
     *
     * @since 1.5
     */
    protected JavaType _nonTrivialBaseType;

    /*
    /*****************************************************
    /* Construction, configuration
    /*****************************************************
     */

    /**
     *
     * @param suppressableValue Value to suppress
     */
    public BeanPropertyWriter(String name, JsonSerializer<Object> ser, TypeSerializer typeSer,
                              JavaType serType,
                              Method acc, Field f,
                              boolean suppressNulls,
                              Object suppressableValue)
    {
        _name = name;
        _serializer = ser;
        _typeSerializer = typeSer;
        _cfgSerializationType = serType;
        _accessorMethod = acc;
        _field = f;
        _suppressNulls = suppressNulls;
        _suppressableValue = suppressableValue;
    }

    /**
     * "Copy constructor" to be used by filtering sub-classes
     */
    protected BeanPropertyWriter(BeanPropertyWriter base)
    {
        _name = base._name;
        _serializer = base._serializer;
        _typeSerializer = base._typeSerializer;
        _cfgSerializationType = base._cfgSerializationType;
        _accessorMethod = base._accessorMethod;
        _field = base._field;
        _suppressNulls = base._suppressNulls;
        _suppressableValue = base._suppressableValue;
    }

    /**
     * Method that will construct and return a new writer that has
     * same properties as this writer, but uses specified serializer
     * instead of currently configured one (if any).
     */
    public BeanPropertyWriter withSerializer(JsonSerializer<Object> ser)
    {
        return new BeanPropertyWriter(_name, ser, _typeSerializer, _cfgSerializationType,
                                      _accessorMethod, _field,
                                      _suppressNulls, _suppressableValue);
    }

    /**
     * Method for defining which views to included value of this
     * property in. If left undefined, will always be included;
     * otherwise active view definition will be checked against
     * definition list and value is only included if active
     * view is one of defined views, or its sub-view (as defined
     * by class/sub-class relationship).
     */
    public void setViews(Class<?>[] views) { _includeInViews = views; }

    /**
     * Method called to define type to consider as "non-trivial" basetype,
     * needed for dynamic serialization resolution for complex (usually container)
     * types
     *
     * @since 1.5
     */
    public void setNonTrivialBaseType(JavaType t) {
        _nonTrivialBaseType = t;
    }
    
    /*
    /*****************************************************
    /* Accessors
    /*****************************************************
     */

    public final String getName() { return _name; }

    public boolean hasSerializer() { return _serializer != null; }
    
    // Needed by BeanSerializer#getSchema
    protected JsonSerializer<Object> getSerializer() {
        return _serializer;
    }

    public JavaType getSerializationType() {
        return _cfgSerializationType;
    }

    public Class<?> getRawSerializationType() {
        return (_cfgSerializationType == null) ? null : _cfgSerializationType.getRawClass();
    }
    
    public Class<?> getPropertyType() 
    {
        if (_accessorMethod != null) {
            return _accessorMethod.getReturnType();
        }
        return _field.getType();
    }

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

    public Class<?>[] getViews() { return _includeInViews; }

    /*
    /*****************************************************
    /* Serialization functionality
    /*****************************************************
    */

    /**
     * Method called to access property that this bean stands for, from
     * within given bean, and to serialize it as a JSON Object field
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
            Class<?> cls = value.getClass();
            if (_nonTrivialBaseType != null) {
                JavaType t = _nonTrivialBaseType.forcedNarrowBy(cls);
                ser = prov.findValueSerializer(t);
            } else {
                ser = prov.findValueSerializer(cls);
            }
        }
        jgen.writeFieldName(_name);
        if (_typeSerializer == null) {
            ser.serialize(value, jgen, prov);
        } else {
            ser.serializeWithType(value, jgen, prov, _typeSerializer);
        }
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
