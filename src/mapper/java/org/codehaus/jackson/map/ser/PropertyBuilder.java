package org.codehaus.jackson.map.ser;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;

/**
 * Helper class for {@link BeanSerializerFactory} that is used to
 * construct {@link BeanPropertyWriter} instances. Can be sub-classed
 * to change behavior.
 */
public class PropertyBuilder
{
    final SerializationConfig _config;
    final BasicBeanDescription _beanDesc;
    final JsonSerialize.Inclusion _outputProps;

    final AnnotationIntrospector _annotationIntrospector;

    /**
     * If a property has serialization inclusion value of
     * {@link Inclusion#ALWAYS}, we need to know the default
     * value of the bean, to know if property value equals default
     * one.
     */
    protected Object _defaultBean;

    public PropertyBuilder(SerializationConfig config, BasicBeanDescription beanDesc)
    {
        _config = config;
        _beanDesc = beanDesc;
        _outputProps = beanDesc.findSerializationInclusion(config.getSerializationInclusion());
        _annotationIntrospector = _config.getAnnotationIntrospector();
    }

    /*
    //////////////////////////////////////////////////
    // Public API
    //////////////////////////////////////////////////
     */

    /**
     * Factory method for constructor a {@link BeanPropertyWriter}
     * that uses specified method as the accessors.
     *
     * @param defaultUseStaticTyping Whether default typing mode is
     *   'static' or not (if not, it's 'dynamic'); can be overridden
     *   by annotation related to property itself
     */
    public BeanPropertyWriter buildProperty(String name, JsonSerializer<Object> ser,
                                            AnnotatedMethod am,
                                            boolean defaultUseStaticTyping)
    {
        Class<?> serializationType = findSerializationType(am, defaultUseStaticTyping);

        // and finally, there may be per-method overrides:
        JsonSerialize.Inclusion methodProps = _annotationIntrospector.findSerializationInclusion(am, _outputProps);
        Method m = am.getAnnotated();
        switch (methodProps) {
        case NON_DEFAULT:
            Object defValue = getDefaultValue(name, am, getDefaultBean());
            if (defValue != null) {
                return new BeanPropertyWriter.NonDefaultMethod(name, ser, serializationType, m, defValue);
            }
            // but if it null for this property, fall through to second case:
        case NON_NULL:
            return new BeanPropertyWriter.NonNullMethod(name, ser, serializationType, m);
        }
        // Default case is to do no filtering:
        return new BeanPropertyWriter.StdMethod(name, ser, serializationType, m);
    }

    /**
     * Factory method for constructor a {@link BeanPropertyWriter}
     * that uses specified method as the accessors.
     *
     * @param defaultUseStaticTyping Whether default typing mode is
     *   'static' or not (if not, it's 'dynamic'); can be overridden
     *   by annotation related to property itself
     */
    public BeanPropertyWriter buildProperty(String name, JsonSerializer<Object> ser,
                                            AnnotatedField af,
                                            boolean defaultUseStaticTyping)
    {
        Class<?> serializationType = findSerializationType(af, defaultUseStaticTyping);
        
        // and finally, there may be per-method overrides:
        JsonSerialize.Inclusion methodProps = _annotationIntrospector.findSerializationInclusion(af, _outputProps);
        Field f = af.getAnnotated();
        switch (methodProps) {
        case NON_DEFAULT:
            Object defValue = getDefaultValue(name, af, getDefaultBean());
            if (defValue != null) {
                return new BeanPropertyWriter.NonDefaultField(name, ser, serializationType, f, defValue);
            }
            // but if it null for this property, fall through to second case:
        case NON_NULL:
            return new BeanPropertyWriter.NonNullField(name, ser, serializationType, f);
        }
        // Default case is to do no filtering:
        return new BeanPropertyWriter.StdField(name, ser, serializationType, f);
    }

    /*
    //////////////////////////////////////////////////
    // Helper methods, generic
    //////////////////////////////////////////////////
     */

    protected Class<?> findSerializationType(Annotated a,
                                             boolean useStaticTyping)
    {
        // [JACKSON-120]: Check to see if serialization type is fixed
        Class<?> serializationType = _annotationIntrospector.findSerializationType(a);
        if (serializationType != null) {
            // Must be a super type...
            Class<?> type = a.getType();
            if (!serializationType.isAssignableFrom(type)) {
                throw new IllegalArgumentException("Illegal concrete-type annotation for method '"+a.getName()+"': class "+serializationType.getName()+" not a super-type of (declared) class "+type.getName());
            }
            return serializationType;
        }
        /* [JACKSON-114]: if using static typing, declared type is known
         * to be the type...
         */
        JsonSerialize.Typing typing = _annotationIntrospector.findSerializationTyping(a);
        if (typing != null) {
            useStaticTyping = (typing == JsonSerialize.Typing.STATIC);
        }
        if (useStaticTyping) {
            return a.getType();
        }
        return null;
    }

    protected Object getDefaultBean()
    {
        if (_defaultBean == null) {
            /* If we can fix access rights, we should; otherwise non-public
             * classes or default constructor will prevent instantiation
             */
            _defaultBean = _beanDesc.instantiateBean(_config.isEnabled(SerializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));
            if (_defaultBean == null) {
                Class<?> cls = _beanDesc.getClassInfo().getAnnotated();
                throw new IllegalArgumentException("Class "+cls.getName()+" has no default constructor; can not instantiate default bean value to support 'properties=JsonSerialize.Inclusion.NON_DEFAULT' annotation");
            }
        }
        return _defaultBean;
    }

    protected Object _throwWrapped(Exception e, String propName, Object defaultBean)
    {
        Throwable t = e;
        while (t.getCause() != null) {
            t = t.getCause();
        }
        if (t instanceof Error) throw (Error) t;
        if (t instanceof RuntimeException) throw (RuntimeException) t;
        throw new IllegalArgumentException("Failed to get property '"+propName+"' of default "+defaultBean.getClass().getName()+" instance");
    }
        
    /*
    //////////////////////////////////////////////////
    // Helper methods, for method-backed properties
    //////////////////////////////////////////////////
     */

    protected Object getDefaultValue(String name, AnnotatedMethod am, Object defaultBean)
    {
        Method m = am.getAnnotated();
        try {
            return m.invoke(defaultBean);
        } catch (Exception e) {
            return _throwWrapped(e, name, defaultBean);
        }
    }

    /*
    //////////////////////////////////////////////////
    // Helper methods, field-backed properties
    //////////////////////////////////////////////////
     */

    protected Object getDefaultValue(String name, AnnotatedField af, Object defaultBean)
    {
        Field f = af.getAnnotated();
        try {
            return f.get(defaultBean);
        } catch (Exception e) {
            return _throwWrapped(e, name, defaultBean);
        }
    }
}
