package org.codehaus.jackson.map.ser;

import java.util.*;

import java.lang.reflect.Method;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Factory class that can provide serializers for any regular Java beans
 * (as defined by "having at least one get method recognizable as bean
 * accessor" -- where {@link Object#getClass} does not count);
 * as well as for "standard" JDK types. Latter is achieved
 * by delegating calls to {@link BasicSerializerFactory} 
 * to find serializers both for "standard" JDK types (and in some cases,
 * sub-classes as is the case for collection classes like
 * {@link java.util.List}s and {@link java.util.Map}s) and bean (value)
 * classes.
 *<p>
 * Note about delegating calls to {@link BasicSerializerFactory}:
 * although it would be nicer to use linear delegation
 * for construction (to essentially dispatch all calls first to the
 * underlying {@link BasicSerializerFactory}; or alternatively after
 * failing to provide bean-based serializer}, there is a problem:
 * priority levels for detecting standard types are mixed. That is,
 * we want to check if a type is a bean after some of "standard" JDK
 * types, but before the rest.
 * As a result, "mixed" delegation used, and calls are NOT done using
 * regular {@link SerializerFactory} interface but rather via
 * direct calls to {@link BasicSerializerFactory}.
 *<p>
 * Finally, since all caching is handled by the serializer provider
 * (not factory), and since there is no configurability, this
 * factory is stateless. And thus a global singleton instance can
 * be used.
 */
public class BeanSerializerFactory
    extends BasicSerializerFactory
{
    /**
     * Like {@link BasicSerializerFactory}, this factory is stateless, and
     * thus a single shared global (== singleton) instance can be used
     * without thread-safety issues.
     */
    public final static BeanSerializerFactory instance = new BeanSerializerFactory();

    /*
    ////////////////////////////////////////////////////////////
    // Life cycle
    ////////////////////////////////////////////////////////////
     */

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected BeanSerializerFactory() { }

    /*
    ////////////////////////////////////////////////////////////
    // JsonSerializerFactory impl
    ////////////////////////////////////////////////////////////
     */

    /**
     * Main serializer constructor method. We will have to be careful
     * with respect to ordering of various method calls: essentially
     * we want to reliably figure out which classes are standard types,
     * and which are beans. The problem is that some bean Classes may
     * implement standard interfaces (say, {@link java.lang.Iterable}.
     *<p>
     * Note: sub-classes may choose to complete replace implementation,
     * if they want to alter priority of serializer lookups.
     */
    @Override
    @SuppressWarnings("unchecked")
        public <T> JsonSerializer<T> createSerializer(Class<T> type, SerializationConfig config)
    {
        // First, fast lookup for exact type:
        JsonSerializer<?> ser = super.findSerializerByLookup(type);
        if (ser == null) {
            // and then introspect for some safe (?) JDK types
            ser = super.findSerializerByPrimaryType(type, config);
            if (ser == null) {
                /* And this is where this class comes in: if type is
                 * not a known "primary JDK type", perhaps it's a bean?
                 * We can still get a null, if we can't find a single
                 * suitable bean property.
                 */
                ser = this.findBeanSerializer(type, config);
                /* Finally: maybe we can still deal with it as an
                 * implementation of some basic JDK interface?
                 */
                if (ser == null) {
                    ser = super.findSerializerByAddonType(type);
                }
            }
        }
        return (JsonSerializer<T>) ser;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Other public methods that are not part of
    // JsonSerializerFactory API
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method that will try to construct a {@link BeanSerializer} for
     * given class. Returns null if no properties are found.
     */
    public JsonSerializer<Object> findBeanSerializer(Class<?> type, SerializationConfig config)
    {
        // First things first: we know some types are not beans...
        if (!isPotentialBeanType(type)) {
            return null;
        }
        BasicBeanDescription beanDesc = config.introspect(type);
        JsonSerializer<Object> ser = findSerializerFromAnnotation(beanDesc.getClassInfo());
        if (ser != null) {
            return ser;
        }

        /* [JACKSON-80]: Should support @JsonValue, which is alternative to
         *   actual bean method introspection.
         */
        AnnotatedMethod valueMethod = beanDesc.findJsonValue();
        if (valueMethod != null) {
            /* Further, method itself may also be annotated to indicate
             * exact JsonSerializer to use for whatever value is returned...
             */
            ser = findSerializerFromAnnotation(valueMethod);
            return new JsonValueSerializer(valueMethod.getAnnotated(), ser);
        }

        // First: what properties are to be serializable?
        Collection<BeanPropertyWriter> props = findBeanProperties(config, beanDesc);
        if (props == null || props.size() == 0) {
            // No properties, no serializer
            return null;
        }
        return new BeanSerializer(type, props);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Overridable internal methods
    ////////////////////////////////////////////////////////////
     */

    /**
     * Helper method used to skip processing for types that we know
     * can not be (i.e. are never consider to be) beans: 
     * things like primitives, Arrays, Enums, and proxy types.
     *<p>
     * Note that usually we shouldn't really be getting these sort of
     * types anyway; but better safe than sorry.
     */
    protected boolean isPotentialBeanType(Class<?> type)
    {
        return (ClassUtil.canBeABeanType(type) == null) && !ClassUtil.isProxyType(type);
    }

    /**
     * Method used to collect all actual serializable properties
     */
    protected Collection<BeanPropertyWriter> findBeanProperties(SerializationConfig config, BasicBeanDescription beanDesc)
    {
        // are getters auto-detected?
        boolean autodetect = config.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS);
        LinkedHashMap<String,AnnotatedMethod> methodsByProp = beanDesc.findGetters(autodetect, null);
        // nothing? can't proceed
        if (methodsByProp.isEmpty()) {
            return null;
        }

        /* are null properties to be written for properties of
         * this class?
         */
        boolean writeNulls = beanDesc.willWriteNullProperties(config.isEnabled(SerializationConfig.Feature.WRITE_NULL_PROPERTIES));
        boolean fixAccess = config.isEnabled(SerializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);

        ArrayList<BeanPropertyWriter> props = new ArrayList<BeanPropertyWriter>(methodsByProp.size());
        for (Map.Entry<String,AnnotatedMethod> en : methodsByProp.entrySet()) {
            AnnotatedMethod am = en.getValue();
            if (fixAccess) {
                am.fixAccess();
            }
            /* One more thing: does Method specify a serializer?
             * If so, let's use it.
             */
            JsonSerializer<Object> ser = findSerializerFromAnnotation(am);
            Method m = am.getAnnotated();
            // and finally, there may be per-method overrides:
            boolean methodNulls = am.willWriteNullProperties(writeNulls);
            props.add(new BeanPropertyWriter(en.getKey(), m, ser, methodNulls));
        }
        return props;
    }
}
