package org.codehaus.jackson.map.ser;

import java.util.*;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;

import org.codehaus.jackson.map.JsonSerializer;

/**
 * Factory class that can provide serializers for any regular Java beans
 * (as defined by "having at least one get method recognizable as bean
 * accessor" -- where {@link Object#getClass} does not count);
 * as well as for "standard" JDK types. Latter is achieved
 * by sub-classing {@link StdSerializerFactory} to augment its functionality
 * by bean introspection.
 *<p>
 * Note about design: although it would be nicer to use linear delegation
 * for construction (to essentially dispatch all calls first to the
 * underlying {@link StdSerializerFactory}, there is one problem:
 * priority levels for detecting standard types are mixed. That is,
 * we want to check if a type is a bean after some of "standard" JDK
 * types, but before the rest. This is why sub-classing is used, and
 * specific calls that std serializer factory exposes, instead of using
 * public {@link JsonSerializerFactory} api.
 */
public class BeanSerializerFactory
    extends StdSerializerFactory
{
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
     */
    @Override
    @SuppressWarnings("unchecked")
    public <T> JsonSerializer<T> createSerializer(Class<T> type)
    {
        // First, fast lookup for exact type:
        JsonSerializer<?> ser = findSerializerByLookup(type);
        if (ser == null) {
            // and then introspect for some safe (?) JDK types
            ser = findSerializerByPrimaryType(type);
            if (ser == null) {
                // But if no  match, let's see if it might be a bean
                ser = findBeanSerializer(type);
                // if not, then fall back to other JDK types
                if (ser == null) {
                    ser = findSerializerByAddonType(type);
                }
            }
        }
        return (JsonSerializer<T>) ser;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method that will try to construct a {@link BeanSerializer} for
     * given class. Returns null if no properties are found.
     */
    protected JsonSerializer<?> findBeanSerializer(Class<?> type)
    {
        // First things first: we know some types are not beans...
        if (!canBeABeanType(type) || isProxyType(type)) {
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

    /**
     * Helper method used to skip processing for types that we know
     * can not be (i.e. are never consider to be) beans: 
     * things like primitives, Arrays, Enums.
     *<p>
     * Note that usually we shouldn't really be getting these sort of
     * types anyway; but better safe than sorry.
     */
    protected boolean canBeABeanType(Class<?> type)
    {
        // First: language constructs that ain't beans:
        if (type.isAnnotation() || type.isArray() || type.isEnum()
            || type.isPrimitive()) {
            return false;
        }
        return true;
    }

    /**
     * Helper method used to weed out dynamic Proxy types; types that do
     * not expose concrete method API that we could use to figure out
     * automatic Bean (property) based serialization.
     */
    protected boolean isProxyType(Class<?> type)
    {
        // Then: well-known proxy (etc) classes
        if (Proxy.isProxyClass(type)) {
            return true;
        }
        String name = type.getName();
        // Hibernate uses proxies heavily as well:
        if (name.startsWith("net.sf.cglib.proxy.")
            || name.startsWith("org.hibernate.proxy.")) {
            return true;
        }
        // Not one of known proxies, nope:
        return false;
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
        Class<?> rt = m.getReturnType();
        if (!Modifier.isPublic(rt.getModifiers())) {
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
            return mangleName(name.substring(3));
        }
        if (name.startsWith("is")) {
            // plus, must return boolean...
            if (rt != Boolean.class && rt != Boolean.TYPE) {
                return null;
            }
            return mangleName(name.substring(2));
        }
        // no, not a match by name
        return null;
    }

    /**
     * Method called to figure out name of the property, given 
     * corresponding suggested name based on method name.
     *
     * @param basename Name of accessor method, not including prefix
     *  ("get" or "is")
     */
    protected String mangleName(String basename)
    {
        int len = basename.length();

        // First things first: empty basename ("is" or "get") is no good
        if (len == 0) {
            return null;
        }
        // otherwise, lower case initial chars
        StringBuilder sb = null;
        for (int i = 0; i < len; ++i) {
            char upper = basename.charAt(i);
            char lower = Character.toLowerCase(upper);
            if (upper == lower) {
                break;
            }
            if (sb == null) {
                sb = new StringBuilder(basename);
            }
            sb.setCharAt(i, lower);
        }
        return (sb == null) ? basename : sb.toString();
    }

    /**
     * Method called to check if we can use the passed method (wrt
     * access restriction -- public methods can be called, others
     * usually not); and if not, if there is a work-around for
     * the problem.
     */
    protected Method checkAccess(Method m, String name)
    {
        if (!m.isAccessible()) {
            try {
                m.setAccessible(true);
            } catch (SecurityException se) {
                throw new IllegalArgumentException("Can not access property '"+name+"' (via method "+m.getDeclaringClass()+"#"+m.getName()+"()); failed to set access: "+se.getMessage());
            }
        }
        return m;
    }
}
