package org.codehaus.jackson.map.ser;

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
            // !!! 25-May-2009, tatu: temporarily fall down to NON_NULL case
            //break;
        case NON_NULL:
            return new BeanPropertyWriter.NonNull(name, m, ser, serializationType);
        }
        // Default case is to do no filtering:
        return new BeanPropertyWriter.Std(name, m, ser, serializationType);
    }

}
