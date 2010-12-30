package org.codehaus.jackson.map.deser;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.*;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ArrayBuilders;
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
    protected final static Deserializers[] NO_DESERIALIZERS = new Deserializers[0];

    /**
     * Signature of <b>Throwable.initCause</b> method.
     */
    private final static Class<?>[] INIT_CAUSE_PARAMS = new Class<?>[] { Throwable.class };

    /*
    /**********************************************************
    /* Helper class to contain configuration settings
    /**********************************************************
     */

    /**
     * Configuration settings container class for bean deserializer factory
     */
    public static class Config
    {
        /**
         * List of providers for additional deserializers, checked before considering default
         * basic or bean deserialializers.
         * 
         * @since 1.7
         */
        protected final Deserializers[] _additionalDeserializers;

        protected Config() {
            this(NO_DESERIALIZERS);
        }

        protected Config(Deserializers[] allAdditionalDeserializers)
        {
            _additionalDeserializers = (allAdditionalDeserializers == null) ?
                    NO_DESERIALIZERS : allAdditionalDeserializers;
        }

        public Config withAdditionalDeserializers(Deserializers additional)
        {
            if (additional == null) {
                throw new IllegalArgumentException("Can not pass null Deserializers");
            }
            Deserializers[] all = ArrayBuilders.insertInList(_additionalDeserializers, additional);
            return new Config(all);
        }

        protected Deserializers[] deserializers() {
            return _additionalDeserializers;
        }
    }
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */
    
    /**
     * Globally shareable thread-safe instance which has no additional custom deserializers
     * registered
     */
    public final static BeanDeserializerFactory instance = new BeanDeserializerFactory(null);

    /**
     * Configuration settings for this factory; immutable instance (just like this
     * factory), new version created via copy-constructor (fluent-style)
     * 
     * @since 1.7
     */
    protected final Config _config;

    @Deprecated
    public BeanDeserializerFactory() {
        this(null);
    }

    /**
     * @since 1.7
     */
    public BeanDeserializerFactory(Config config) {
        if (config == null) {
            config = new Config();
        }
        _config = config;
    }

    /**
     * Method used by module registration functionality, to attach additional
     * deserializer providers into this deserializer factory. This is typically
     * handled by constructing a new instance with additional deserializers,
     * to ensure thread-safe access.
     * 
     * @since 1.7
     */
    @Override
    public DeserializerFactory withAdditionalDeserializers(Deserializers additional)
    {
        /* 22-Nov-2010, tatu: Handling of subtypes is tricky if we do immutable-with-copy-ctor;
         *    and we pretty much have to here either choose between losing subtype instance
         *    when registering additional deserializers, or losing deserializers.
         *    Instead, let's actually just throw an error if this method is called when subtype
         *    has not properly overridden this method; this to indicate problem as soon as possible.
         */
        if (getClass() != BeanDeserializerFactory.class) {
            throw new IllegalStateException("Subtype of BeanDeserializerFactory ("+getClass().getName()
                    +") has not properly overridden method 'withAdditionalDeserializers': can not instantiate subtype with "
                    +"additional deserializer definitions");
        }
        return new BeanDeserializerFactory(_config.withAdditionalDeserializers(additional));
    }

    /*
    /**********************************************************
    /* Overrides for super-class methods used for finding
    /* custom deserializers
    /**********************************************************
     */

    @Override
    protected JsonDeserializer<?> _findCustomArrayDeserializer(ArrayType type, DeserializationConfig config,
            DeserializerProvider provider,
            BeanProperty property,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _config.deserializers()) {
            JsonDeserializer<?> deser = d.findArrayDeserializer(type, config, provider, property,
                        elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @Override
    protected JsonDeserializer<?> _findCustomCollectionDeserializer(CollectionType type, DeserializationConfig config,
            DeserializerProvider provider, BasicBeanDescription beanDesc,
            BeanProperty property,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _config.deserializers()) {
            JsonDeserializer<?> deser = d.findCollectionDeserializer(type, config, provider, beanDesc, property,
                    elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @Override
    protected JsonDeserializer<?> _findCustomEnumDeserializer(Class<?> type, DeserializationConfig config,
            BasicBeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        for (Deserializers d  : _config.deserializers()) {
            JsonDeserializer<?> deser = d.findEnumDeserializer(type, config, beanDesc, property);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @Override
    protected JsonDeserializer<?> _findCustomMapDeserializer(MapType type, DeserializationConfig config,
            DeserializerProvider provider, BasicBeanDescription beanDesc, BeanProperty property,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _config.deserializers()) {
            JsonDeserializer<?> deser = d.findMapDeserializer(type, config, provider, beanDesc, property,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @Override
    protected JsonDeserializer<?> _findCustomTreeNodeDeserializer(Class<? extends JsonNode> type,
            DeserializationConfig config, BeanProperty property)
        throws JsonMappingException
    {
        for (Deserializers d  : _config.deserializers()) {
            JsonDeserializer<?> deser = d.findTreeNodeDeserializer(type, config, property);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    // Note: NOT overriding, superclass has no matching method
    @SuppressWarnings("unchecked")
    protected JsonDeserializer<Object> _findCustomBeanDeserializer(JavaType type, DeserializationConfig config,
            DeserializerProvider provider, BasicBeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        for (Deserializers d  : _config.deserializers()) {
            JsonDeserializer<?> deser = d.findBeanDeserializer(type, config, provider, beanDesc, property);
            if (deser != null) {
                return (JsonDeserializer<Object>) deser;
            }
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* DeserializerFactory API implementation
    /**********************************************************
     */

    /**
     * Method that {@link DeserializerProvider}s call to create a new
     * deserializer for types other than Collections, Maps, arrays and
     * enums.
     */
    @Override
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, DeserializerProvider p,
            JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        // First things first: maybe explicit definition via annotations?
        BasicBeanDescription beanDesc = config.introspect(type);
        JsonDeserializer<Object> ad = findDeserializerFromAnnotation(config, beanDesc.getClassInfo(), property);
        if (ad != null) {
            return ad;
        }
        // Or value annotation that indicates more specific type to use:
        JavaType newType =  modifyTypeByAnnotation(config, beanDesc.getClassInfo(), type, null);
        if (newType.getRawClass() != type.getRawClass()) {
            type = newType;
            beanDesc = config.introspect(type);
        }
        // We may also have custom overrides:
        JsonDeserializer<Object> custom = _findCustomBeanDeserializer(type, config, p, beanDesc, property);
        if (custom != null) {
            return custom;
        }

        /* Let's call super class first: it knows simple types for
         * which we have default deserializers
         */
        JsonDeserializer<Object> deser = super.createBeanDeserializer(config, p, type, property);
        if (deser != null) {
            return deser;
        }
        // Otherwise: could the class be a Bean class?
        if (!isPotentialBeanType(type.getRawClass())) {
            return null;
        }

        /* One more thing to check: do we have an exception type
         * (Throwable or its sub-classes)? If so, need slightly
         * different handling.
         */
        if (type.isThrowable()) {
            return buildThrowableDeserializer(config, type, beanDesc, property);
        }

        /* Abstract types can not be handled using regular bean deserializer, but need
         * either type deserializer (which caller usually deals with), or resolve
         * to a concrete type.
         */
        if (type.isAbstract()) {
            // [JACKSON-41] (v1.6): Let's make it possible to materialize abstract types.
            /* One check however: if type information is indicated, we usually do not
             * want materialization...
             */
            AbstractTypeResolver res = config.getAbstractTypeResolver();
            if (res != null) {
                AnnotationIntrospector intr = config.getAnnotationIntrospector();
                if (intr.findTypeResolver(beanDesc.getClassInfo(), type) == null) {
                    JavaType concrete = res.resolveAbstractType(config, type);
                    if (concrete != null) {
                        /* important: introspect actual implementation (abstract class or
                         * interface doesn't have constructors, for one)
                         */
                        beanDesc = config.introspect(concrete);
                        return buildBeanDeserializer(config, concrete, beanDesc, property);
                    }
                }
            }
            // otherwise we assume there must be extra type information coming
            return new AbstractDeserializer(type);
        }
        
        /* Otherwise we'll just use generic bean introspection
         * to build deserializer
         */
        return buildBeanDeserializer(config, type, beanDesc, property);
    }

    /*
    /**********************************************************
    /* Public construction method beyond DeserializerFactory API:
    /* can be called from outside as well as overridden by
    /* sub-classes
    /**********************************************************
     */

    /**
     * Method that is to actually build a bean deserializer instance.
     * All basic sanity checks have been done to know that what we have
     * may be a valid bean type, and that there are no default simple
     * deserializers.
     */
    public JsonDeserializer<Object> buildBeanDeserializer(DeserializationConfig config,
            JavaType type, BasicBeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        BeanDeserializer deser = constructBeanDeserializerInstance(config, type, beanDesc, property);

        // First: add constructors
        addDeserializerCreators(config, beanDesc, deser);
        // and check that there are enough
        /* 23-Jan-2010, tatu: can not do that any more, must allow partial
         *   handling of abstract classes with polymorphic types
         */
        //deser.validateCreators();

         // And then setters for deserializing from Json Object
        addBeanProps(config, beanDesc, deser);
        // managed/back reference fields/setters need special handling... first part
        addReferenceProperties(config, beanDesc, deser);

        return deser;
    }

    public JsonDeserializer<Object> buildThrowableDeserializer(DeserializationConfig config,
            JavaType type, BasicBeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        /* First, construct plain old bean deserializer and add
         * basic stuff
         */
        BeanDeserializer deser = constructThrowableDeserializerInstance(config, type, beanDesc, property);
        addDeserializerCreators(config, beanDesc, deser);
        /* 23-Jan-2010, tatu: can not do that any more, must allow partial
         *   handling of abstract classes with polymorphic types
         */
        //deser.validateCreators();
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
    /**********************************************************
    /* Helper methods for Bean deserializer construction,
    /* overridable by sub-classes
    /**********************************************************
     */

    /**
     * Method for construcing "empty" deserializer: overridable to allow
     * sub-classing of {@link BeanDeserializer}.
     */
    protected BeanDeserializer constructBeanDeserializerInstance(DeserializationConfig config,
            JavaType type,
            BasicBeanDescription beanDesc, BeanProperty property)
    {
        return new BeanDeserializer(beanDesc.getClassInfo(), type, property);
    }

    protected ThrowableDeserializer constructThrowableDeserializerInstance(DeserializationConfig config,
            JavaType type,
            BasicBeanDescription beanDesc, BeanProperty property)
    {
        return new ThrowableDeserializer(beanDesc.getClassInfo(), type, property);
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

        // First, let's figure out constructor/factory-based instantation
        // 23-Jan-2010, tatus: but only for concrete types
        if (beanDesc.getType().isConcrete()) {
            Constructor<?> defaultCtor = beanDesc.findDefaultConstructor();
            if (defaultCtor != null) {
                if (fixAccess) {
                    ClassUtil.checkAndFixAccess(defaultCtor);
                }
    
                deser.setDefaultConstructor(defaultCtor);
            }
        }

        // need to construct suitable visibility checker:
        VisibilityChecker<?> vchecker = config.getDefaultVisibilityChecker();
        if (!config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS)) {
            vchecker = vchecker.withCreatorVisibility(Visibility.NONE);
        }
        vchecker = config.getAnnotationIntrospector().findAutoDetectVisibility(beanDesc.getClassInfo(), vchecker);

        CreatorContainer creators =  new CreatorContainer(beanDesc.getBeanClass(), fixAccess);
        _addDeserializerConstructors(config, beanDesc, vchecker, deser, intr, creators);
        _addDeserializerFactoryMethods(config, beanDesc, vchecker, deser, intr, creators);
        deser.setCreators(creators);
    }

    protected void _addDeserializerConstructors
        (DeserializationConfig config, BasicBeanDescription beanDesc, VisibilityChecker<?> vchecker,
         BeanDeserializer deser, AnnotationIntrospector intr,
         CreatorContainer creators)
        throws JsonMappingException
    {
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
                        if (isCreator || vchecker.isCreatorVisible(ctor)) {
                            creators.addStringConstructor(ctor);
                        }
                        continue;
                    }
                    if (type == int.class || type == Integer.class) {
                        if (isCreator || vchecker.isCreatorVisible(ctor)) {
                            creators.addIntConstructor(ctor);
                        }
                        continue;
                    }
                    if (type == long.class || type == Long.class) {
                        if (isCreator || vchecker.isCreatorVisible(ctor)) {
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
                    throw new IllegalArgumentException("Argument #"+i+" of constructor "+ctor+" has no property name annotation; must have name when multiple-paramater constructor annotated as Creator");
                }
                properties[i] = constructCreatorProperty(config, beanDesc, name, i, param);
            }
            creators.addPropertyConstructor(ctor, properties);
        }
    }

    protected void _addDeserializerFactoryMethods
        (DeserializationConfig config, BasicBeanDescription beanDesc, VisibilityChecker<?> vchecker,
         BeanDeserializer deser, AnnotationIntrospector intr,        // define unusable constructor to prevent deserialization

         CreatorContainer creators)
        throws JsonMappingException
    {

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
                        if (isCreator || vchecker.isCreatorVisible(factory)) {
                            creators.addStringFactory(factory);
                        }
                        continue;
                    }
                    if (type == int.class || type == Integer.class) {
                        if (isCreator || vchecker.isCreatorVisible(factory)) {
                            creators.addIntFactory(factory);
                        }
                        continue;
                    }
                    if (type == long.class || type == Long.class) {
                        if (isCreator || vchecker.isCreatorVisible(factory)) {
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
     * bean deserializer to use.
     *<p>
     * Note: designed to be overridable, and effort is made to keep interface
     * similar between versions.
     */
    protected void addBeanProps(DeserializationConfig config,
            BasicBeanDescription beanDesc, BeanDeserializer deser)
        throws JsonMappingException
    {
        // Ok: let's aggregate visibility settings: first, baseline:
        VisibilityChecker<?> vchecker = config.getDefaultVisibilityChecker();
        // then global overrides (disabling)
        if (!config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_SETTERS)) {
            vchecker = vchecker.withSetterVisibility(Visibility.NONE);
        }
        if (!config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        // and finally per-class overrides:
        vchecker = config.getAnnotationIntrospector().findAutoDetectVisibility(beanDesc.getClassInfo(), vchecker);

        Map<String,AnnotatedMethod> setters = beanDesc.findSetters(vchecker);
        // Also, do we have a fallback "any" setter?
        AnnotatedMethod anySetter = beanDesc.findAnySetter();

        // Things specified as "ok to ignore"? [JACKSON-77]
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        boolean ignoreAny = false;
        {
            Boolean B = intr.findIgnoreUnknownProperties(beanDesc.getClassInfo());
            if (B != null) {
                ignoreAny = B.booleanValue();
                deser.setIgnoreUnknownProperties(ignoreAny);
            }
        }
        // Or explicit/implicit definitions?
        HashSet<String> ignored = ArrayBuilders.arrayToSet(intr.findPropertiesToIgnore(beanDesc.getClassInfo()));        
        // But let's only add these if we'd otherwise fail with exception (save some memory here)
        /* 03-Oct-2010, tatu: As per [JACKSON-383], we can't optimize here,
         *   since doing so may interfere with handling of @JsonAnySetter.
         */
        //if (!ignoreAny && config.isEnabled(DeserializationConfig.Feature.FAIL_ON_UNKNOWN_PROPERTIES))
        {
            for (String propName : ignored) {
                deser.addIgnorable(propName);
            }
            // Implicit ones via @JsonIgnore and equivalent?
            AnnotatedClass ac = beanDesc.getClassInfo();
            for (AnnotatedMethod am : ac.ignoredMemberMethods()) {
                String name = beanDesc.okNameForSetter(am);
                if (name != null) {
                    deser.addIgnorable(name);
                }
            }
            for (AnnotatedField af : ac.ignoredFields()) {
                deser.addIgnorable(af.getName());
            }
        }

        HashMap<Class<?>,Boolean> ignoredTypes = new HashMap<Class<?>,Boolean>();
        
        // These are all valid setters, but we do need to introspect bit more
        for (Map.Entry<String,AnnotatedMethod> en : setters.entrySet()) {
            String name = en.getKey();            
            if (!ignored.contains(name)) { // explicit ignoral using @JsonIgnoreProperties needs to block entries
                AnnotatedMethod setter = en.getValue();
                // [JACKSON-429] Some types are declared as ignorable as well
                Class<?> type = setter.getParameterClass(0);
                if (isIgnorableType(config, beanDesc, type, ignoredTypes)) {
                    // important: make ignorable, to avoid errors if value is actually seen
                    deser.addIgnorable(name);
                    continue;
                }
                SettableBeanProperty prop = constructSettableProperty(config, beanDesc, name, setter);
                if (prop != null) {
                    deser.addProperty(prop);
                }
            }
        }
        if (anySetter != null) {
            deser.setAnySetter(constructAnySetter(config, beanDesc, anySetter));
        }

        HashSet<String> addedProps = new HashSet<String>(setters.keySet());
        /* [JACKSON-98]: also include field-backed properties:
         *   (second arg passed to ignore anything for which there is a getter
         *   method)
         */
        LinkedHashMap<String,AnnotatedField> fieldsByProp = beanDesc.findDeserializableFields(vchecker, addedProps);
        for (Map.Entry<String,AnnotatedField> en : fieldsByProp.entrySet()) {
            String name = en.getKey();
            if (!ignored.contains(name) && !deser.hasProperty(name)) {
                AnnotatedField field = en.getValue();
                // [JACKSON-429] Some types are declared as ignorable as well
                Class<?> type = field.getRawType();
                if (isIgnorableType(config, beanDesc, type, ignoredTypes)) {
                    // important: make ignorable, to avoid errors if value is actually seen
                    deser.addIgnorable(name);
                    continue;
                }
                SettableBeanProperty prop = constructSettableProperty(config, beanDesc, name, field);
                if (prop != null) {
                    deser.addProperty(prop);
                    addedProps.add(name);
                }
            }
        }

        /* As per [JACKSON-88], may also need to consider getters
         * for Map/Collection properties
         */
        /* also, as per [JACKSON-328], should not override fields (or actual setters),
         * thus these are added AFTER adding fields
         */
        if (config.isEnabled(DeserializationConfig.Feature.USE_GETTERS_AS_SETTERS)) {
            /* Hmmh. We have to assume that 'use getters as setters' also
             * implies 'yes, do auto-detect these getters'? (if not, we'd
             * need to add AUTO_DETECT_GETTERS to deser config too, not
             * just ser config)
             */
            Map<String,AnnotatedMethod> getters = beanDesc.findGetters(vchecker, addedProps);

            for (Map.Entry<String,AnnotatedMethod> en : getters.entrySet()) {
                AnnotatedMethod getter = en.getValue();
                // should only consider Collections and Maps, for now?
                Class<?> rt = getter.getRawType();
                if (!Collection.class.isAssignableFrom(rt) && !Map.class.isAssignableFrom(rt)) {
                    continue;
                }
                String name = en.getKey();
                if (!ignored.contains(name) && !deser.hasProperty(name)) {
                    deser.addProperty(constructSetterlessProperty(config, beanDesc, name, getter));
                    addedProps.add(name);
                }
            }
        }
        
    }

    /**
     * Method that will find if bean has any managed- or back-reference properties,
     * and if so add them to bean, to be linked during resolution phase.
     * 
     * @since 1.6
     */
    protected void addReferenceProperties(DeserializationConfig config,
            BasicBeanDescription beanDesc, BeanDeserializer deser)
        throws JsonMappingException
    {
        // and then back references, not necessarily found as regular properties
        Map<String,AnnotatedMember> refs = beanDesc.findBackReferenceProperties();
        if (refs != null) {
            for (Map.Entry<String, AnnotatedMember> en : refs.entrySet()) {
                String name = en.getKey();
                AnnotatedMember m = en.getValue();
                if (m instanceof AnnotatedMethod) {
                    deser.addBackReferenceProperty(name, constructSettableProperty(
                            config, beanDesc, m.getName(), (AnnotatedMethod) m));
                } else {
                    deser.addBackReferenceProperty(name, constructSettableProperty(
                            config, beanDesc, m.getName(), (AnnotatedField) m));
                }
            }
        }
    }
        
    /**
     * Method called to construct fallback {@link SettableAnyProperty}
     * for handling unknown bean properties, given a method that
     * has been designated as such setter.
     */
    protected SettableAnyProperty constructAnySetter(DeserializationConfig config,
            BasicBeanDescription beanDesc, AnnotatedMethod setter)
        throws JsonMappingException
    {
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            setter.fixAccess(); // to ensure we can call it
        }
        // we know it's a 2-arg method, second arg is the value
        JavaType type = TypeFactory.type(setter.getParameterType(1), beanDesc.bindingsForBeanType());
        BeanProperty.Std property = new BeanProperty.Std(setter.getName(), type, beanDesc.getClassAnnotations(), setter);
        type = resolveType(config, beanDesc, type, setter, property);

        /* AnySetter can be annotated with @JsonClass (etc) just like a
         * regular setter... so let's see if those are used.
         * Returns null if no annotations, in which case binding will
         * be done at a later point.
         */
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(config, setter, property);
        if (deser != null) {
            SettableAnyProperty prop = new SettableAnyProperty(property, setter, type);
            prop.setValueDeserializer(deser);
            return prop;
        }
        /* Otherwise, method may specify more specific (sub-)class for
         * value (no need to check if explicit deser was specified):
         */
        type = modifyTypeByAnnotation(config, setter, type, property.getName());
        return new SettableAnyProperty(property, setter, type);
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
            BasicBeanDescription beanDesc, String name,
            AnnotatedMethod setter)
        throws JsonMappingException
    {
        // need to ensure method is callable (for non-public)
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            setter.fixAccess();
        }

        // note: this works since we know there's exactly one arg for methods
        JavaType t0 = TypeFactory.type(setter.getParameterType(0), beanDesc.bindingsForBeanType());
        BeanProperty.Std property = new BeanProperty.Std(name, t0, beanDesc.getClassAnnotations(), setter);
        JavaType type = resolveType(config, beanDesc, t0, setter, property);
        // did type change?
        if (type != t0) {
            property = property.withType(type);
        }
        
        /* First: does the Method specify the deserializer to use?
         * If so, let's use it.
         */
        JsonDeserializer<Object> propDeser = findDeserializerFromAnnotation(config, setter, property);
        type = modifyTypeByAnnotation(config, setter, type, name);
        TypeDeserializer typeDeser = type.getTypeHandler();
        SettableBeanProperty prop = new SettableBeanProperty.MethodProperty(name, type, typeDeser,
                beanDesc.getClassAnnotations(), setter);
        if (propDeser != null) {
            prop.setValueDeserializer(propDeser);
        }
        // [JACKSON-235]: need to retain name of managed forward references:
        AnnotationIntrospector.ReferenceProperty ref = config.getAnnotationIntrospector().findReferenceType(setter);
        if (ref != null && ref.isManagedReference()) {
            prop.setManagedReferenceName(ref.getName());
        }
        return prop;
    }

    protected SettableBeanProperty constructSettableProperty(DeserializationConfig config,
            BasicBeanDescription beanDesc, String name, AnnotatedField field)
        throws JsonMappingException
    {
        // need to ensure method is callable (for non-public)
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            field.fixAccess();
        }
        JavaType t0 = TypeFactory.type(field.getGenericType(), beanDesc.bindingsForBeanType());
        BeanProperty.Std property = new BeanProperty.Std(name, t0, beanDesc.getClassAnnotations(), field);
        JavaType type = resolveType(config, beanDesc, t0, field, property);
        // did type change?
        if (type != t0) {
            property = property.withType(type);
        }
        /* First: does the Method specify the deserializer to use?
         * If so, let's use it.
         */
        JsonDeserializer<Object> propDeser = findDeserializerFromAnnotation(config, field, property);
        type = modifyTypeByAnnotation(config, field, type, name);
        TypeDeserializer typeDeser = type.getTypeHandler();
        SettableBeanProperty prop = new SettableBeanProperty.FieldProperty(name, type, typeDeser,
                beanDesc.getClassAnnotations(), field);
        if (propDeser != null) {
            prop.setValueDeserializer(propDeser);
        }
        // [JACKSON-235]: need to retain name of managed forward references:
        AnnotationIntrospector.ReferenceProperty ref = config.getAnnotationIntrospector().findReferenceType(field);
        if (ref != null && ref.isManagedReference()) {
            prop.setManagedReferenceName(ref.getName());
        }
        return prop;
    }

    /**
     * Method that will construct a regular bean property setter using
     * the given setter method.
     *
     * @param getter Method to use to get property value to modify, null if
     *    none. Non-null for "setterless" properties.
     */
    protected SettableBeanProperty constructSetterlessProperty(DeserializationConfig config,
            BasicBeanDescription beanDesc, String name, AnnotatedMethod getter)
        throws JsonMappingException
    {
        // need to ensure it is callable now:
        if (config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            getter.fixAccess();
        }

        JavaType type = getter.getType(beanDesc.bindingsForBeanType());
        /* First: does the Method specify the deserializer to use?
         * If so, let's use it.
         */
        BeanProperty.Std property = new BeanProperty.Std(name, type, beanDesc.getClassAnnotations(), getter);
        // @TODO: create BeanProperty to pass?
        JsonDeserializer<Object> propDeser = findDeserializerFromAnnotation(config, getter, property);
        type = modifyTypeByAnnotation(config, getter, type, name);
        TypeDeserializer typeDeser = type.getTypeHandler();
        SettableBeanProperty prop = new SettableBeanProperty.SetterlessProperty(name, type, typeDeser,
                beanDesc.getClassAnnotations(), getter);
        if (propDeser != null) {
            prop.setValueDeserializer(propDeser);
        }
        return prop;
    }

    /*
    /**********************************************************
    /* Helper methods for Bean deserializer, other
    /**********************************************************
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

    /**
     * Helper method that will check whether given raw type is marked as always ignorable
     * (for purpose of ignoring properties with type)
     */
    protected boolean isIgnorableType(DeserializationConfig config, BasicBeanDescription beanDesc,
            Class<?> type, Map<Class<?>,Boolean> ignoredTypes)
    {
        Boolean status = ignoredTypes.get(type);
        if (status == null) {
            BasicBeanDescription desc = config.introspectClassAnnotations(type);
            status = config.getAnnotationIntrospector().isIgnorableType(desc.getClassInfo());
            // We default to 'false', ie. not ignorable
            if (status == null) {
                status = Boolean.FALSE;
            }
        }
        return status;
    }
}
