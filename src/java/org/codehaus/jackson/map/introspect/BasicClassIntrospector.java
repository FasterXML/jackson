package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.map.ClassIntrospector;

public class BasicClassIntrospector
	extends ClassIntrospector<BasicBeanDescription>
{
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
            return AnnotatedMethod.hasGetterSignature(m);
        }
    }

    /**
     * Filter used to only include methods that have signature that is
     * compatible with "setters": take one and only argument and
     * are non-static.
     *<p>
     * 23-Mar-2009, tsaloranta: Actually, also need to include 2-arg
     *    methods to support "any setters"...
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
            // Must take just one arg, or be an AnySetter with 2 args:
            int pcount = m.getParameterTypes().length;
            if (pcount == 0 || pcount > 2) {
                return false;
            }
            if (pcount == 2) {
                if (m.getAnnotation(JsonAnySetter.class) == null) {
                    return false;
                }
            }
            // No checking for returning type; usually void, don't care
            // Otherwise, potentially ok
            return true;
        }
    }

    /*
    ///////////////////////////////////////////////////////
    // Life cycle
    ///////////////////////////////////////////////////////
     */

    public final static BasicClassIntrospector instance = new BasicClassIntrospector();

    public BasicClassIntrospector() { }

    /*
    ///////////////////////////////////////////////////////
    // Factory method impls
    ///////////////////////////////////////////////////////
     */

    public BasicBeanDescription forSerialization(Class<?> c)
    {
        /* Simpler for serialization; just need class annotations
         * and setters, not creators.
         */
    	MethodFilter mf = getGetterMethodFilter();
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, false, mf);
        return new BasicBeanDescription(c, ac);
    }

    @Override
    public BasicBeanDescription forDeserialization(Class<?> c)
    {
        /* More info needed than with serialization, also need creator
         * info
         */
    	MethodFilter mf = getSetterMethodFilter();
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, true, mf);
        return new BasicBeanDescription(c, ac);
    }

    @Override
    public BasicBeanDescription forCreation(Class<?> c)
    {
        /* Just need constructors and factory methods, but no
         * member methods
         */
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, true, null);
        return new BasicBeanDescription(c, ac);
    }

    @Override
    public BasicBeanDescription forClassAnnotations(Class<?> c)
    {
        /* More infor for serialization, also need creator
         * info
         */
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, false, null);
        return new BasicBeanDescription(c, ac);
    }

    /*
    ///////////////////////////////////////////////////////
    // Overridable helper methods
    ///////////////////////////////////////////////////////
     */
    
    protected MethodFilter getGetterMethodFilter()
    {
    	return GetterMethodFilter.instance;
    }

    protected MethodFilter getSetterMethodFilter()
    {
    	return SetterMethodFilter.instance;
    }
}
