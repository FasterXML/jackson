package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.InternCache;

/**
 * Base class for settable properties of a bean: contains
 * both type and name definitions, and reflection-based set functionality.
 * Concrete sub-classes implement details, so that both field- and
 * setter-backed properties can be handled
 */
public abstract class SettableBeanProperty
{
    /**
     * Logical name of the property (often but not always derived
     * from the setter method name)
     */
    final String _propName;

    final JavaType _type;

    protected JsonDeserializer<Object> _valueDeserializer;

    /**
     * If value will contain type information (to support
     * polymorphic handling), this is the type deserializer
     * used to handle type resolution.
     */
    protected TypeDeserializer _valueTypeDeserializer;
    
    /**
     * Value to be used when 'null' literal is encountered in Json.
     * For most types simply Java null, but for primitive types must
     * be a non-null value (like Integer.valueOf(0) for int).
     */
    protected Object _nullValue;

    /*
    /******************************************************
    /* Life-cycle (construct & configure)
    /******************************************************
     */

    protected SettableBeanProperty(String propName, JavaType type,
            TypeDeserializer typeDeser)
    {
        /* 09-Jan-2009, tatu: Intern()ing makes sense since Jackson parsed
         *   field names are (usually) interned too, hence lookups will be faster.
         */
        // 23-Oct-2009, tatu: should this be disabled wrt [JACKSON-180]?
        if (propName == null || propName.length() == 0) {
            _propName = "";
        } else {
            _propName = InternCache.instance.intern(propName);
        }
        _type = type;
        _valueTypeDeserializer = typeDeser;
    }

    public void setValueDeserializer(JsonDeserializer<Object> deser)
    {
        if (_valueDeserializer != null) { // sanity check
            throw new IllegalStateException("Already had assigned deserializer for property '"+_propName+"' (class "+getDeclaringClass().getName()+")");
        }
        _valueDeserializer = deser;
        _nullValue = _valueDeserializer.getNullValue();
    }

    protected abstract Class<?> getDeclaringClass();

    /*
    /******************************************************
    /* Accessors
    /******************************************************
     */

    public String getPropertyName() { return _propName; }
    public JavaType getType() { return _type; }

    public boolean hasValueDeserializer() { return (_valueDeserializer != null); }

    /**
     * Method to use for accessing index of the property (related to
     * other properties in the same context); currently only applicable
     * to "Creator properties".
     *<p>
     * Base implementation returns -1 to indicate that no index exists
     * for the property.
     */
    public int getCreatorIndex() { return -1; }

    /*
    /******************************************************
    /* Public API
    /******************************************************
     */

