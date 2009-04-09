package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.map.ClassIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.SerializationConfig;

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
     * Actually, also need to include 2-arg  methods to support
     * "any setters"; as well as 0-arg getters as long as they
     * return Collection or Map type.
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
            int pcount = m.getParameterTypes().length;
            // Ok; multiple acceptable parameter counts:
            switch (pcount) {
            case 0:
                /* Getters (no args and) that return Map or Collection may
                 * act as getter-as-setters.
                 */
                Class<?> rt = m.getReturnType();
                if (Collection.class.isAssignableFrom(rt)
                    || Map.class.isAssignableFrom(rt)) {
                    return true; 
                }
                break;
            case 1:
                // Regular setters take just one param, so include:
                return true;
            case 2:
                // AnySetters take 2 args...
                if (m.getAnnotation(JsonAnySetter.class) != null) {
                    return true;
                }
                break;
            }
            return false;
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

    public BasicBeanDescription forSerialization(SerializationConfig cfg,
                                                 Class<?> c)
    {
        /* Simpler for serialization; just need class annotations
         * and setters, not creators.
         */
    	MethodFilter mf = getGetterMethodFilter(cfg);
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, false, mf);
        return new BasicBeanDescription(c, ac);
    }

    @Override
    public BasicBeanDescription forDeserialization(DeserializationConfig cfg,
                                                   Class<?> c)
    {
        /* More info needed than with serialization, also need creator
         * info
         */
    	MethodFilter mf = getSetterMethodFilter(cfg);
        AnnotatedClass ac = AnnotatedClass.constructFull
            (c, JacksonAnnotationFilter.instance, true, mf);
        return new BasicBeanDescription(c, ac);
    }

    @Override
    public BasicBeanDescription forCreation(DeserializationConfig cfg,
                                            Class<?> c)
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
    
    protected MethodFilter getGetterMethodFilter(SerializationConfig cfg)
    {
    	return GetterMethodFilter.instance;
    }

    protected MethodFilter getSetterMethodFilter(DeserializationConfig cfg)
    {
    	return SetterMethodFilter.instance;
    }
}
