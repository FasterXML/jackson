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
    /*
    ///////////////////////////////////////////////////////
    // Lazy-loaded reusable pieces of reflection info
    ///////////////////////////////////////////////////////
     */

    transient Constructor<?>[] _ctors;

    transient Method[] _directMethods;

    /*
    ///////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////
     */

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
        DeclMethodIter it = methodIterator();
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
                throw new IllegalArgumentException("Conflicting getter definitions for property \""+propName+"\": "+oldDesc+"() vs "+newDesc+"()");
            }
        }

        return results;
    }

    /*
    ///////////////////////////////////////////////////////
    // Introspection for setters
    ///////////////////////////////////////////////////////
     */

    /**
     * @return Ordered Map with logical property name as key, and
     *    matching setter method as value.
     */
    public LinkedHashMap<String,Method> findSetters()
    {
        LinkedHashMap<String,Method> results = new LinkedHashMap<String,Method>();

        /* Also: need to keep track of Method masking: that is, super-class
         * methods should not be visible if masked
         */
        HashSet<String> maskedMethods = new HashSet<String>();

        DeclMethodIter it = methodIterator();
        Method m;

        while ((m = it.next()) != null) {
            // First, let's ignore anything that's not formally ok (fast check)
            if (!okSignatureForSetter(m)) {
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
             * (a) be marked with @JsonSetter OR
             * (b) have suitable name (setXxx) (NOTE: need not be
             *    public, unlike with getters)
             */
            JsonSetter ann = m.getAnnotation(JsonSetter.class);
            String propName;

            if (ann != null) {
                propName = ann.value();
                if (propName == null || propName.length() == 0) {
                    // Defaults to method name
                    propName = m.getName();
                }
            } else { // nope, but is public bean-setter name?
                propName = okNameForSetter(m);
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
                throw new IllegalArgumentException("Conflicting setter definitions for property \""+propName+"\": "+oldDesc+"() vs "+newDesc+"()");
            }
        }

        return results;
    }

    /*
    ///////////////////////////////////////////////////////
    // Introspection for constructors, factory methods
    ///////////////////////////////////////////////////////
     */

    /**
     * Method that will locate the no-arg constructor for this class,
     * if it has one, and that constructor has not been marked as
     * ignorable.
     * Method will also ensure that the constructor is accessible.
     */
    public Constructor<?> findDefaultConstructor()
    {
        for (Constructor<?> ctor : declaredConstructors()) {
            // won't use varargs, no point
            if (!ctor.isVarArgs() && ctor.getParameterTypes().length == 0) {
                // 11-Feb-2009, tatu: Also, must ignore if instructed to:
                if (!ctor.isAnnotationPresent(JsonIgnore.class)) {
                    ClassUtil.checkAndFixAccess(ctor, _class);
                    return ctor;
                }
                // only one such ctor anyway, so let's bail if that one's no good
                break;
            }
        }
        return null;
    }

    /**
     * Method that can be called to locate a single-arg constructor that
     * takes specified exact type (will not accept supertype constructors)
     *
     * @param argTypes Type(s) of the argument that we are looking for
     */
    public Constructor<?> findSingleArgConstructor(Class<?>... argTypes)
    {
        for (Constructor<?> c : declaredConstructors()) {
            // First: must obey @JsonIgnore if present, and skip it
            if (c.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            Class<?>[] args = c.getParameterTypes();
            // Otherwise must have just one arg of specific type
            if (args.length == 1) {
                Class<?> actArg = args[0];
                for (Class<?> expArg : argTypes) {
                    if (expArg == actArg) {
                        ClassUtil.checkAndFixAccess(c, _class);
                        return c;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Method for obtaining list of all static methods with given name,
     * declared directly within instropected class
     */
    public List<Method> findStaticSingleArgMethods()
    {
        ArrayList<Method> result = null;
        for (Method m : declaredMethods()) {
            // only static methods will do
            if (!Modifier.isStatic(m.getModifiers())) {
                continue;
            }
            // can't be included if directed to be ignored
            if (m.isAnnotationPresent(JsonIgnore.class)) {
                continue;
            }
            // and must take exactly one argument
            if (m.getParameterTypes().length != 1) {
                continue;
            }
            // ok, need to add
            if (result == null) {
                result = new ArrayList<Method>();
            }
            result.add(m);
        }
        if (result == null) {
            return Collections.emptyList();
        }
        return result;
    }
            

    /**
     * Method that can be called to find if introspected class declares
     * a static "valueOf" factory method that returns an instance of
     * introspected type, given one of acceptable types.
     *
     * @param expArgTypes Types that the matching single argument factory
     *   method can take: will also accept super types of these types
     *   (ie. arg just has to be assignable from expArgType)
     * @param needsToBePublic If true, will only consider public methods;
     *   if false, will accept any access type (and will also fix access
     *   modifiers if so)
     */
    public Method findFactoryMethod(Class<?>... expArgTypes)
    {
        // So, of all single-arg static methods:
        for (Method m : findStaticSingleArgMethods()) {
            // First: return type must be the introspected class
            if (m.getReturnType() != _class) {
                continue;
            }
            /* Then: must be a recognized factory, meaning:
             * (a) public "valueOf", OR
             * (b) marked with @JsonCreator annotation
             */
            if (m.isAnnotationPresent(JsonCreator.class)) {
                ;
            } else if ("valueOf".equals(m.getName())
                       && Modifier.isPublic(m.getModifiers())) {
                ;
            } else { // not recognized, skip
                continue;
            }

            // And finally, must take one of expected arg types (or supertype)
            Class<?> actualArgType = m.getParameterTypes()[0];
            for (Class<?> expArgType : expArgTypes) {
                // And one that matches what we would pass in
                if (actualArgType.isAssignableFrom(expArgType)) {
                    ClassUtil.checkAndFixAccess(m, _class);
                    return m;
                }
            }
        }
        return null;
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
    // Helper methods for setters
    ///////////////////////////////////////////////////////
     */

    /**
     * Method that verifies that the given method's signature
     * is compatible with method possibly being a setter method;
     * that is, method is non-static, does return a value (not void)
     * and does not take any arguments.
     */
    protected boolean okSignatureForSetter(Method m)
    {
        // First: we can't use static methods
        if (Modifier.isStatic(m.getModifiers())) {
            return false;
        }
        // Must take just one arg
        Class<?>[] pts = m.getParameterTypes();
        if ((pts == null) || (pts.length != 1)) {
            return false;
        }
        // No checking for returning type; usually void, don't care
        // Otherwise, potentially ok
        return true;
    }

    protected String okNameForSetter(Method m)
    {
        String name = m.getName();

        /* For mutators, let's not require it to be public. Just need
         * to be able to call it, i.e. do need to 'fix' access if so
         * (which is done at a later point as needed)
         */
        if (name.startsWith("set")) {
            name = mangleSetterName(m, name.substring(3));
            if (name == null) { // plain old "set" is no good...
                return null;
            }
            return name;
        }
        return null;
    }

    /**
     * @return Null to indicate that method is not a valid accessor;
     *   otherwise name of the property it is accessor for
     */
    protected String mangleSetterName(Method method, String basename)
    {
        return ClassUtil.manglePropertyName(basename);
    }

    /*
    ///////////////////////////////////////////////////////
    // Low-level class info helper methods
    ///////////////////////////////////////////////////////
     */

    protected Constructor<?>[] declaredConstructors()
    {
        if (_ctors == null) {
            _ctors = _class.getDeclaredConstructors();
        }
        return _ctors;
    }

    protected Method[] declaredMethods()
    {
        if (_directMethods == null) {
            _directMethods = _class.getDeclaredMethods();
        }
        return _directMethods;
    }

    protected DeclMethodIter methodIterator()
    {
        return new DeclMethodIter(_class, declaredMethods());
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

        public DeclMethodIter(Class<?> c, Method[] declMethods)
        {
            _currClass = c;
            _currMethods = declMethods;
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

