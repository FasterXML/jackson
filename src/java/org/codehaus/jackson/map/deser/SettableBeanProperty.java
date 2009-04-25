package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.InternCache;

/**
 * Class that represents a single settable property of a bean: contains
 * both type and name definitions, and reflection-based set functionality
 */
public final class SettableBeanProperty
{
    /**
     * Logical name of the property (often but not always derived
     * from the setter method name)
     */
    final String _propName;

    /**
     * Setter method for modifying property value; used for
     * properties other than "setterless" ones.
     */
    final Method _setter;

    /**
     * Get method for accessing property value; only used for
     * "setterless" properties.
     */
    final Method _getter;

    final JavaType _type;

    protected JsonDeserializer<Object> _valueDeserializer;

    /**
     * Value to be used when 'null' literal is encountered in Json.
     * For most types simply Java null, but for primitive types must
     * be a non-null value (like Integer.valueOf(0) for int).
     */
    protected Object _nullValue;

    /*
    ////////////////////////////////////////////////////////
    // Life-cycle (construct & configure)
    ////////////////////////////////////////////////////////
     */

    public SettableBeanProperty(String propName, JavaType type, Method setter,
                                Method getter)
    {
        /* 09-Jan-2009, tatu: Intern()ing makes sense since Jackson parsed
         *   field names are interned too, hence lookups will be faster.
         */
        _propName = InternCache.instance.intern(propName);
        _type = type;
        _setter = setter;
        _getter = getter;
    }

    public void setValueDeserializer(JsonDeserializer<Object> deser)
    {
        if (_valueDeserializer != null) { // sanity check
            throw new IllegalStateException("Already had assigned deserializer for property '"+_propName+"' (class "+_setter.getDeclaringClass().getName()+")");
        }
        _valueDeserializer = deser;
        _nullValue = _valueDeserializer.getNullValue();
    }

    /*
    ////////////////////////////////////////////////////////
    // Accessors
    ////////////////////////////////////////////////////////
     */

    public String getPropertyName() { return _propName; }
    public JavaType getType() { return _type; }

    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    /*
    ////////////////////////////////////////////////////////
    // Public API
    ////////////////////////////////////////////////////////
     */

    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate method (a setter method).
     */
    public final void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                        Object instance)
        throws IOException, JsonProcessingException
    {
        /* As per [JACKSON-88], we now have 2 ways to instantiate the
         * value...
         */
        if (_getter == null) { // regular, with-setter property
            set(instance, deserialize(jp, ctxt));
        } else { // setterless
            _deserializeSetterless(jp, ctxt, _getSetterless(instance));
        }
    }

    public final Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.nextToken();
        if (t == JsonToken.VALUE_NULL) {
            return _nullValue;
        }
        return _valueDeserializer.deserialize(jp, ctxt);
    }

    public final void set(Object instance, Object value)
        throws IOException
    {
        try {
            _setter.invoke(instance, value);
        } catch (Exception e) {
            _throwAsIOE(e, value);
        }
    }

    /*
    ////////////////////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////////////////////
     */

    private final Object _deserializeSetterless(JsonParser jp, DeserializationContext ctxt,
                                    Object withValue)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.nextToken();
        /* Hmmh. Is this a problem? We won't be setting anyting, so it's
         * equivalent of empty Collection/Map in this case
         */
        if (t == JsonToken.VALUE_NULL) {
            return _nullValue;
        }
        return _valueDeserializer.deserialize(jp, ctxt, withValue);
    }


    private final Object _getSetterless(Object instance)
        throws IOException
    {
        Object value;
        try {
            value = _getter.invoke(instance);
        } catch (Exception e) {
            _throwAsIOE(e);
            return null;
        }
        /* Note: null won't work, since we can't then inject anything
         * in. At least that's not good in common case. However,
         * theoretically the case where we get Json null might
         * be compatible. If so, implementation could be changed.
         */
        if (value == null) {
            throw new JsonMappingException("Problem deserializing 'setterless' property '"+getPropertyName()+"': get method returned null");
        }
        return value;
    }

    /**
     * Method that takes in exception of any type, and casts or wraps it
     * to an IOException or its subclass.
     */
    protected void _throwAsIOE(Exception e, Object value)
        throws IOException
    {
        if (e instanceof IllegalArgumentException) {
            String actType = (value == null) ? "[NULL]" : value.getClass().getName();
            StringBuilder msg = new StringBuilder("Problem deserializing property '").append(getPropertyName());
            msg.append("' (expected type: ").append(getType());
            msg.append("; actual type: ").append(actType).append(")");
            String origMsg = e.getMessage();
            if (origMsg != null) {
                msg.append(", problem: ").append(origMsg);
            } else {
                msg.append(" (no error message provided)");
            }
            throw new JsonMappingException(msg.toString(), null, e);
        }
        _throwAsIOE(e);
    }

    protected IOException _throwAsIOE(Exception e)
        throws IOException
    {
        if (e instanceof IOException) {
            throw (IOException) e;
        }
        if (e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        // let's wrap the innermost problem
        Throwable th = e;
        while (th.getCause() != null) {
            th = th.getCause();
        }
        throw new JsonMappingException(th.getMessage(), null, th);
    }

    @Override public String toString() { return "[property '"+_propName+"]"; }
}
