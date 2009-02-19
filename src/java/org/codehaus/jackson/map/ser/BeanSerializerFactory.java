package org.codehaus.jackson.map.ser;

import java.util.*;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;

import org.codehaus.jackson.annotate.JsonUseSerializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerFactory;
import org.codehaus.jackson.map.type.ClassIntrospector;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Factory class that can provide serializers for any regular Java beans
 * (as defined by "having at least one get method recognizable as bean
 * accessor" -- where {@link Object#getClass} does not count);
 * as well as for "standard" JDK types. Latter is achieved
 * by delegating calls to {@link StdSerializerFactory} 
 * to find serializers both for "standard" JDK types (and in some cases,
 * sub-classes as is the case for collection classes like
 * {@link java.util.List}s and {@link java.util.Map}s) and bean (value)
 * classes.
 *<p>
 * Note about delegating calls to {@link StdSerializerFactory}:
 * although it would be nicer to use linear delegation
 * for construction (to essentially dispatch all calls first to the
 * underlying {@link StdSerializerFactory}; or alternatively after
 * failing to provide bean-based serializer}, there is a problem:
 * priority levels for detecting standard types are mixed. That is,
 * we want to check if a type is a bean after some of "standard" JDK
 * types, but before the rest.
 * As a result, "mixed" delegation used, and calls are NOT done using
 * regular {@link SerializerFactory} interface but rather via
 * direct calls to {@link StdSerializerFactory}.
 */
public class BeanSerializerFactory
    extends SerializerFactory
{
    /**
     * Like {@link StdSerializerFactory}, this factory is stateless, and
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
    public <T> JsonSerializer<T> createSerializer(Class<T> type)
    {
        // First, fast lookup for exact type:
        StdSerializerFactory stdF = StdSerializerFactory.instance;
        JsonSerializer<?> ser = stdF.findSerializerByLookup(type);
        if (ser == null) {
            // and then introspect for some safe (?) JDK types
            ser = stdF.findSerializerByPrimaryType(type);
            if (ser == null) {
                /* And this is where this class comes in: if type is
                 * not a known "primary JDK type", perhaps it's a bean?
                 * We can still get a null, if we can't find a single
                 * suitable bean property.
                 */
                ser = this.findBeanSerializer(type);
                /* Finally: maybe we can still deal with it as an
                 * implementation of some basic JDK interface?
                 */
                if (ser == null) {
                    ser = stdF.findSerializerByAddonType(type);
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
    public JsonSerializer<Object> findBeanSerializer(Class<?> type)
    {
        JsonSerializer<Object> ser = findSerializerByAnnotation(type);
        if (ser != null) {
            return ser;
        }

        // First things first: we know some types are not beans...
        if (!isPotentialBeanType(type)) {
            return null;
        }

        // First: what properties are to be serializable?
        Collection<BeanPropertyWriter> props = findBeanProperties(type);
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
     * Helper method called to check if the class in question
     * has {@link JsonUseSerializer} annotation which tells the
     * class to use for serialization.
     * Returns null if no such annotation found.
     */
    protected JsonSerializer<Object> findSerializerByAnnotation(AnnotatedElement elem)
    {
        JsonUseSerializer ann = elem.getAnnotation(JsonUseSerializer.class);
        if (ann != null) {
            Class<?> serClass = ann.value();
            // Must be of proper type, of course
            if (!JsonSerializer.class.isAssignableFrom(serClass)) {
                throw new IllegalArgumentException("Invalid @JsonSerializer annotation for "+ClassUtil.descFor(elem)+": value ("+serClass.getName()+") does not implement JsonSerializer interface");
            }
            try {
                Object ob = serClass.newInstance();
                @SuppressWarnings("unchecked")
                    JsonSerializer<Object> ser = (JsonSerializer<Object>) ob;
                return ser;
            } catch (Exception e) {
                throw new IllegalArgumentException("Failed to instantiate "+serClass.getName()+" to use as serializer for "+ClassUtil.descFor(elem)+", problem: "+e.getMessage(), e);
            }
        }
        return null;
    }

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
    protected Collection<BeanPropertyWriter> findBeanProperties(Class<?> forClass)
    {
        ClassIntrospector intr = new ClassIntrospector(forClass);
        LinkedHashMap<String,Method> methodsByProp = intr.findGetters();
        // nothing? can't proceed
        if (methodsByProp.isEmpty()) {
            return null;
        }
        ArrayList<BeanPropertyWriter> props = new ArrayList<BeanPropertyWriter>(methodsByProp.size());
        for (Map.Entry<String,Method> en : methodsByProp.entrySet()) {
            Method m = en.getValue();
            ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());
            BeanPropertyWriter wprop = new BeanPropertyWriter(en.getKey(), m);
            props.add(wprop);

            /* One more thing: does Method specify a serializer?
             * If so, let's use it.
             */
            JsonSerializer<Object> ser = findSerializerByAnnotation(m);
            if (ser != null) {
                wprop.assignSerializer(ser);
            }
        }
        return props;
    }
}
