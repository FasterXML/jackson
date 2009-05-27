package org.codehaus.jackson.map.ser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.OutputProperties;
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
    final OutputProperties _outputProps;

    /**
     * If a property has serialization inclusion value of
     * {@link OutputProperties#ALL}, we need to know the default
     * value of the bean, to know if property value equals default
     * one.
     */
    protected Object _defaultBean;

    public PropertyBuilder(SerializationConfig config, BasicBeanDescription beanDesc)
    {
        _config = config;
        _beanDesc = beanDesc;
        _outputProps = beanDesc.findSerializationInclusion(config.getSerializationInclusion());
    }

    /**
     * @param ser (optional) Serializer detected for this method from
     *    annotations, if any; null if no serializer configured (and is to
     *    be located at a later stage)
     */
    protected  BeanPropertyWriter buildProperty(String name, AnnotatedMethod am,
                                                JsonSerializer<Object> ser)
    {
        AnnotationIntrospector intr = _config.getAnnotationIntrospector();
        // [JACKSON-120]: Check to see if serialization type is fixed
        Class<?> serializationType = intr.findSerializationType(am);
        if (serializationType != null) {
            // Must be a super type...
            Class<?> rt = am.getReturnType();
            if (!serializationType.isAssignableFrom(rt)) {
                throw new IllegalArgumentException("Illegal concrete-type annotation for method '"+am.getName()+"': class "+serializationType.getName()+" not a super-type of (declared) class "+rt.getName());
            }
        }
        
        // and finally, there may be per-method overrides:
        OutputProperties methodProps = intr.findSerializationInclusion(am, _outputProps);
        //Object defaultValue;
        Method m = am.getAnnotated();
        switch (methodProps) {
        case NON_DEFAULT:
            Object defValue = getDefault(name, am);
            if (defValue != null) {
                return new BeanPropertyWriter.NonDefault(name, m, ser, serializationType, defValue);
            }
            // but if it null for this property, fall through to second case:
        case NON_NULL:
            return new BeanPropertyWriter.NonNull(name, m, ser, serializationType);
        }
        // Default case is to do no filtering:
        return new BeanPropertyWriter.Std(name, m, ser, serializationType);
    }

    protected Object getDefault(String name, AnnotatedMethod am)
    {
        if (_defaultBean == null) {
            /* If we can fix access rights, we should; otherwise non-public
             * classes or default constructor will prevent instantiation
             */
            _defaultBean = _beanDesc.instantiateBean(_config.isEnabled(SerializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));
            if (_defaultBean == null) {
                Class<?> cls = _beanDesc.getClassInfo().getAnnotated();
                throw new IllegalArgumentException("Class "+cls.getName()+" has no default constructor; can not instantiate default bean value to support 'include=OutputProperties.NON_DEFAULT' annotation");
            }
        }
        Method m = am.getAnnotated();
        try {
            return m.invoke(_defaultBean);
        } catch (Exception e) {
            Throwable t = e;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof Error) throw (Error) t;
            if (t instanceof RuntimeException) throw (RuntimeException) t;
            throw new IllegalArgumentException("Failed to get property '"+name+"' of default "+_defaultBean.getClass().getName()+" instance (using method '"+m.getName()+"')");
        }
    }
}
