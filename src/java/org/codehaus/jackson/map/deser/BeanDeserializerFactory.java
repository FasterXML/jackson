package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.map.*;
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

    public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        // Very first thing: do we even handle this type as a Bean?
        Class<?> beanClass = type.getRawClass();
        if (!isPotentialBeanType(beanClass)) {
            return null;
        }

        // And then: maybe it's explicitly defined by annotations?
        JsonDeserializer<Object> ad = findDeserializerByAnnotation(beanClass);
        if (ad != null) {
            return ad;
        }

        /* Ok then: let's figure out scalar value - based construction
         * aspects.
         *
         * !!! 09-Jan-2009, tatu: Should allow construction from Map
         *   (which would then be assumed to be "untyped"), iff it's
         *   marked with @JsonCreator.
         *   (same with Collections too)
         */

        ClassIntrospector intr = ClassIntrospector.forDeserialization(beanClass);

        BeanDeserializer.StringConstructor sctor = getStringCreators(beanClass, intr);
        BeanDeserializer.NumberConstructor nctor = getNumberCreators(beanClass, intr);
        Constructor<?> defaultCtor = intr.findDefaultConstructor();

        // sanity check: must have a constructor of one type or another
        if ((sctor == null) && (nctor == null) && (defaultCtor == null)) {
            throw new IllegalArgumentException("Can not create Bean deserializer for ("+type+"): neither default constructor nor factory methods found");
        }
        BeanDeserializer deser = new BeanDeserializer(type, defaultCtor, sctor, nctor);
        // And then things we need if we get Json Object:
        addBeanProps(intr, deser);
        return deser;
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
    protected void addBeanProps(ClassIntrospector intr, BeanDeserializer deser)
        throws JsonMappingException
    {
        Class<?> beanClass = deser.getBeanClass();

        LinkedHashMap<String,Method> methodsByProp = intr.findSetters();

        /* No setters? Should we proceed here? It may well be ok, if
         * there are factory methods or such.
         */
        //if (methodsByProp.isEmpty()) ...

        // These are all valid setters, but we do need to introspect bit more
        for (Map.Entry<String,Method> en : methodsByProp.entrySet()) {
            String name = en.getKey();
            Method m = en.getValue();
            // need to ensure it is callable now:
            ClassUtil.checkAndFixAccess(m, m.getDeclaringClass());

            // note: this works since we know there's exactly one arg for methods
            Type rawType = m.getGenericParameterTypes()[0];
            JavaType type = TypeFactory.instance.fromType(rawType);

            /* First: does the Method specify the deserializer to use?
             * If so, let's use it.
             */
            SettableBeanProperty prop;
            JsonDeserializer<Object> propDeser = findDeserializerByAnnotation(m);
            if (propDeser != null) {
                prop = new SettableBeanProperty(name, type, m);
                prop.setValueDeserializer(propDeser);
            } else {
                /* Otherwise, method may specify more specific (sub-)class for
                 * value (no need to check if explicit deser was specified):
                 */
                type = modifyTypeByAnnotation(m, type);
                prop = new SettableBeanProperty(name, type, m);
            }
            SettableBeanProperty oldP = deser.addSetter(prop);
            if (oldP != null) { // can this ever occur?
                throw new IllegalArgumentException("Duplicate property '"+name+"' for class "+beanClass.getName());
            }

        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer: factory methods
    ////////////////////////////////////////////////////////////
     */

    BeanDeserializer.StringConstructor getStringCreators(Class<?> beanClass,
                                                         ClassIntrospector intr)
    {
        // Single-string ctor
        Constructor<?> sctor = intr.findSingleArgConstructor(String.class);
        // and/or one of "well-known" factory methods
        Method factoryMethod = intr.findFactoryMethod(String.class);
        return new BeanDeserializer.StringConstructor(beanClass, sctor, factoryMethod);
    }

    BeanDeserializer.NumberConstructor getNumberCreators(Class<?> beanClass,
                                                         ClassIntrospector intr)
    {
        // single-arg ctors 
        Constructor<?> intCtor = intr.findSingleArgConstructor(int.class, Integer.class);
        Constructor<?> longCtor = intr.findSingleArgConstructor(long.class, Long.class);


        // and/or one of "well-known" factory methods
        Method intFactoryMethod = intr.findFactoryMethod(int.class, Integer.class);
        Method longFactoryMethod = intr.findFactoryMethod(long.class, Long.class);

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
