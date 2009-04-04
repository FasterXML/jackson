package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;

/**
 * Concrete deserializer factory class that adds full Bean deserializer
 * construction logic using class introspection.
 *<p>
 * Since there is no caching, this factory is stateless and a globally
 * shared singleton instance ({@link #instance}) can be  used by
 * {@link DeserializerProvider}s).
 */
public class BeanDeserializerFactory
    extends BasicDeserializerFactory
{
    final static Class<?>[] INIT_CAUSE_PARAMS = new Class<?>[] { Throwable.class };

    public final static BeanDeserializerFactory instance = new BeanDeserializerFactory();

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected BeanDeserializerFactory() { super(); }

    /*
    ///////////////////////////////////////////////////////////
    // Implementations of remaining DeserializerFactory API
    ///////////////////////////////////////////////////////////
     */

    @Override
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        /* Let's call super class first: it knows simple types for
         * which we have default deserializers
         */
        JsonDeserializer<Object> deser = super.createBeanDeserializer(config, type, p);
        if (deser != null) {
            return deser;
        }

        // Otherwise: could the class be a Bean class?
        Class<?> beanClass = type.getRawClass();
        if (!isPotentialBeanType(beanClass)) {
            return null;
        }

        /* Ok then: let's figure out scalar value - based construction
         * aspects.
         *
         * !!! 09-Jan-2009, tatu: Should allow construction from Map
         *   (which would then be assumed to be "untyped") factory method,
         *   iff it's marked with @JsonCreator.
         *   (same with Collections?)
         */

        BasicBeanDescription beanDesc = config.introspect(beanClass);
        // maybe it's explicitly defined by annotations?
        JsonDeserializer<Object> ad = findDeserializerFromAnnotation(beanDesc.getClassInfo());
        if (ad != null) {
            return ad;
        }

        /* 02-Mar-2009, tatu: Can't instantiate abstract classes or interfaces
         *   so now might be a good time to catch that problem...
         */
        if (!ClassUtil.isConcrete(beanClass)) {
            return null;
        }

        BeanDeserializer.StringConstructor sctor = getStringCreators(beanClass, beanDesc);
        BeanDeserializer.NumberConstructor nctor = getNumberCreators(beanClass, beanDesc);
        Constructor<?> defaultCtor = beanDesc.findDefaultConstructor();

        // sanity check: must have a constructor of one type or another
        if ((sctor == null) && (nctor == null) && (defaultCtor == null)) {
            throw new IllegalArgumentException("Can not create Bean deserializer for ("+type+"): neither default constructor nor factory methods found");
        }
        BeanDeserializer bd = new BeanDeserializer(type, defaultCtor, sctor, nctor);
        // And then things we need if we get Json Object:
        addBeanProps(beanDesc, bd);
        return bd;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer: property handling
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method called to figure out settable properties for the
     * deserializer.
     */
    protected void addBeanProps(BasicBeanDescription beanDesc, BeanDeserializer deser)
        throws JsonMappingException
    {
        Class<?> beanClass = beanDesc.getBeanClass();

        Map<String,AnnotatedMethod> methodsByProp = beanDesc.findSetters();
        // May need to add other "non-traditional" setters?
        addCustomSetters(beanDesc, methodsByProp);

        // Also, do we have a fallback "any" setter? If so, need to bind
        {
            AnnotatedMethod anyM = beanDesc.findAnySetter();
            if (anyM != null) {
                deser.setAnySetter(constructAnySetter(anyM));
            }
        }

        /* No setters? Should we proceed here? It may well be ok, if
         * there are factory methods or such.
         */
        //if (methodsByProp.isEmpty() && anySetter == null) ...

        // These are all valid setters, but we do need to introspect bit more
        for (Map.Entry<String,AnnotatedMethod> en : methodsByProp.entrySet()) {
            String name = en.getKey();
            AnnotatedMethod am = en.getValue();
            // need to ensure it is callable now:
            am.fixAccess();

            // note: this works since we know there's exactly one arg for methods
            Type rawType = am.getGenericParameterTypes()[0];
            JavaType type = TypeFactory.fromType(rawType);

            /* First: does the Method specify the deserializer to use?
             * If so, let's use it.
             */
            SettableBeanProperty prop;
            JsonDeserializer<Object> propDeser = findDeserializerFromAnnotation(am);

            Method m = am.getAnnotated();
            if (propDeser != null) {
                prop = new SettableBeanProperty(name, type, m);
                prop.setValueDeserializer(propDeser);
            } else {
                /* Otherwise, method may specify more specific (sub-)class for
                 * value (no need to check if explicit deser was specified):
                 */
                type = modifyTypeByAnnotation(am, type);
                prop = new SettableBeanProperty(name, type, m);
            }
            SettableBeanProperty oldP = deser.addSetter(prop);
            if (oldP != null) { // can this ever occur?
                throw new IllegalArgumentException("Duplicate property '"+name+"' for class "+beanClass.getName());
            }
        }

        // and we may also want to do some other post-processing?
        addIgnorableProperties(beanDesc, deser);
    }

    /**
     * Method called to allow adding other custom setters beyond ones
     * that were introspected using naming convention or annotations.
     */
    protected void addCustomSetters(BasicBeanDescription beanDesc, Map<String, AnnotatedMethod> methods)
    {
        Class<?> cls = beanDesc.getBeanClass();

        /* To resolve [JACKSON-95], need to add "initCause" as setter
         * for exceptions (sub-classes of Throwable).
         */
        if (Throwable.class.isAssignableFrom(cls)) {
            AnnotatedMethod m = beanDesc.findMethod("initCause", INIT_CAUSE_PARAMS);
            if (m != null) {
                methods.put("cause", m);
            }
        }
    }

    protected void addIgnorableProperties(BasicBeanDescription beanDesc, BeanDeserializer deser)
    {
        Class<?> cls = beanDesc.getBeanClass();
        /* To resolve [JACKSON-95], need to mark property "localizedMessage"
         * as ignorable; there's no way to inject that in
         */
        if (Throwable.class.isAssignableFrom(cls)) {
            deser.addIgnorable("localizedMessage");
            // !!! TEST: also ignore "message", for now
            deser.addIgnorable("message");
        }
    }

    /**
     * Method called to construct fallback {@link SettableAnyProperty}
     * for handling unknown bean properties, given a method that
     * has been designated as such setter.
     */
    protected SettableAnyProperty constructAnySetter(AnnotatedMethod am)
        throws JsonMappingException
    {
        am.fixAccess(); // to ensure we can call it
        /* AnySetter can be annotated with @JsonClass (etc) just like a
         * regular setter... so let's see if those are used.
         * Returns null if no annotations, in which case binding will
         * be done at a later point.
         */
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(am);
        // we know it's a 2-arg method, second arg is the vlaue
        Type rawType = am.getGenericParameterTypes()[1];
        JavaType type = TypeFactory.fromType(rawType);
        Method m = am.getAnnotated();
        if (deser != null) {
            SettableAnyProperty prop = new SettableAnyProperty(type, m);
            prop.setValueDeserializer(deser);
            return prop;
        }
        /* Otherwise, method may specify more specific (sub-)class for
         * value (no need to check if explicit deser was specified):
         */
        type = modifyTypeByAnnotation(am, type);
        return new SettableAnyProperty(type, m);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer: factory methods
    ////////////////////////////////////////////////////////////
     */

    BeanDeserializer.StringConstructor getStringCreators(Class<?> beanClass,
                                                         BasicBeanDescription beanDesc)
    {
        // Single-string ctor
        Constructor<?> sctor = beanDesc.findSingleArgConstructor(String.class);
        // and/or one of "well-known" factory methods
        Method factoryMethod = beanDesc.findFactoryMethod(String.class);
        return new BeanDeserializer.StringConstructor(beanClass, sctor, factoryMethod);
    }

    BeanDeserializer.NumberConstructor getNumberCreators(Class<?> beanClass,
                                                         BasicBeanDescription beanDesc)
    {
        // single-arg ctors 
        Constructor<?> intCtor = beanDesc.findSingleArgConstructor(int.class, Integer.class);
        Constructor<?> longCtor = beanDesc.findSingleArgConstructor(long.class, Long.class);


        // and/or one of "well-known" factory methods
        Method intFactoryMethod = beanDesc.findFactoryMethod(int.class, Integer.class);
        Method longFactoryMethod = beanDesc.findFactoryMethod(long.class, Long.class);

        return new BeanDeserializer.NumberConstructor(beanClass, intCtor, longCtor, intFactoryMethod, longFactoryMethod);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer, other
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
        String typeStr = ClassUtil.canBeABeanType(type);
        if (typeStr != null) {
            throw new IllegalArgumentException("Can not deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
        if (ClassUtil.isProxyType(type)) {
            throw new IllegalArgumentException("Can not deserialize Proxy class "+type.getName()+" as a Bean");
        }
        // also: can't deserialize local (in-method, anonymous, non-static-enclosed) classes
        typeStr = ClassUtil.isLocalType(type);
        if (typeStr != null) {
            throw new IllegalArgumentException("Can not deserialize Class "+type.getName()+" (of type "+typeStr+") as a Bean");
        }
    	return true;
    }
}
