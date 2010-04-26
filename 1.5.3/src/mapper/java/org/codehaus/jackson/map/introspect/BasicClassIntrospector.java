package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ClassIntrospector;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.MapperConfig;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;

public class BasicClassIntrospector
    extends ClassIntrospector<BasicBeanDescription>
{
    /**
     * Filter used to only include methods that have signature that is
     * compatible with "getters": take no arguments, are non-static,
     * and return something.
     */
    public static class GetterMethodFilter
        implements MethodFilter
    {
        public final static GetterMethodFilter instance = new GetterMethodFilter();

        private GetterMethodFilter() { }
    
        public boolean includeMethod(Method m)
        {
            return ClassUtil.hasGetterSignature(m);
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
    public static class SetterMethodFilter
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
            case 1:
                // Regular setters take just one param, so include:
                return true;
            case 2:
                /* 2-arg version only for "AnySetters"; they are not
                 * auto-detected, and need to have an annotation.
                 * However, due to annotation inheritance we do, we
                 * don't yet know if sub-classes might have annotations...
                 * so shouldn't leave out any methods quite yet.
                 */
                //if (m.getAnnotation(JsonAnySetter.class) != null) { ... }

                return true;
            }
            return false;
        }
    }

    /**
     * Filter used if some getters (namely, once needed for "setterless
     * collection injection") are also needed, not just setters.
     */
    public final static class SetterAndGetterMethodFilter
        extends SetterMethodFilter
    {
        public final static SetterAndGetterMethodFilter instance = new SetterAndGetterMethodFilter();

        public boolean includeMethod(Method m)
        {
            if (super.includeMethod(m)) {
                return true;
            }
            if (!ClassUtil.hasGetterSignature(m)) {
                return false;
            }
            // but furthermore, only accept Collections & Maps, for now
            Class<?> rt = m.getReturnType();
            if (Collection.class.isAssignableFrom(rt)
                || Map.class.isAssignableFrom(rt)) {
                return true; 
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
                                                 Class<?> c,
                                                 MixInResolver r)
    {
        AnnotationIntrospector ai = cfg.getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.construct(c, ai, r);
        // False -> no need to collect ignorable member list
        ac.resolveMemberMethods(getSerializationMethodFilter(cfg), false);
        /* only the default constructor needed here (that's needed
         * in case we need to check default bean property values,
         * to omit them)
         */
        /* 31-Oct-2009, tatus: Actually, creator info will come in handy
         *   for resolving [JACKSON-170] as well
         */
        ac.resolveCreators(true);
        // False -> no need to collect ignorable field list
        ac.resolveFields(false);
        return new BasicBeanDescription(TypeFactory.type(c), ac, ai);
    }

    @Override
    public BasicBeanDescription forDeserialization(DeserializationConfig cfg,
                                                   JavaType type,
                                                   MixInResolver r)
    {
        AnnotationIntrospector ai = cfg.getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.construct(type.getRawClass(), ai, r);
        // everything needed for deserialization, including ignored methods
        ac.resolveMemberMethods(getDeserializationMethodFilter(cfg), true);
        // include all kinds of creator methods:
        ac.resolveCreators(true);
        // yes, we need info on ignored fields as well
        ac.resolveFields(true);
        return new BasicBeanDescription(type, ac, ai);
    }

    @Override
    public BasicBeanDescription forCreation(DeserializationConfig cfg,
            Class<?> c, MixInResolver r)
    {
        AnnotationIntrospector ai = cfg.getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.construct(c, ai, r);
        ac.resolveCreators(true);
        return new BasicBeanDescription(TypeFactory.type(c), ac, ai);
    }

    @Override
    public BasicBeanDescription forClassAnnotations(MapperConfig<?> cfg,
            Class<?> c, MixInResolver r)
    {
        AnnotationIntrospector ai = cfg.getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.construct(c, ai, r);
        return new BasicBeanDescription(TypeFactory.type(c), ac, ai);
    }

    @Override
    public BasicBeanDescription forDirectClassAnnotations(MapperConfig<?> cfg,
            Class<?> c, MixInResolver r)
    {
        AnnotationIntrospector ai = cfg.getAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(c, ai, r);
        return new BasicBeanDescription(TypeFactory.type(c), ac, ai);
    }
    
    /*
    ///////////////////////////////////////////////////////
    // Overridable helper methods
    ///////////////////////////////////////////////////////
     */
    
    /**
     * Helper method for getting access to filter that only guarantees
     * that methods used for serialization are to be included.
     */
    protected MethodFilter getSerializationMethodFilter(SerializationConfig cfg)
    {
    	return GetterMethodFilter.instance;
    }

    /**
     * Helper method for getting access to filter that only guarantees
     * that methods used for deserialization are to be included.
     */
    protected MethodFilter getDeserializationMethodFilter(DeserializationConfig cfg)
    {
        /* [JACKSON-88]: may also need to include getters (at least for
         * Collection and Map types)
         */
        if (cfg.isEnabled(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS)) {
            return SetterAndGetterMethodFilter.instance;
            
        }
    	return SetterMethodFilter.instance;
    }
}
