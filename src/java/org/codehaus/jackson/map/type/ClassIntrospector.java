package org.codehaus.jackson.map.type;

import java.lang.annotation.Annotation;
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
    // Helper classes
    ///////////////////////////////////////////////////////
     */

    /**
     * Filter used to only include methods that have signature that is
     * compatible with "getters": take no arguments, are non-static,
     * and return something.
     */
    public final static class GetterMethodFilter
        implements MethodFilter
    {
        public final static GetterMethodFilter instance = new GetterMethodFilter();

        private GetterMethodFilter() { }
    
        public boolean includeMethod(Method m)
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
    }

    /**
     * Filter used to only include methods that have signature that is
     * compatible with "setters": take one and only argument and
     * are non-static.
     */
    public final static class SetterMethodFilter
        implements MethodFilter
    {
        public final static SetterMethodFilter instance = new SetterMethodFilter();

        public boolean includeMethod(Method m)
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
    }

    /*
    ///////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////
     */

    /**
     * Class being introspected
     */
    final Class<?> _class;

    /**
     * Information collected about the class introspected.
     */
    final AnnotatedClass _classInfo;

    /*
    ///////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////
     */

    public ClassIntrospector(Class<?> c, AnnotatedClass ac)
    {
        _class = c;
        _classInfo = ac;
    }

    /**
     * Factory method that constructs an introspector that has all
     * information needed for serialization purposes.
     */
    public static ClassIntrospector forSerialization(Class<?> c)
    {
        /* Simpler for serialization; just need class annotations
         * and setters, not creators.
         */
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, false, GetterMethodFilter.instance);
        return new ClassIntrospector(c, ac);
    }

    /**
     * Factory method that constructs an introspector that has all
     * information needed for deserialization purposes.
     */
    public static ClassIntrospector forDeserialization(Class<?> c)
    {
        /* More infor for serialization, also need creator
         * info
         */
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, true, SetterMethodFilter.instance);
        return new ClassIntrospector(c, ac);
    }

    /**
     * Factory method that constructs an introspector that has
     * information necessary for creating instances of given
     * class ("creator"), as well as class annotations, but
     * no information on member methods
     */
    public static ClassIntrospector forCreation(Class<?> c)
    {
        /* More infor for serialization, also need creator
         * info
         */
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, true, null);
        return new ClassIntrospector(c, ac);
    }

    /*
    ///////////////////////////////////////////////////////
    // Generic introspection
    ///////////////////////////////////////////////////////
     */

    public <A extends Annotation> A getClassAnnotation(Class<A> acls)
    {
        return _classInfo.getClassAnnotation(acls);
    }

    /*
    ///////////////////////////////////////////////////////
    // Introspection for getters
    ///////////////////////////////////////////////////////
     */

    /**
     * @param autodetect Whether to use Bean naming convention to
     *   automatically detect bean properties; if true will do that,
     *   if false will require explicit annotations.
     *
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     */
    public LinkedHashMap<String,Method> findGetters(boolean autodetect)
    {
        /* As part of [JACKSON-52] we'll use baseline settings for
         * auto-detection, but also see if the class might override
         * that setting.
         */
        JsonAutoDetect cann = _classInfo.getClassAnnotation(JsonAutoDetect.class);
        if (cann != null) {
            JsonMethod[] methods = cann.value();
            if (methods != null) {
                autodetect = false;
                for (JsonMethod jm : methods) {
                    if (jm.getterEnabled()) {
                        autodetect = true;
                        break;
                    }
                }
            }
        }

        LinkedHashMap<String,Method> results = new LinkedHashMap<String,Method>();
        for (AnnotatedMethod am : _classInfo.getMemberMethods()) {
            // note: signature has already been checked via filters
            // Marked with @JsonIgnore?
            if (isIgnored(am)) {
                continue;
            }

            /* So far so good: final check, then; has to either
             * (a) be marked with @JsonGetter OR
             * (b) be public AND have suitable name (getXxx or isXxx)
             */
            JsonGetter ann = am.getAnnotation(JsonGetter.class);
            String propName;

            if (ann != null) {
                propName = ann.value();
                if (propName == null || propName.length() == 0) {
                    // Defaults to method name
                    propName = am.getName();
                }
            } else { // nope, but is public bean-getter name?
                if (!autodetect) {
                    continue;
                }
                propName = okNameForGetter(am);
                if (propName == null) { // null means 'not valid'
                    continue;
                }
            }

            /* Yup, it is a valid name. But now... do we have a conflict?
             * If so, should throw an exception
             */
            Method m = am.getAnnotated();
            ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());
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
        for (AnnotatedMethod am : _classInfo.getMemberMethods()) {
            // note: signature has already been checked via filters
            // Marked with @JsonIgnore?
            if (isIgnored(am)) {
                continue;
            }

            /* So far so good: final check, then; has to either
             * (a) be marked with @JsonSetter OR
             * (b) have suitable name (setXxx) (NOTE: need not be
             *    public, unlike with getters)
             */
            JsonSetter ann = am.getAnnotation(JsonSetter.class);
            String propName;

            if (ann != null) {
                propName = ann.value();
                if (propName == null || propName.length() == 0) {
                    // Defaults to method name
                    propName = am.getName();
                }
            } else { // nope, but is public bean-setter name?
                propName = okNameForSetter(am);
                if (propName == null) { // null means 'not valid'
                    continue;
                }
            }

            /* Yup, it is a valid name. But now... do we have a conflict?
             * If so, should throw an exception
             */
            Method m = am.getAnnotated();
            ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());
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
        AnnotatedConstructor ac = _classInfo.getDefaultConstructor();
        if (ac == null) {
            return null;
        }
        Constructor<?> c = ac.getAnnotated();
        ClassUtil.checkAndFixAccess(c, c.getDeclaringClass());
        return c;
    }

    /**
     * Method that can be called to locate a single-arg constructor that
     * takes specified exact type (will not accept supertype constructors)
     *
     * @param argTypes Type(s) of the argument that we are looking for
     */
    public Constructor<?> findSingleArgConstructor(Class<?>... argTypes)
    {
        for (AnnotatedConstructor ac : _classInfo.getSingleArgConstructors()) {
            // This list is already filtered to only include accessible
            Class<?>[] args = ac.getParameterTypes();
            /* (note: for now this is a redundant check; but in future
             * that'll change; thus leaving here for now)
             */
            if (args.length == 1) {
                Class<?> actArg = args[0];
                for (Class<?> expArg : argTypes) {
                    if (expArg == actArg) {
                        Constructor<?> c = ac.getAnnotated();
                        ClassUtil.checkAndFixAccess(c, c.getDeclaringClass());
                        return c;
                    }
                }
            }
        }
        return null;
    }

    /**
     * Method that can be called to find if introspected class declares
     * a static "valueOf" factory method that returns an instance of
     * introspected type, given one of acceptable types.
     *
     * @param expArgTypes Types that the matching single argument factory
     *   method can take: will also accept super types of these types
     *   (ie. arg just has to be assignable from expArgType)
     */
    public Method findFactoryMethod(Class<?>... expArgTypes)
    {
        // So, of all single-arg static methods:
        for (AnnotatedMethod am : _classInfo.getSingleArgStaticMethods()) {
            // First: return type must be the introspected class
            if (am.getReturnType() != _class) {
                continue;
            }
            /* Then: must be a recognized factory, meaning:
             * (a) public "valueOf", OR
             * (b) marked with @JsonCreator annotation
             */
            if (am.hasAnnotation(JsonCreator.class)) {
                ;
            } else if ("valueOf".equals(am.getName())) {
                ;
            } else { // not recognized, skip
                continue;
            }

            // And finally, must take one of expected arg types (or supertype)
            Class<?> actualArgType = am.getParameterTypes()[0];
            Method m = am.getAnnotated();
            for (Class<?> expArgType : expArgTypes) {
                // And one that matches what we would pass in
                if (actualArgType.isAssignableFrom(expArgType)) {
                    ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());
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

    protected String okNameForGetter(AnnotatedMethod am)
    {
        String name = am.getName();
        Method m = am.getAnnotated();

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

            /* 16-Feb-2009, tatus: To handle [JACKSON-53], need to block
             *   CGLib-provided method "getCallbacks". Not sure of exact
             *   safe critieria to get decent coverage without false matches;
             *   but for now let's assume there's no reason to use any 
             *   such getter from CGLib.
             *   But let's try this approach...
             */
            if ("getCallbacks".equals(name)) {
                if (isCglibGetCallbacks(m)) {
                    return null;
                }
            }

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

    /**
     * This method was added to address [JACKSON-53]: need to weed out
     * CGLib-injected "getCallbacks". 
     * At this point caller has detected a potential getter method
     * with name "getCallbacks" and we need to determine if it is
     * indeed injectect by Cglib. We do this by verifying that the
     * result type is "net.sf.cglib.proxy.Callback[]"
     */
    protected boolean isCglibGetCallbacks(Method m)
    {
        Class<?> rt = m.getReturnType();
        // Ok, first: must return an array type
        if (rt == null || !rt.isArray()) {
            return false;
        }
        /* And that type needs to be "net.sf.cglib.proxy.Callback".
         * Theoretically could just be a type that implements it, but
         * for now let's keep things simple, fix if need be.
         */
        Class<?> compType = rt.getComponentType();
        // Actually, let's just verify it's a "net.sf.cglib.*" class/interface
        Package pkg = compType.getPackage();
        if (pkg != null && pkg.getName().startsWith("net.sf.cglib")) {
            return true;
        }
        return false;
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper methods for setters
    ///////////////////////////////////////////////////////
     */

    protected String okNameForSetter(AnnotatedMethod am)
    {
        String name = am.getName();
        Method m = am.getAnnotated();

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

    /**
     * Helper method used to check whether given element
     * (method, constructor, class) has enabled (active)
     * instance of {@link JsonIgnore} annotation.
     */
    protected boolean isIgnored(AnnotatedMethod am)
    {
        JsonIgnore ann = am.getAnnotation(JsonIgnore.class);
        return (ann != null && ann.value());
    }
}

