package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.*;
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
    /**
     * Signature of <b>Throwable.initCause</b> method.
     */
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
    // DeserializerFactory API implementation
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method that {@link DeserializerProvider}s call to create a new
     * deserializer for types other than Collections, Maps, arrays and
     * enums.
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
        BasicBeanDescription beanDesc = config.introspect(type);
        // maybe it's explicitly defined by annotations?
        JsonDeserializer<Object> ad = findDeserializerFromAnnotation(config, beanDesc.getClassInfo());
        if (ad != null) {
            return ad;
        }
        /* 02-Mar-2009, tatu: Can't instantiate abstract classes or interfaces
         *   so now might be a good time to catch that problem...
         */
        if (!ClassUtil.isConcrete(beanClass)) {
            return null;
        }

        /* One more thing to check: do we have an exception type
         * (Throwable or its sub-classes)? If so, need slightly
         * different handling.
         */
        if (Throwable.class.isAssignableFrom(beanClass)) {
            return buildThrowableDeserializer(config, type, beanDesc);
        }

        /* Otherwise we'll just use generic bean introspection
         * to build deserializer
         */
        return buildBeanDeserializer(config, type, beanDesc);
    }

    /*
    ///////////////////////////////////////////////////////////
    // Public construction method beyond DeserializerFactory API:
    // can be called from outside as well as overridden by
    // sub-classes
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method that is to actually build a bean deserializer instance.
     * All basic sanity checks have been done to know that what we have
     * may be a valid bean type, and that there are no default simple
     * deserializers.
     */
    public JsonDeserializer<Object> buildBeanDeserializer(DeserializationConfig config,
                                                          JavaType type,
                                                          BasicBeanDescription beanDesc)
        throws JsonMappingException
    {
        BeanDeserializer deser = constructBeanDeserializerInstance(config, type, beanDesc);

        // First: add constructors
        addDeserializerCreators(config, beanDesc, deser);
        // and check that there are enough
        deser.validateCreators();

         // And then setters for deserializing from Json Object
        addBeanProps(config, beanDesc, deser);

        return deser;
    }

    public JsonDeserializer<Object> buildThrowableDeserializer(DeserializationConfig config,
                                                              JavaType type,
                                                              BasicBeanDescription beanDesc)
        throws JsonMappingException
    {
        /* First, construct plain old bean deserializer and add
         * basic stuff
         */
        BeanDeserializer deser = constructThrowableDeserializerInstance(config, type, beanDesc);
        addDeserializerCreators(config, beanDesc, deser);
        deser.validateCreators();
        addBeanProps(config, beanDesc, deser);

        /* But then let's decorate things a bit
         */
        /* To resolve [JACKSON-95], need to add "initCause" as setter
         * for exceptions (sub-classes of Throwable).
         */
        AnnotatedMethod am = beanDesc.findMethod("initCause", INIT_CAUSE_PARAMS);
        if (am != null) { // should never be null
            SettableBeanProperty prop = constructSettableProperty(config, beanDesc, "cause", am);
            if (prop != null) {
                deser.addProperty(prop);
            }
        }

        // And also need to ignore "localizedMessage"
        deser.addIgnorable("localizedMessage");
        /* As well as "message": it will be passed via constructor,
         * as there's no 'setMessage()' method
        */
        deser.addIgnorable("message");

        // And finally: make String constructor the thing we need...?

        return deser;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for Bean deserializer construction,
    // overridable by sub-classes
    ////////////////////////////////////////////////////////////
     */

    /**
     * Method for construcing "empty" deserializer: overridable to allow
     * sub-classing of {@link BeanDeserializer}.
     */
    protected BeanDeserializer constructBeanDeserializerInstance(DeserializationConfig config,
                                                                JavaType type,
                                                                BasicBeanDescription beanDesc)
    {
        return new BeanDeserializer(type);
    }

    protected ThrowableDeserializer constructThrowableDeserializerInstance(DeserializationConfig config,
                                                                           JavaType type,
                                                                           BasicBeanDescription beanDesc)
    {
        return new ThrowableDeserializer(type);
    }

    /**
     * Method that is to find all creators (constructors, factory methods)
     * for the bean type to deserialize.
     */
    protected void addDeserializerCreators(DeserializationConfig config,
                                           BasicBeanDescription beanDesc,
                                           BeanDeserializer deser)
        throws JsonMappingException
    {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        boolean fixAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);

        // First, let's figure out constructor/factor- based instantation
        Constructor<?> defaultCtor = beanDesc.findDefaultConstructor();
        if (defaultCtor != null) {
            if (fixAccess) {
                ClassUtil.checkAndFixAccess(defaultCtor);
            }

            deser.setDefaultConstructor(defaultCtor);
        }

        CreatorContainer creators =  new CreatorContainer(beanDesc.getBeanClass(), fixAccess);
        _addDeserializerConstructors(config, beanDesc, deser, intr, creators);
        _addDeserializerFactoryMethods(config, beanDesc, deser, intr, creators);
        deser.setCreators(creators);
    }

    protected void _addDeserializerConstructors
        (DeserializationConfig config, BasicBeanDescription beanDesc,
         BeanDeserializer deser, AnnotationIntrospector intr,
         CreatorContainer creators)
        throws JsonMappingException
    {
        boolean autodetect = config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS);

        for (AnnotatedConstructor ctor : beanDesc.getConstructors()) {
            int argCount = ctor.getParameterCount();
            if (argCount < 1) {
                continue;
            }
            boolean isCreator = intr.hasCreatorAnnotation(ctor);
            // some single-arg constructors (String, number) are auto-detected
            if (argCount == 1) {
                /* but note: if we do have parameter name, it'll be
                 * "property constructor", and needs to be skipped for now
                 */
                String name = intr.findPropertyNameForParam(ctor.getParameter(0));
                if (name == null || name.length() == 0) { // not property based
                    Class<?> type = ctor.getParameterClass(0);
                    if (type == String.class) {
                        if (autodetect || isCreator) {
                            creators.addStringConstructor(ctor);
                        }
                        continue;
                    }
                    if (type == int.class || type == Integer.class) {
                        if (autodetect || isCreator) {
                            creators.addIntConstructor(ctor);
                        }
                        continue;
                    }
                    if (type == long.class || type == Long.class) {
                        if (autodetect || isCreator) {
                            creators.addLongConstructor(ctor);
                        }
                        continue;
                    }
                    // Delegating constructor ok iff it has @JsonCreator (etc)
                    if (intr.hasCreatorAnnotation(ctor)) {
                        creators.addDelegatingConstructor(ctor);
                    }
                    // otherwise just ignored
                    continue;
                }
                // fall through if there's name
            } else {
                // more than 2 args, must be @JsonCreator
                if (!intr.hasCreatorAnnotation(ctor)) {
                    continue;
                }
            }

            // 1 or more args; all params must have name annotations
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            for (int i = 0; i < argCount; ++i) {
                AnnotatedParameter param = ctor.getParameter(i);
                String name = (param == null) ? null : intr.findPropertyNameForParam(param);
                // At this point, name annotation is NOT optional
                if (name == null || name.length() == 0) {
                    throw new IllegalArgumentException("Argument #"+i+" of constructor "+ctor+" has no property name annotation; must have when multiple-paramater constructor annotated as Creator");
                }
                properties[i] = constructCreatorProperty(config, beanDesc, name, i, param);
            }
            creators.addPropertyConstructor(ctor, properties);
        }
    }

    protected void _addDeserializerFactoryMethods
        (DeserializationConfig config, BasicBeanDescription beanDesc,
         BeanDeserializer deser, AnnotationIntrospector intr,
         CreatorContainer creators)
        throws JsonMappingException
    {
        // and/or single-arg static methods
        boolean autodetect = config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS);

        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            int argCount = factory.getParameterCount();
            if (argCount < 1) {
                continue;
            }
            boolean isCreator = intr.hasCreatorAnnotation(factory);
            // some single-arg factory methods (String, number) are auto-detected
            if (argCount == 1) {
                /* but as above: if we do have parameter name, it'll be
                 * "property constructor", and needs to be skipped for now
                 */
                String name = intr.findPropertyNameForParam(factory.getParameter(0));
                if (name == null || name.length() == 0) { // not property based
                    Class<?> type = factory.getParameterClass(0);
                    if (type == String.class) {
                        if (autodetect || isCreator) {
                            creators.addStringFactory(factory);
                        }
                        continue;
                    }
                    if (type == int.class || type == Integer.class) {
                        if (autodetect || isCreator) {
                            creators.addIntFactory(factory);
                        }
                        continue;
                    }
                    if (type == long.class || type == Long.class) {
                        if (autodetect || isCreator) {
                            creators.addLongFactory(factory);
                        }
                        continue;
                    }
                    if (intr.hasCreatorAnnotation(factory)) {
                        creators.addDelegatingFactory(factory);
                    }
                    // otherwise just ignored
                    continue;
                }
                // fall through if there's name
            } else {
                // more than 2 args, must be @JsonCreator
                if (!intr.hasCreatorAnnotation(factory)) {
                    continue;
                }
            }
            // 1 or more args; all params must have name annotations
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            for (int i = 0; i < argCount; ++i) {
                AnnotatedParameter param = factory.getParameter(i);
                String name = intr.findPropertyNameForParam(param);
                // At this point, name annotation is NOT optional
                if (name == null || name.length() == 0) {
                    throw new IllegalArgumentException("Argument #"+i+" of factory method "+factory+" has no property name annotation; must have when multiple-paramater static method annotated as Creator");
                }
                properties[i] = constructCreatorProperty(config, beanDesc, name, i, param);
            }
            creators.addPropertyFactory(factory, properties);
        }
    }
    
    /**
     * Method called to figure out settable properties for the
     * deserializer.
     */

    protected void addBeanProps(DeserializationConfig config,
                                BasicBeanDescription beanDesc, BeanDeserializer deser)
        throws JsonMappingException
    {
        boolean autodetect = config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS);
        Map<String,AnnotatedMethod> setters = beanDesc.findSetters(autodetect);
        // Also, do we have a fallback "any" setter? If so, need to bind
        {
            AnnotatedMethod anyM = beanDesc.findAnySetter();
            if (anyM != null) {
                deser.setAnySetter(constructAnySetter(config, anyM));
            }
        }

        /* No setters? Should we proceed here? It may well be ok, if
         * there are factory methods or such.
         */
        //if (setters.isEmpty() && anySetter == null) ...

        // These are all valid setters, but we do need to introspect bit more
        for (Map.Entry<String,AnnotatedMethod> en : setters.entrySet()) {
            SettableBeanProperty prop = constructSettableProperty(config, beanDesc, en.getKey(), en.getValue());
            if (prop != null) {
                deser.addProperty(prop);
            }
        }

        /* As per [JACKSON-88], may also need to consider getters
         * for Map/Collection properties
         */
        HashSet<String> addedProps = new HashSet<String>(setters.keySet());
        if (config.isEnabled(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS)) {
            /* Hmmh. We have to assume that 'use getters as setters' also
             * implies 'yes, do auto-detect these getters'? (if not, we'd
             * need to add AUTO_DETECT_GETTERS to deser config too, not
             * just ser config)
             */
            Map<String,AnnotatedMethod> getters = beanDesc.findGetters(true, false, addedProps);
            for (Map.Entry<String,AnnotatedMethod> en : getters.entrySet()) {
                AnnotatedMethod getter = en.getValue();
                // should only consider Collections and Maps, for now?
                Class<?> rt = getter.getReturnType();
                if (Collection.class.isAssignableFrom(rt)
                    || Map.class.isAssignableFrom(rt)) {
                    String name = en.getKey();
                    deser.addProperty(constructSetterlessProperty(config, name, getter));
                    addedProps.add(name);
                }
            }
        }
        
        /* [JACKSON-98]: also include field-backed properties:
         *   (second arg passed to ignore anything for which there is a getter
         *   method)
         */
        LinkedHashMap<String,AnnotatedField> fieldsByProp = beanDesc.findDeserializableFields(config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_FIELDS), addedProps);
        for (Map.Entry<String,AnnotatedField> en : fieldsByProp.entrySet()) {
            SettableBeanProperty prop = constructSettableProperty(config, beanDesc, en.getKey(), en.getValue());
            if (prop != null) {
                deser.addProperty(prop);
            }
        }
    }

    /**
     * Method called to construct fallback {@link SettableAnyProperty}
     * for handling unknown bean properties, given a method that
     * has been designated as such setter.
     */
    protected SettableAnyProperty constructAnySetter(DeserializationConfig config,
                                                     AnnotatedMethod am)
        throws JsonMappingException
    {
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            am.fixAccess(); // to ensure we can call it
        }
        /* AnySetter can be annotated with @JsonClass (etc) just like a
         * regular setter... so let's see if those are used.
         * Returns null if no annotations, in which case binding will
         * be done at a later point.
         */
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(config, am);
        // we know it's a 2-arg method, second arg is the vlaue
        Type rawType = am.getParameterType(1);
        JavaType type = TypeFactory.type(rawType);
        Method m = am.getAnnotated();
        if (deser != null) {
            SettableAnyProperty prop = new SettableAnyProperty(type, m);
            prop.setValueDeserializer(deser);
            return prop;
        }
        /* Otherwise, method may specify more specific (sub-)class for
         * value (no need to check if explicit deser was specified):
         */
        type = modifyTypeByAnnotation(config, am, type);
        return new SettableAnyProperty(type, m);
    }

    /**
     * Method that will construct a regular bean property setter using
     * the given setter method.
     *
     * @param setter Method to use to set property value; or null if none.
     *    Null only for "setterless" properties
     *
     * @return Property constructed, if any; or null to indicate that
     *   there should be no property based on given definitions.
     */
    protected SettableBeanProperty constructSettableProperty(DeserializationConfig config,
                                                             BasicBeanDescription beanDesc,
                                                             String name,
                                                             AnnotatedMethod setter)
        throws JsonMappingException
    {
        // need to ensure method is callable (for non-public)
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            setter.fixAccess();
        }

        // note: this works since we know there's exactly one arg for methods
        JavaType type = resolveType(config, beanDesc, setter.getParameterType(0), setter);
        
        /* First: does the Method specify the deserializer to use?
         * If so, let's use it.
         */
        JsonDeserializer<Object> propDeser = findDeserializerFromAnnotation(config, setter);
        
        Method m = setter.getAnnotated();
        if (propDeser != null) {
            SettableBeanProperty prop = new SettableBeanProperty.MethodProperty(name, type, m);
            prop.setValueDeserializer(propDeser);
            return prop;
        }
        /* Otherwise, method may specify more specific (sub-)class for
         * value (no need to check if explicit deser was specified):
         */
        type = modifyTypeByAnnotation(config, setter, type);
        return new SettableBeanProperty.MethodProperty(name, type, m);
    }

    protected SettableBeanProperty constructSettableProperty(DeserializationConfig config,
                                                             BasicBeanDescription beanDesc,
                                                             String name,
                                                             AnnotatedField field)
        throws JsonMappingException
    {
        // need to ensure method is callable (for non-public)
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            field.fixAccess();
        }
        JavaType type = resolveType(config, beanDesc, field.getGenericType(), field);
        /* First: does the Method specify the deserializer to use?
         * If so, let's use it.
         */
        JsonDeserializer<Object> propDeser = findDeserializerFromAnnotation(config, field);
        
        Field f = field.getAnnotated();
        if (propDeser != null) {
            SettableBeanProperty prop = new SettableBeanProperty.FieldProperty(name, type, f);
            prop.setValueDeserializer(propDeser);
            return prop;
        }
        // Otherwise, method may specify more specific (sub-)class for
        // value (no need to check if explicit deser was specified):
        type = modifyTypeByAnnotation(config, field, type);
        return new SettableBeanProperty.FieldProperty(name, type, f);
    }

    /**
     * Method that will construct a regular bean property setter using
     * the given setter method.
     *
     * @param getter Method to use to get property value to modify, null if
     *    none. Non-null for "setterless" properties.
     */
    protected SettableBeanProperty constructSetterlessProperty(DeserializationConfig config,
                                                               String name,
                                                               AnnotatedMethod getter)
        throws JsonMappingException
    {
        // need to ensure it is callable now:
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            getter.fixAccess();
        }

        // note: this works since we know there's exactly one arg for methods
        JavaType type = TypeFactory.type(getter.getGenericReturnType());
        /* First: does the Method specify the deserializer to use?
         * If so, let's use it.
         */
        JsonDeserializer<Object> propDeser = findDeserializerFromAnnotation(config, getter);
        
        Method m = getter.getAnnotated();
        if (propDeser != null) {
            SettableBeanProperty prop = new SettableBeanProperty.SetterlessProperty(name, type, m);
            prop.setValueDeserializer(propDeser);
            return prop;
        }
        /* Otherwise, method may specify more specific (sub-)class for
         * value (no need to check if explicit deser was specified):
         */
        type = modifyTypeByAnnotation(config, getter, type);
        return new SettableBeanProperty.SetterlessProperty(name, type, m);
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
