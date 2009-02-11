package org.codehaus.jackson.map.type;

import java.util.*;
import java.lang.reflect.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Helper class used to introspect methods (getters, setters, creators)
 * that can be used handle POJOs.
 */
public class ClassIntrospector
{
    /**
     * Class that we are introspecting things about
     */
    protected final Class<?> _class;

    public ClassIntrospector(Class<?> c)
    {
        _class = c;
    }

    /*
    ///////////////////////////////////////////////////////
    // Introspection for getters
    ///////////////////////////////////////////////////////
     */

    /**
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     */
    public LinkedHashMap<String,Method> findGetters()
    {
        LinkedHashMap<String,Method> results = new LinkedHashMap<String,Method>();

        /* Also: need to keep track of Method masking: that is, super-class
         * methods should not be visible if masked
         */
        HashSet<String> maskedMethods = new HashSet<String>();

        DeclMethodIter it = new DeclMethodIter(_class);
        Method m;

        while ((m = it.next()) != null) {
            // First, let's ignore anything that's not formally ok (fast check)
            if (!okSignatureForGetter(m)) {
                continue;
            }
            String name = m.getName();
            // Then, can not be masked
            if (!maskedMethods.add(name)) { // was already in there, skip
                continue;
            }
            // Marked with @JsonIgnore?
            if (m.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }

            /* So far so good: final check, then; has to either
             * (a) be marked with @JsonGetter OR
             * (b) be public AND have suitable name (getXxx or isXxx)
             */
            JsonGetter ann = m.getAnnotation(JsonGetter.class);
            String propName;

            if (ann != null) {
                propName = ann.value();
                if (propName == null || propName.length() == 0) {
                    // Defaults to method name
                    propName = m.getName();
                }
            } else { // nope, but is public bean-getter name?
                propName = okNameForGetter(m);
                if (propName == null) { // null means 'not valid'
                    continue;
                }
            }

            /* Yup, it is a valid name. But now... do we have a conflict?
             * If so, should throw an exception
             */
            Method old = results.put(propName, m);
            if (old != null) {
                String oldDesc = old.getDeclaringClass().getName() + "#" + old.getName();
                String newDesc = m.getDeclaringClass().getName() + "#" + m.getName();
                throw new IllegalArgumentException("Overlapping getter definitions for property \""+propName+"\": "+oldDesc+"() vs "+newDesc+"()");
            }
        }

        return results;
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper methods for getters
    ///////////////////////////////////////////////////////
     */

    /**
     * Method that verifies that the given method's signature
     * is compatible with method possibly being a getter method;
     * that is, method is non-static, does return a value (not void)
     * and does not take any arguments.
     */
    protected boolean okSignatureForGetter(Method m)
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

    protected String okNameForGetter(Method m)
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
             * is not considered a bean accessor.
             * (but is ok if overriden)
             */
            // 10-Feb-2009, tatus: Should never occur, actually
            /*if ("getClass".equals(m.getName()) && m.getDeclaringClass() == Object.class) {
                return null;
            }
            */
            return mangleGetterName(m, name.substring(3));
        }
        if (name.startsWith("is")) {
            // plus, must return boolean...
            Class<?> rt = m.getReturnType();
            if (rt != Boolean.class && rt != Boolean.TYPE) {
                return null;
            }
            return mangleGetterName(m, name.substring(2));
        }
        // no, not a match by name
        return null;
    }

    /**
     * @return Null to indicate that method is not a valid accessor;
     *   otherwise name of the property it is accessor for
     */
    protected String mangleGetterName(Method method, String basename)
    {
        return ClassUtil.manglePropertyName(basename);
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////////////
     */

    /**
     * Let's abstract out details of iterating over all declared
     * methods of a class, in decreasing order (starting with sub-class,
     * following super-type chain)
     */
    static class DeclMethodIter
    {
        Class<?> _currClass;

        /**
         * Methods of the current class
         */
        Method[] _currMethods;

        int _currIndex;

        public DeclMethodIter(Class<?> c)
        {
            _currClass = c;
            _currMethods = c.getDeclaredMethods();
            _currIndex = 0;
        }

        public Method next()
        {
            while (_currIndex >= _currMethods.length) { // need more
                if (_currClass == null) {
                    return null;
                }
                _currClass = _currClass.getSuperclass();
                if (_currClass == null || _currClass == Object.class) {
                    return null;
                }
                _currIndex = 0;
                _currMethods = _currClass.getDeclaredMethods();
            }
            return _currMethods[_currIndex++];
        }
    }
}