    /**
     * Method called to deserialize appropriate value, given parser (and
     * context), and set it using appropriate mechanism.
     * Pre-condition is that passed parser must point to the first token
     * that should be consumed to produce the value (the only value for
     * scalars, multiple for Objects and Arrays).
     */
    public abstract void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                           Object instance)
        throws IOException, JsonProcessingException;

    public abstract void set(Object instance, Object value)
        throws IOException;

    /**
     * This method is needed by some specialized bean deserializers,
     * and also called by some {@link #deserializeAndSet} implementations.
     *<p>
     * Pre-condition is that passed parser must point to the first token
     * that should be consumed to produce the value (the only value for
     * scalars, multiple for Objects and Arrays).
     */
    public final Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.VALUE_NULL) {
            return _nullValue;
        }
        if (_valueTypeDeserializer != null) {
            return _valueTypeDeserializer.deserializeTypedFromObject(jp, ctxt);
        }
        return _valueDeserializer.deserialize(jp, ctxt);
    }

    /*
    ////////////////////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////////////////////
     */

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
    
    @Override public String toString() { return "[property '"+_propName+"']"; }

    /*
    ////////////////////////////////////////////////////////
    // Impl classes
    ////////////////////////////////////////////////////////
     */

    /**
     * This concrete sub-class implements property that is set
     * using regular "setter" method.
     */
    public final static class MethodProperty
        extends SettableBeanProperty
    {
        /**
         * Setter method for modifying property value; used for
         * "regular" method-accessible properties.
         */
        protected final Method _setter;

        public MethodProperty(String propName, JavaType type, TypeDeserializer typeDeser,
                Method setter)
        {
            super(propName, type, typeDeser);
            _setter = setter;
        }

        protected Class<?> getDeclaringClass()
        {
            return _setter.getDeclaringClass();
        }

        public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                      Object instance)
            throws IOException, JsonProcessingException
        {
            set(instance, deserialize(jp, ctxt));
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
    }

    /**
     * This concrete sub-class implements Collection or Map property that is
     * indirectly by getting the property value and directly modifying it.
     */
    public final static class SetterlessProperty
        extends SettableBeanProperty
    {
        /**
         * Get method for accessing property value used to access property
         * (of Collection or Map type) to modify.
         */
        protected final Method _getter;

        public SetterlessProperty(String propName, JavaType type, TypeDeserializer typeDeser,
                                  Method getter)
        {
            super(propName, type, typeDeser);
            _getter = getter;
        }

        protected Class<?> getDeclaringClass()
        {
            return _getter.getDeclaringClass();
        }
        
        public final void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                            Object instance)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NULL) {
                /* Hmmh. Is this a problem? We won't be setting anything, so it's
                 * equivalent of empty Collection/Map in this case
                 */
                return;
            }

            // Ok: then, need to fetch Collection/Map to modify:
            Object toModify;
            try {
                toModify = _getter.invoke(instance);
            } catch (Exception e) {
                _throwAsIOE(e);
                return; // never gets here
            }
            /* Note: null won't work, since we can't then inject anything
             * in. At least that's not good in common case. However,
             * theoretically the case where we get Json null might
             * be compatible. If so, implementation could be changed.
             */
            if (toModify == null) {
                throw new JsonMappingException("Problem deserializing 'setterless' property '"+getPropertyName()+"': get method returned null");
            }
            _valueDeserializer.deserialize(jp, ctxt, toModify);
        }

        public final void set(Object instance, Object value)
            throws IOException
        {
            throw new UnsupportedOperationException("Should never call 'set' on setterless property");
        }
    }

    /**
     * This concrete sub-class implements property that is set
     * directly assigning to a Field.
     */
    public final static class FieldProperty
        extends SettableBeanProperty
    {
        /**
         * Actual field to set when deserializing this property.
         */
        protected final Field _field;

        public FieldProperty(String propName, JavaType type, TypeDeserializer typeDeser,
                             Field f)
        {
            super(propName, type, typeDeser);
            _field = f;
        }

        protected Class<?> getDeclaringClass()
        {
            return _field.getDeclaringClass();
        }

        public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                      Object instance)
            throws IOException, JsonProcessingException
        {
            set(instance, deserialize(jp, ctxt));
        }

        public final void set(Object instance, Object value)
            throws IOException
        {
            try {
                _field.set(instance, value);
            } catch (Exception e) {
                _throwAsIOE(e, value);
            }
        }
    }

    /**
     * This concrete sub-class implements property that is passed
     * via Creator (constructor or static factory method).
     */
    public final static class CreatorProperty
        extends SettableBeanProperty
    {
        final Class<?> _declaringClass;

        /**
         * Index of the property
         */
        final int _index;

        public CreatorProperty(String propName, JavaType type,
                               TypeDeserializer typeDeser,
                               Class<?> declaringClass, int index)
        {
            super(propName, type, typeDeser);
            _declaringClass = declaringClass;
            _index = index;
        }

        /**
         * Method to use for accessing index of the property (related to
         * other properties in the same context); currently only applicable
         * to "Creator properties".
         *<p>
         * Base implementation returns -1 to indicate that no index exists
         * for the property.
         */
        public int getCreatorIndex() { return _index; }
        
        protected Class<?> getDeclaringClass()
        {
            return _declaringClass;
        }

        public void deserializeAndSet(JsonParser jp, DeserializationContext ctxt,
                                      Object instance)
            throws IOException, JsonProcessingException
        {
            set(instance, deserialize(jp, ctxt));
        }

        public void set(Object instance, Object value)
            throws IOException
        {
            /* Hmmmh. Should we return quietly (NOP), or error?
             * For now, let's just bail out without fuss.
             */
            //throw new IllegalStateException("Method should never be called on a "+getClass().getName());
        }
    }
}
