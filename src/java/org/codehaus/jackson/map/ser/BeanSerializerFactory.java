package org.codehaus.jackson.map.ser;

import java.util.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerFactory;
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
    public JsonSerializer<?> findBeanSerializer(Class<?> type)
    {
        // First things first: we know some types are not beans...
        if (!isPotentialBeanType(type)) {
            return null;
        }

        // First: what properties are to be serializable?
        Collection<WritableBeanProperty> props = findBeanProperties(type);
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
    protected Collection<WritableBeanProperty> findBeanProperties(Class<?> type)
    {
        /* Ok, now; we could try Class.getMethods(), but it has couple of
         * problems:
         *
         * (a) Only returns public methods (which is ok for accessor checks,
         *   but should allow annotations to indicate others too)
         * (b) Ordering is arbitrary (may be a problem with other accessors
         *   too?)
         *
         * So: let's instead gather methods ourself. One simplification is
         * that we should always be getting concrete type; hence we need
         * not worry about interfaces or such. Also, we can ignore everything
         * from java.lang.Object, which is neat.
         */
        LinkedHashMap<String,Method> methods = new LinkedHashMap<String,Method>();
        findCandidateMethods(type, methods);
        // nothing? can't proceed
        if (methods.isEmpty()) {
            return null;
        }
        LinkedHashMap<String,WritableBeanProperty> props = new LinkedHashMap<String,WritableBeanProperty>();
        for (Method m : methods.values()) {
            WritableBeanProperty p = convertToProperty(m);
            if (p != null) {
                // Also: we don't want dups...
                WritableBeanProperty prev = props.put(p.getName(), p);
                if (prev != null) {
                    throw new IllegalArgumentException("Duplicate property '"+p.getName()+"' for class "+type);
                }
            }
        }
        return props.values();
    }

    /**
     * Method for collecting list of all Methods that could conceivably
     * be accessors. At this point we will only do preliminary checks,
     * to weed out things that can not possibly be accessors (i.e. solely
     * based on signature, but not on name or annotations)
     */
    protected void findCandidateMethods(Class<?> type, Map<String,Method> result)
    {
        /* we'll add base class methods first (for ordering purposes), but
         * then override as necessary
         */
        Class<?> parent = type.getSuperclass();
        if (parent != null && parent != Object.class) {
            findCandidateMethods(parent, result);
        }
        for (Method m : type.getDeclaredMethods()) {
            if (okSignatureForAccessor(m)) {
                result.put(m.getName(), m);
            }
        }
    }

    protected boolean okSignatureForAccessor(Method m)
    {
        // First: we can't use static methods
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        // Must take no args
        Class<?>[] pts = m.getParameterTypes();
        if ((pts != null) && (pts.length > 0)) {
            return false;
        }
        // Can't be a void method
        Class<?> rt = m.getReturnType();
        if (rt == Void.TYPE) {
            return false;
        }
        // Otherwise, potentially ok
        return true;
    }

    /**
     * Method called to determine if given method defines a writable
     * property.
     */
    protected WritableBeanProperty convertToProperty(Method m)
    {
        /* Ok: at this point we already know that the signature is ok
         * (no args, returns a value); so we need to check
         * that name is ok. Sub-classes may also want to verify
         * annotations.
         */
        String name = okNameForAccessor(m);
        if (name != null) {
            // and finally, we may need to deal with access restrictions
            m = checkAccess(m, name);
            if (m != null) {
                return new WritableBeanProperty(name, m);
            }
        }
        // nope, not good
        return null;
    }

    protected String okNameForAccessor(Method m)
    {
        String name = m.getName();

        /* Actually, for non-annotation based names, let's require that
         * the method is public?
         */
        if (!Modifier.isPublic(m.getModifiers())) {
            return null;
        }

        if (name.startsWith("get")) {
            /* also, base definition (from java.lang.Object) of getClass()
             * is not consider a bean accessor.
             * (but is ok if overriden)
             */
            if ("getClass".equals(m.getName()) && m.getDeclaringClass() == Object.class) {
                return null;
            }
            return mangleName(m, name.substring(3));
        }
        if (name.startsWith("is")) {
            // plus, must return boolean...
            Class<?> rt = m.getReturnType();
            if (rt != Boolean.class && rt != Boolean.TYPE) {
                return null;
            }
            return ClassUtil.manglePropertyName(name.substring(2));
        }
        // no, not a match by name
        return null;
    }

    /**
     * @return Null to indicate that method is not a valid accessor;
     *   otherwise name of the property it is accessor for
     */
    protected String mangleName(Method method, String basename)
    {
        return ClassUtil.manglePropertyName(basename);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Other internal methods
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method called to check if we can use the passed method (wrt
     * access restriction -- public methods can be called, others
     * usually not); and if not, if there is a work-around for
     * the problem.
     */
    protected Method checkAccess(Method m, String name)
    {
        // this can only fail from exception: should we catch it?
        ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());
        return m;
    }
}
