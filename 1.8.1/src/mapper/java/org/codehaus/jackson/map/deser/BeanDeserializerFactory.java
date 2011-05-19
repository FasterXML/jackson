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
    /**
     * Signature of <b>Throwable.initCause</b> method.
     */
    private final static Class<?>[] INIT_CAUSE_PARAMS = new Class<?>[] { Throwable.class };

    /*
    /**********************************************************
    /* Config class implementation
    /**********************************************************
     */
    
    /**
     * Standard configuration settings container class implementation.
     * 
     * @since 1.7
     */
    public static class ConfigImpl extends Config
    {
        protected final static KeyDeserializers[] NO_KEY_DESERIALIZERS = new KeyDeserializers[0];
        protected final static BeanDeserializerModifier[] NO_MODIFIERS = new BeanDeserializerModifier[0];
        protected final static AbstractTypeResolver[] NO_ABSTRACT_TYPE_RESOLVERS = new AbstractTypeResolver[0];
        
        /**
         * List of providers for additional deserializers, checked before considering default
         * basic or bean deserializers.
         * 
         * @since 1.7
         */
        protected final Deserializers[] _additionalDeserializers;

        /**
         * List of providers for additional key deserializers, checked before considering
         * standard key deserializers.
         * 
         * @since 1.7
         */
        protected final KeyDeserializers[] _additionalKeyDeserializers;
        
        /**
         * List of modifiers that can change the way {@link BeanDeserializer} instances
         * are configured and constructed.
         */
        protected final BeanDeserializerModifier[] _modifiers;

        /**
         * List of objects that may be able to resolve abstract types to
         * concrete types. Used by functionality like "mr Bean" to materialize
         * types as needed.
         * 
         * @since 1.8
         */
        protected final AbstractTypeResolver[] _abstractTypeResolvers;
        
        /**
         * Constructor for creating basic configuration with no additional
         * handlers.
         */
        public ConfigImpl() {
            this(null, null, null, null);
        }

        /**
         * Copy-constructor that will create an instance that contains defined
         * set of additional deserializer providers.
         */
        protected ConfigImpl(Deserializers[] allAdditionalDeserializers,
                KeyDeserializers[] allAdditionalKeyDeserializers,
                BeanDeserializerModifier[] modifiers,
                AbstractTypeResolver[] atr)
        {
            _additionalDeserializers = (allAdditionalDeserializers == null) ?
                    NO_DESERIALIZERS : allAdditionalDeserializers;
            _additionalKeyDeserializers = (allAdditionalKeyDeserializers == null) ?
                    NO_KEY_DESERIALIZERS : allAdditionalKeyDeserializers;
            _modifiers = (modifiers == null) ? NO_MODIFIERS : modifiers;
            _abstractTypeResolvers = (atr == null) ? NO_ABSTRACT_TYPE_RESOLVERS : atr;
        }

        @Override
        public Config withAdditionalDeserializers(Deserializers additional)
        {
            if (additional == null) {
                throw new IllegalArgumentException("Can not pass null Deserializers");
            }
            Deserializers[] all = ArrayBuilders.insertInListNoDup(_additionalDeserializers, additional);
            return new ConfigImpl(all, _additionalKeyDeserializers, _modifiers, _abstractTypeResolvers);
        }

        @Override
        public Config withAdditionalKeyDeserializers(KeyDeserializers additional)
        {
            if (additional == null) {
                throw new IllegalArgumentException("Can not pass null KeyDeserializers");
            }
            KeyDeserializers[] all = ArrayBuilders.insertInListNoDup(_additionalKeyDeserializers, additional);
            return new ConfigImpl(_additionalDeserializers, all, _modifiers, _abstractTypeResolvers);
        }
        
        @Override
        public Config withDeserializerModifier(BeanDeserializerModifier modifier)
        {
            if (modifier == null) {
                throw new IllegalArgumentException("Can not pass null modifier");
            }
            BeanDeserializerModifier[] all = ArrayBuilders.insertInListNoDup(_modifiers, modifier);
            return new ConfigImpl(_additionalDeserializers, _additionalKeyDeserializers, all, _abstractTypeResolvers);
        }

        @Override
        public Config withAbstractTypeResolver(AbstractTypeResolver resolver)
        {
            if (resolver == null) {
                throw new IllegalArgumentException("Can not pass null resolver");
            }
            AbstractTypeResolver[] all = ArrayBuilders.insertInListNoDup(_abstractTypeResolvers, resolver);
            return new ConfigImpl(_additionalDeserializers, _additionalKeyDeserializers, _modifiers, all);
        }
        
        @Override
        public boolean hasDeserializers() { return _additionalDeserializers.length > 0; }

        @Override
        public boolean hasKeyDeserializers() { return _additionalKeyDeserializers.length > 0; }
        
        @Override
        public boolean hasDeserializerModifiers() { return _modifiers.length > 0; }

        @Override
        public boolean hasAbstractTypeResolvers() { return _abstractTypeResolvers.length > 0; }
        
        @Override
        public Iterable<Deserializers> deserializers() {
            return ArrayBuilders.arrayAsIterable(_additionalDeserializers);
        }

        @Override
        public Iterable<KeyDeserializers> keyDeserializers() {
            return ArrayBuilders.arrayAsIterable(_additionalKeyDeserializers);
        }
        
        @Override
        public Iterable<BeanDeserializerModifier> deserializerModifiers() {
            return ArrayBuilders.arrayAsIterable(_modifiers);
        }

        @Override
        public Iterable<AbstractTypeResolver> abstractTypeResolvers() {
            return ArrayBuilders.arrayAsIterable(_abstractTypeResolvers);
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
    protected final Config _factoryConfig;

    @Deprecated
    public BeanDeserializerFactory() {
        this(null);
    }

    /**
     * @since 1.7
     */
    public BeanDeserializerFactory(DeserializerFactory.Config config) {
        if (config == null) {
            config = new ConfigImpl();
        }
        _factoryConfig = config;
    }

    @Override
    public final Config getConfig() {
        return _factoryConfig;
    }
    
    /**
     * Method used by module registration functionality, to construct a new bean
     * deserializer factory
     * with different configuration settings.
     * 
     * @since 1.7
     */
    @Override
    public DeserializerFactory withConfig(DeserializerFactory.Config config)
    {
        if (_factoryConfig == config) {
            return this;
        }

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
        return new BeanDeserializerFactory(config);
    }
    
    /*
    /**********************************************************
    /* Overrides for super-class methods used for finding
    /* custom deserializers
    /**********************************************************
     */

    @Override
    public KeyDeserializer createKeyDeserializer(DeserializationConfig config, JavaType type,
            BeanProperty property)
        throws JsonMappingException
    {
        if (_factoryConfig.hasKeyDeserializers()) {
            BasicBeanDescription beanDesc = config.introspectClassAnnotations(type.getRawClass());
            for (KeyDeserializers d  : _factoryConfig.keyDeserializers()) {
                KeyDeserializer deser = d.findKeyDeserializer(type, config, beanDesc, property);
                if (deser != null) {
                    return deser;
                }
            }
        }
        return null;
    }
    
    @Override
    protected JsonDeserializer<?> _findCustomArrayDeserializer(ArrayType type, DeserializationConfig config,
            DeserializerProvider provider,
            BeanProperty property,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
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
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findCollectionDeserializer(type, config, provider, beanDesc, property,
                    elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @Override
    protected JsonDeserializer<?> _findCustomCollectionLikeDeserializer(CollectionLikeType type, DeserializationConfig config,
            DeserializerProvider provider, BasicBeanDescription beanDesc,
            BeanProperty property,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findCollectionLikeDeserializer(type, config, provider, beanDesc, property,
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
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findEnumDeserializer(type, config, beanDesc, property);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @Override
    protected JsonDeserializer<?> _findCustomMapDeserializer(MapType type,
            DeserializationConfig config,
            DeserializerProvider provider, BasicBeanDescription beanDesc, BeanProperty property,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findMapDeserializer(type, config, provider, beanDesc, property,
                    keyDeserializer, elementTypeDeserializer, elementDeserializer);
            if (deser != null) {
                return deser;
            }
        }
        return null;
    }

    @Override
    protected JsonDeserializer<?> _findCustomMapLikeDeserializer(MapLikeType type,
            DeserializationConfig config,
            DeserializerProvider provider, BasicBeanDescription beanDesc, BeanProperty property,
            KeyDeserializer keyDeserializer,
            TypeDeserializer elementTypeDeserializer, JsonDeserializer<?> elementDeserializer)
        throws JsonMappingException
    {
        for (Deserializers d  : _factoryConfig.deserializers()) {
            JsonDeserializer<?> deser = d.findMapLikeDeserializer(type, config, provider, beanDesc, property,
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
        for (Deserializers d  : _factoryConfig.deserializers()) {
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
        for (Deserializers d  : _factoryConfig.deserializers()) {
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
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config,
            DeserializerProvider p, JavaType type, BeanProperty property)
        throws JsonMappingException
    {
        // First things first: abstract types may use defaulting:
        if (type.isAbstract()) {
            type = mapAbstractType(config, type);
        }
        
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
        /* One more thing to check: do we have an exception type
         * (Throwable or its sub-classes)? If so, need slightly
         * different handling.
         */
        if (type.isThrowable()) {
            return buildThrowableDeserializer(config, type, beanDesc, property);
        }
        /* Or, for abstract types, may have alternate means for resolution
         * (defaulting, materialization)
         */
        if (type.isAbstract()) {
            // [JACKSON-41] (v1.6): Let's make it possible to materialize abstract types.
            JavaType concreteType = materializeAbstractType(config, beanDesc);
            if (concreteType != null) {
                /* important: introspect actual implementation (abstract class or
                 * interface doesn't have constructors, for one)
                 */
                beanDesc = config.introspect(concreteType);
                return buildBeanDeserializer(config, concreteType, beanDesc, property);
            }
        }

        // Otherwise, may want to check handlers for standard types, from superclass:
        JsonDeserializer<Object> deser = findStdBeanDeserializer(config, p, type, property);
        if (deser != null) {
            return deser;
        }

        // Otherwise: could the class be a Bean class? If not, bail out
        if (!isPotentialBeanType(type.getRawClass())) {
            return null;
        }
        
        // if we still just have abstract type (but no deserializer), probably need type info, so:
        if (type.isAbstract()) {
            return new AbstractDeserializer(type);
            
        }
        /* Otherwise we'll just use generic bean introspection
         * to build deserializer
         */
        return buildBeanDeserializer(config, type, beanDesc, property);
    }


    /**
     * Method that will find complete abstract type mapping for specified type, doing as
     * many resolution steps as necessary.
     */
    @Override
    protected JavaType mapAbstractType(DeserializationConfig config, JavaType type)
        throws JsonMappingException
    {
        while (true) {
            JavaType next = _mapAbstractType2(config, type);
            if (next == null) {
                return type;
            }
            /* Should not have to worry about cycles; but better verify since they will invariably
             * occur... :-)
             * (also: guard against invalid resolution to a non-related type)
             */
            Class<?> prevCls = type.getRawClass();
            Class<?> nextCls = next.getRawClass();
            if ((prevCls == nextCls) || !prevCls.isAssignableFrom(nextCls)) {
                throw new IllegalArgumentException("Invalid abstract type resolution from "+type+" to "+next+": latter is not a subtype of former");
            }
            type = next;
        }
    }

    /**
     * Method that will find abstract type mapping for specified type, doing a single
     * lookup through registered abstract type resolvers; will not do recursive lookups.
     */
    protected JavaType _mapAbstractType2(DeserializationConfig config, JavaType type)
        throws JsonMappingException
    {
        Class<?> currClass = type.getRawClass();
        if (_factoryConfig.hasAbstractTypeResolvers()) {
            for (AbstractTypeResolver resolver : _factoryConfig.abstractTypeResolvers()) {
                JavaType concrete = resolver.findTypeMapping(config, type);
                if (concrete != null && concrete.getRawClass() != currClass) {
                    return concrete;
                }
            }
        }
        // Also; as a fallback,  we support (until 2.0) old extension point too
        @SuppressWarnings("deprecation")
        AbstractTypeResolver resolver = config.getAbstractTypeResolver();
        if (resolver != null) {
            JavaType concrete = resolver.findTypeMapping(config, type);
            if (concrete != null && concrete.getRawClass() != currClass) {
                return concrete;
            }
        }
        return null;
    }
    
    protected JavaType materializeAbstractType(DeserializationConfig config,
            BasicBeanDescription beanDesc)
        throws JsonMappingException
    {
        // Deprecated registration method, try this one:
        @SuppressWarnings("deprecation")
        AbstractTypeResolver resolver = config.getAbstractTypeResolver();

        // Quick shortchut: if nothing registered, return quickly:
        if (resolver == null && !_factoryConfig.hasAbstractTypeResolvers()) {
            return null;
        }

        final JavaType abstractType = beanDesc.getType();
        // Otherwise check that there is no @JsonTypeInfo registered (to avoid conflicts)
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        if (intr.findTypeResolver(config, beanDesc.getClassInfo(), abstractType) != null) {
            return null;
        }

        if (resolver != null) {
            JavaType concrete = resolver.resolveAbstractType(config, abstractType);
            if (concrete != null) {
                return concrete;
            }
        }
        
        /* [JACKSON-502] (1.8): Now it is possible to have multiple resolvers too,
         *   as they are registered via module interface.
         */
        for (AbstractTypeResolver r : _factoryConfig.abstractTypeResolvers()) {
            JavaType concrete = r.resolveAbstractType(config, abstractType);
            if (concrete != null) {
                return concrete;
            }
        }
        return null;
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
    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> buildBeanDeserializer(DeserializationConfig config,
            JavaType type, BasicBeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(beanDesc);
        builder.setCreators(findDeserializerCreators(config, beanDesc));
         // And then setters for deserializing from JSON Object
        addBeanProps(config, beanDesc, builder);
        // managed/back reference fields/setters need special handling... first part
        addReferenceProperties(config, beanDesc, builder);

        // [JACKSON-440]: update builder now that all information is in?
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, beanDesc, builder);
            }
        }
        JsonDeserializer<?> deserializer = builder.build(property);

        // [JACKSON-440]: may have modifier(s) that wants to modify or replace serializer we just built:
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, beanDesc, deserializer);
            }
        }
        return (JsonDeserializer<Object>) deserializer;
        
    }

    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> buildThrowableDeserializer(DeserializationConfig config,
            JavaType type, BasicBeanDescription beanDesc, BeanProperty property)
        throws JsonMappingException
    {
        // first: construct like a regular bean deserializer...
        BeanDeserializerBuilder builder = constructBeanDeserializerBuilder(beanDesc);
        builder.setCreators(findDeserializerCreators(config, beanDesc));

        addBeanProps(config, beanDesc, builder);
        // (and assume there won't be any back references)

        // But then let's decorate things a bit
        /* To resolve [JACKSON-95], need to add "initCause" as setter
         * for exceptions (sub-classes of Throwable).
         */
        AnnotatedMethod am = beanDesc.findMethod("initCause", INIT_CAUSE_PARAMS);
        if (am != null) { // should never be null
            SettableBeanProperty prop = constructSettableProperty(config, beanDesc, "cause", am);
            if (prop != null) {
                builder.addProperty(prop);
            }
        }

        // And also need to ignore "localizedMessage"
        builder.addIgnorable("localizedMessage");
        /* As well as "message": it will be passed via constructor,
         * as there's no 'setMessage()' method
        */
        builder.addIgnorable("message");

        // [JACKSON-440]: update builder now that all information is in?
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                builder = mod.updateBuilder(config, beanDesc, builder);
            }
        }
        JsonDeserializer<?> deserializer = builder.build(property);
        
        /* At this point it ought to be a BeanDeserializer; if not, must assume
         * it's some other thing that can handle deserialization ok...
         */
        if (deserializer instanceof BeanDeserializer) {
            deserializer = new ThrowableDeserializer((BeanDeserializer) deserializer);
        }

        // [JACKSON-440]: may have modifier(s) that wants to modify or replace serializer we just built:
        if (_factoryConfig.hasDeserializerModifiers()) {
            for (BeanDeserializerModifier mod : _factoryConfig.deserializerModifiers()) {
                deserializer = mod.modifyDeserializer(config, beanDesc, deserializer);
            }
        }
        return (JsonDeserializer<Object>) deserializer;
    }

    /*
    /**********************************************************
    /* Helper methods for Bean deserializer construction,
    /* overridable by sub-classes
    /**********************************************************
     */

    /**
     * Overridable method that constructs a {@link BeanDeserializerBuilder}
     * which is used to accumulate information needed to create deserializer
     * instance.
     * 
     * @since 1.7
     */
    protected BeanDeserializerBuilder constructBeanDeserializerBuilder(BasicBeanDescription beanDesc) {
        return new BeanDeserializerBuilder(beanDesc);
    }

    /**
     * Method that is to find all creators (constructors, factory methods)
     * for the bean type to deserialize.
     * 
     * @since 1.7
     */
    protected CreatorContainer findDeserializerCreators(DeserializationConfig config,
            BasicBeanDescription beanDesc)
        throws JsonMappingException
    {
        boolean fixAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
        CreatorContainer creators =  new CreatorContainer(beanDesc, fixAccess);
        AnnotationIntrospector intr = config.getAnnotationIntrospector();

        // First, let's figure out constructor/factory-based instantation
        // 23-Jan-2010, tatus: but only for concrete types
        if (beanDesc.getType().isConcrete()) {
            Constructor<?> defaultCtor = beanDesc.findDefaultConstructor();
            if (defaultCtor != null) {
                if (fixAccess) {
                    ClassUtil.checkAndFixAccess(defaultCtor);
                }
    
                creators.setDefaultConstructor(defaultCtor);
            }
        }

        // need to construct suitable visibility checker:
        VisibilityChecker<?> vchecker = config.getDefaultVisibilityChecker();
        if (!config.isEnabled(DeserializationConfig.Feature.AUTO_DETECT_CREATORS)) {
            vchecker = vchecker.withCreatorVisibility(Visibility.NONE);
        }
        vchecker = config.getAnnotationIntrospector().findAutoDetectVisibility(beanDesc.getClassInfo(), vchecker);

        _addDeserializerConstructors(config, beanDesc, vchecker, intr, creators);
        _addDeserializerFactoryMethods(config, beanDesc, vchecker, intr, creators);
        return creators;
    }

    protected void _addDeserializerConstructors
        (DeserializationConfig config, BasicBeanDescription beanDesc, VisibilityChecker<?> vchecker,
         AnnotationIntrospector intr, CreatorContainer creators)
        throws JsonMappingException
    {
        for (AnnotatedConstructor ctor : beanDesc.getConstructors()) {
            int argCount = ctor.getParameterCount();
            if (argCount < 1) {
                continue;
            }
            boolean isCreator = intr.hasCreatorAnnotation(ctor);
            boolean isVisible =  vchecker.isCreatorVisible(ctor);
            // some single-arg constructors (String, number) are auto-detected
            if (argCount == 1) {
                /* but note: if we do have parameter name, it'll be
                 * "property constructor", and needs to be skipped for now
                 */
                AnnotatedParameter param = ctor.getParameter(0);
				String name = intr.findPropertyNameForParam(param);
                if (name == null || name.length() == 0) { // not property based
                    Class<?> type = ctor.getParameterClass(0);
                    if (type == String.class) {
                        if (isCreator || isVisible) {
                            creators.addStringConstructor(ctor);
                        }
                        continue;
                    }
                    if (type == int.class || type == Integer.class) {
                        if (isCreator || isVisible) {
                            creators.addIntConstructor(ctor);
                        }
                        continue;
                    }
                    if (type == long.class || type == Long.class) {
                        if (isCreator || isVisible) {
                            creators.addLongConstructor(ctor);
                        }
                        continue;
                    }
                    // Delegating constructor ok iff it has @JsonCreator (etc)
                    if (isCreator) {
                        creators.addDelegatingConstructor(ctor);
                    }
                    // otherwise just ignored
                    continue;
                }
                // We know there's a name and it's only 1 parameter.
                SettableBeanProperty[] properties = new SettableBeanProperty[1];
                properties[0] = constructCreatorProperty(config, beanDesc, name, 0, param);
                creators.addPropertyConstructor(ctor, properties);
                continue;
            } else if (!isCreator && !isVisible) {
            	continue;
            }
            // [JACKSON-541] improved handling a bit so:
            // 2 or more args; all params must have name annotations.
            // But if it was auto-detected and there's no annotations, keep silent (was not meant to be a creator?)
            boolean annotationFound = false;
            boolean notAnnotatedParamFound = false;
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            for (int i = 0; i < argCount; ++i) {
                AnnotatedParameter param = ctor.getParameter(i);
                String name = (param == null) ? null : intr.findPropertyNameForParam(param);
                // If some parameters are annotated and others not, it's invalid.
                // If the constructor is annotated with @JsonCreator, all params must have annotation
                notAnnotatedParamFound |= (name == null || name.length() == 0);
                annotationFound |= !notAnnotatedParamFound;
                if (notAnnotatedParamFound && (annotationFound || isCreator)) {
                    throw new IllegalArgumentException("Argument #"+i+" of constructor "+ctor+" has no property name annotation; must have name when multiple-paramater constructor annotated as Creator");
                }
                properties[i] = constructCreatorProperty(config, beanDesc, name, i, param);
            }
            if (annotationFound) {
            	creators.addPropertyConstructor(ctor, properties);
            }
        }
    }

    protected void _addDeserializerFactoryMethods
        (DeserializationConfig config, BasicBeanDescription beanDesc, VisibilityChecker<?> vchecker,
         AnnotationIntrospector intr, CreatorContainer creators)
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
            BasicBeanDescription beanDesc, BeanDeserializerBuilder builder)
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
                builder.setIgnoreUnknownProperties(ignoreAny);
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
                builder.addIgnorable(propName);
            }
            // Implicit ones via @JsonIgnore and equivalent?
            AnnotatedClass ac = beanDesc.getClassInfo();
            for (AnnotatedMethod am : ac.ignoredMemberMethods()) {
                String name = beanDesc.okNameForSetter(am);
                if (name != null) {
                    builder.addIgnorable(name);
                }
            }
            for (AnnotatedField af : ac.ignoredFields()) {
                builder.addIgnorable(af.getName());
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
                    builder.addIgnorable(name);
                    continue;
                }
                SettableBeanProperty prop = constructSettableProperty(config, beanDesc, name, setter);
                if (prop != null) {
                    builder.addProperty(prop);
                }
            }
        }
        if (anySetter != null) {
            builder.setAnySetter(constructAnySetter(config, beanDesc, anySetter));
        }

        HashSet<String> addedProps = new HashSet<String>(setters.keySet());
        /* [JACKSON-98]: also include field-backed properties:
         *   (second arg passed to ignore anything for which there is a getter
         *   method)
         */
        LinkedHashMap<String,AnnotatedField> fieldsByProp = beanDesc.findDeserializableFields(vchecker, addedProps);
        for (Map.Entry<String,AnnotatedField> en : fieldsByProp.entrySet()) {
            String name = en.getKey();
            if (!ignored.contains(name) && !builder.hasProperty(name)) {
                AnnotatedField field = en.getValue();
                // [JACKSON-429] Some types are declared as ignorable as well
                Class<?> type = field.getRawType();
                if (isIgnorableType(config, beanDesc, type, ignoredTypes)) {
                    // important: make ignorable, to avoid errors if value is actually seen
                    builder.addIgnorable(name);
                    continue;
                }
                SettableBeanProperty prop = constructSettableProperty(config, beanDesc, name, field);
                if (prop != null) {
                    builder.addProperty(prop);
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
                if (!ignored.contains(name) && !builder.hasProperty(name)) {
                    builder.addProperty(constructSetterlessProperty(config, beanDesc, name, getter));
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
            BasicBeanDescription beanDesc, BeanDeserializerBuilder builder)
        throws JsonMappingException
    {
        // and then back references, not necessarily found as regular properties
        Map<String,AnnotatedMember> refs = beanDesc.findBackReferenceProperties();
        if (refs != null) {
            for (Map.Entry<String, AnnotatedMember> en : refs.entrySet()) {
                String name = en.getKey();
                AnnotatedMember m = en.getValue();
                if (m instanceof AnnotatedMethod) {
                    builder.addBackReferenceProperty(name, constructSettableProperty(
                            config, beanDesc, m.getName(), (AnnotatedMethod) m));
                } else {
                    builder.addBackReferenceProperty(name, constructSettableProperty(
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
        JavaType type = beanDesc.bindingsForBeanType().resolveType(setter.getParameterType(1));
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
        JavaType t0 = beanDesc.bindingsForBeanType().resolveType(setter.getParameterType(0));
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
        JavaType t0 = beanDesc.bindingsForBeanType().resolveType(field.getGenericType());
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
