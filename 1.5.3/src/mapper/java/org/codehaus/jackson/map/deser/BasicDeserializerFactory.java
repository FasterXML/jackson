package org.codehaus.jackson.map.deser;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedConstructor;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.AnnotatedParameter;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.SubTypeHelper;
import org.codehaus.jackson.type.JavaType;

/**
 * Abstract factory base class that can provide deserializers for standard
 * JDK classes, including collection classes and simple heuristics for
 * "upcasting" commmon collection interface types
 * (such as {@link java.util.Collection}).
 *<p>
 * Since all simple deserializers are eagerly instantiated, and there is
 * no additional introspection or customizability of these types,
 * this factory is stateless.
 */
public abstract class BasicDeserializerFactory
    extends DeserializerFactory
{
    // // Can cache some types

    final static JavaType TYPE_STRING = TypeFactory.type(String.class);

    /**
     * We will pre-create serializers for common non-structured
     * (that is things other than Collection, Map or array)
     * types. These need not go through factory.
     */
    final static HashMap<JavaType, JsonDeserializer<Object>> _simpleDeserializers = StdDeserializers.constructAll();


    /* We do some defaulting for abstract Map classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Maps will do.
     */
    @SuppressWarnings("unchecked")
    final static HashMap<String, Class<? extends Map>> _mapFallbacks =
        new HashMap<String, Class<? extends Map>>();
    static {

        _mapFallbacks.put(Map.class.getName(), LinkedHashMap.class);
        _mapFallbacks.put(ConcurrentMap.class.getName(), ConcurrentHashMap.class);
        _mapFallbacks.put(SortedMap.class.getName(), TreeMap.class);

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _mapFallbacks.put("java.util.NavigableMap", TreeMap.class);
        try {
            Class<?> key = Class.forName("java.util.ConcurrentNavigableMap");
            Class<?> value = Class.forName("java.util.ConcurrentSkipListMap");
            @SuppressWarnings("unchecked")
                Class<? extends Map> mapValue = (Class<? extends Map>) value;
            _mapFallbacks.put(key.getName(), mapValue);
        } catch (ClassNotFoundException cnfe) { // occurs on 1.5
        }
    }

    /* We do some defaulting for abstract Collection classes and
     * interfaces, to avoid having to use exact types or annotations in
     * cases where the most common concrete Collection will do.
     */
    @SuppressWarnings("unchecked")
    final static HashMap<String, Class<? extends Collection>> _collectionFallbacks =
        new HashMap<String, Class<? extends Collection>>();
    static {
        _collectionFallbacks.put(Collection.class.getName(), ArrayList.class);
        _collectionFallbacks.put(List.class.getName(), ArrayList.class);
        _collectionFallbacks.put(Set.class.getName(), HashSet.class);
        _collectionFallbacks.put(SortedSet.class.getName(), TreeSet.class);
        _collectionFallbacks.put(Queue.class.getName(), LinkedList.class);

        /* 11-Jan-2009, tatu: Let's see if we can still add support for
         *    JDK 1.6 interfaces, even if we run on 1.5. Just need to be
         *    more careful with typos, since compiler won't notice any
         *    problems...
         */
        _collectionFallbacks.put("java.util.Deque", LinkedList.class);
        _collectionFallbacks.put("java.util.NavigableSet", TreeSet.class);
    }

    /**
     * And finally, we have special array deserializers for primitive
     * array types
     */
    final static HashMap<JavaType,JsonDeserializer<Object>> _arrayDeserializers = ArrayDeserializers.getAll();

    /*
    /****************************************************
    /* Life cycle
    /****************************************************
     */

    protected BasicDeserializerFactory() { }

    /*
    /*****************************************************
    /* JsonDeserializerFactory impl
    /*****************************************************
     */

    @Override
    public JsonDeserializer<?> createArrayDeserializer(DeserializationConfig config, ArrayType type, DeserializerProvider p)
        throws JsonMappingException
    {
        JavaType elemType = type.getContentType();
        
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = elemType.getValueHandler();
        if (contentDeser == null) {
            // Maybe special array type, such as "primitive" arrays (int[] etc)
            JsonDeserializer<Object> deser = _arrayDeserializers.get(elemType);
            if (deser != null) {
                return deser;
            }
            // If not, generic one:
            if (elemType.isPrimitive()) { // sanity check
                throw new IllegalArgumentException("Internal error: primitive type ("+type+") passed, no array deserializer found");
            }
            // 'null' -> arrays have no referring fields
            contentDeser = p.findValueDeserializer(config, elemType, type, null);
        }
        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        TypeDeserializer elemTypeDeser = elemType.getTypeHandler();
        // but if not, may still be possible to find:
        if (elemTypeDeser == null) {
        	elemTypeDeser = findTypeDeserializer(config, elemType);
        }
        return new ArrayDeserializer(type, contentDeser, elemTypeDeser);
    }

    @Override
    public JsonDeserializer<?> createCollectionDeserializer(DeserializationConfig config,
    		CollectionType type, DeserializerProvider p)
        throws JsonMappingException
    {
        Class<?> collectionClass = type.getRawClass();
        BasicBeanDescription beanDesc = config.introspectClassAnnotations(collectionClass);
        // Explicit deserializer to use? (@JsonDeserialize.using)
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(config, beanDesc.getClassInfo());
        if (deser != null) {
            return deser;
        }
        // If not, any type modifiers? (@JsonDeserialize.as)
        type = modifyTypeByAnnotation(config, beanDesc.getClassInfo(), type, null);

        JavaType contentType = type.getContentType();
        // Very first thing: is deserializer hard-coded for elements?
        JsonDeserializer<Object> contentDeser = contentType.getValueHandler();

        if (contentDeser == null) { // not defined by annotation
            // One special type: EnumSet:
            if (EnumSet.class.isAssignableFrom(collectionClass)) {
                return new EnumSetDeserializer(EnumResolver.constructUnsafe(contentType.getRawClass(),
                		config.getAnnotationIntrospector()));
            }
            // But otherwise we can just use a generic value deserializer:
            // 'null' -> collections have no referring fields
            contentDeser = p.findValueDeserializer(config, contentType, type, null);            
        }

        /* One twist: if we are being asked to instantiate an interface or
         * abstract Collection, we need to either find something that implements
         * the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (type.isInterface() || type.isAbstract()) {
            @SuppressWarnings("unchecked")
            Class<? extends Collection> fallback = _collectionFallbacks.get(collectionClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Collection type "+type);
            }
            collectionClass = fallback;
        }
        // Then optional type info (1.5): if type has been resolved, we may already know type deserializer:
        
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
            contentTypeDeser = findTypeDeserializer(config, contentType);
        }

        boolean fixAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
        @SuppressWarnings("unchecked")
        Constructor<Collection<Object>> ctor = ClassUtil.findConstructor((Class<Collection<Object>>)collectionClass, fixAccess);
        return new CollectionDeserializer(collectionClass, contentDeser, contentTypeDeser, ctor);
    }

    @Override
    public JsonDeserializer<?> createMapDeserializer(DeserializationConfig config, MapType type, DeserializerProvider p)
        throws JsonMappingException
    {
        Class<?> mapClass = type.getRawClass();

        BasicBeanDescription beanDesc = config.introspectForCreation(mapClass);
        // Explicit deserializer to use? (@JsonDeserialize.using)
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(config, beanDesc.getClassInfo());
        if (deser != null) {
            return deser;
        }
        // If not, any type modifiers? (@JsonDeserialize.as)
        type = modifyTypeByAnnotation(config, beanDesc.getClassInfo(), type, null);
        
        JavaType keyType = type.getKeyType();
        JavaType contentType = type.getContentType();

        // First: is there annotation-specified deserializer for values?
        @SuppressWarnings("unchecked")
        JsonDeserializer<Object> contentDeser = (JsonDeserializer<Object>) contentType.getValueHandler();
        if (contentDeser == null) { // nope...
            // 'null' -> maps have no referring fields
            contentDeser = p.findValueDeserializer(config, contentType, type, null);
        }
        /* Value handling is identical for all,
         * but EnumMap requires special handling for keys
         */
        if (EnumMap.class.isAssignableFrom(mapClass)) {
            return new EnumMapDeserializer(EnumResolver.constructUnsafe(keyType.getRawClass(), config.getAnnotationIntrospector()), contentDeser);
        }

        // Otherwise, generic handler works ok.
        // Ok: need a key deserializer (null indicates 'default' here)
        KeyDeserializer keyDes = (KeyDeserializer) keyType.getValueHandler();
        if (keyDes == null) {
            keyDes = (TYPE_STRING.equals(keyType)) ? null : p.findKeyDeserializer(config, keyType);
        }

        /* But there is one more twist: if we are being asked to instantiate
         * an interface or abstract Map, we need to either find something
         * that implements the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (type.isInterface() || type.isAbstract()) {
            @SuppressWarnings("unchecked")
            Class<? extends Map> fallback = _mapFallbacks.get(mapClass.getName());
            if (fallback == null) {
                throw new IllegalArgumentException("Can not find a deserializer for non-concrete Map type "+type);
            }
            mapClass = fallback;
            // But if so, also need to re-check creators...
            beanDesc = config.introspectForCreation(mapClass);
        }

        // [JACKSON-153]: allow use of @JsonCreator
        boolean fixAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
        // First, locate the default constructor (if one available)
        @SuppressWarnings("unchecked")
        Constructor<Map<Object,Object>> defaultCtor = (Constructor<Map<Object,Object>>) beanDesc.findDefaultConstructor();
        if (defaultCtor != null) {
            if (fixAccess) {
                ClassUtil.checkAndFixAccess(defaultCtor);
            }
        }
        // Then optional type info (1.5); either attached to type, or resolve separately:
        TypeDeserializer contentTypeDeser = contentType.getTypeHandler();
        // but if not, may still be possible to find:
        if (contentTypeDeser == null) {
        	contentTypeDeser = findTypeDeserializer(config, contentType);
        }
        MapDeserializer md = new MapDeserializer(type, defaultCtor, keyDes, contentDeser, contentTypeDeser);
        md.setIgnorableProperties(config.getAnnotationIntrospector().findPropertiesToIgnore(beanDesc.getClassInfo()));
        md.setCreators(findMapCreators(config, beanDesc));
        return md;
    }

    /**
     * Factory method for constructing serializers of {@link Enum} types.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<?> createEnumDeserializer(DeserializationConfig config, Class<?> enumClass, DeserializerProvider p)
        throws JsonMappingException
    {
        /* 18-Feb-2009, tatu: Must first check if we have a class annotation
         *    that should override default deserializer
         */
        BasicBeanDescription beanDesc = config.introspectClassAnnotations(enumClass);
        JsonDeserializer<Object> des = findDeserializerFromAnnotation(config, beanDesc.getClassInfo());
        if (des != null) {
            return des;
        }
        JsonDeserializer<?> d2 = new EnumDeserializer(EnumResolver.constructUnsafe(enumClass, config.getAnnotationIntrospector()));
        return (JsonDeserializer<Object>) d2;
    }

    @Override
    public JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config, Class<? extends JsonNode> nodeClass, DeserializerProvider p)
        throws JsonMappingException
    {
        return JsonNodeDeserializer.getDeserializer(nodeClass);
    }

    @Override
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return _simpleDeserializers.get(type);
    }

    @Override
    public TypeDeserializer findTypeDeserializer(DeserializationConfig config, JavaType baseType)
    {
        Class<?> cls = baseType.getRawClass();
        BasicBeanDescription bean = config.introspectClassAnnotations(cls);
        AnnotatedClass ac = bean.getClassInfo();
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findTypeResolver(ac, baseType);
        /* Ok: if there is no explicit type info handler, we may want to
         * use a default. If so, config object knows what to use.
         */
        Collection<NamedType> subtypes = null;
        if (b == null) {
            b = config.getDefaultTyper(baseType);
        } else {
            subtypes = SubTypeHelper.collectAndResolveSubtypes(ac, config, ai);
        }
        return (b == null) ? null : b.buildTypeDeserializer(baseType, subtypes);
    }    

    /*
    /*****************************************************
    /* Extended API
    /*****************************************************
     */

    /**
     * Method called to create a type information deserializer for values of
     * given non-container property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for non-container bean properties,
     * and not for values in container types or root values (or container properties)
     *
     * @param baseType Declared base type of the value to deserializer (actual
     *    deserializer type will be this type or its subtype)
     * 
     * @return Type deserializer to use for given base type, if one is needed; null if not.
     * 
     * @since 1.5
     */
    public TypeDeserializer findPropertyTypeDeserializer(DeserializationConfig config, JavaType baseType,
            AnnotatedMember propertyEntity)
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyTypeResolver(propertyEntity, baseType);        

        // Defaulting: if no annotations on member, check value class
        if (b == null) {
            return findTypeDeserializer(config, baseType);
        }
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> subtypes = SubTypeHelper.collectAndResolveSubtypes(propertyEntity, config, ai);
        return b.buildTypeDeserializer(baseType, subtypes);
    }
    
    /**
     * Method called to find and create a type information deserializer for values of
     * given container (list, array, map) property, if one is needed.
     * If not needed (no polymorphic handling configured for property), should return null.
     *<p>
     * Note that this method is only called for container bean properties,
     * and not for values in container types or root values (or non-container properties)
     * 
     * @param containerType Type of property; must be a container type
     * @param propertyEntity Field or method that contains container property
     * 
     * @since 1.5
     */    
    public TypeDeserializer findPropertyContentTypeDeserializer(DeserializationConfig config, JavaType containerType,
            AnnotatedMember propertyEntity)
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyContentTypeResolver(propertyEntity, containerType);        
        JavaType contentType = containerType.getContentType();
        // Defaulting: if no annotations on member, check class
        if (b == null) {
            return findTypeDeserializer(config, contentType);
        }
        // but if annotations found, may need to resolve subtypes:
        Collection<NamedType> subtypes = SubTypeHelper.collectAndResolveSubtypes(propertyEntity, config, ai);
        return b.buildTypeDeserializer(contentType, subtypes);
    }
    
    /*
    /*********************************************************
    /* Helper methods, value/content/key type introspection
    /*********************************************************
     */

    /**
     * Helper method called to check if a class or method
     * has annotation that tells which class to use for deserialization.
     * Returns null if no such annotation found.
     */
    protected JsonDeserializer<Object> findDeserializerFromAnnotation(DeserializationConfig config, Annotated a)
    {
        Object deserDef = config.getAnnotationIntrospector().findDeserializer(a);
        if (deserDef != null) {
            return _constructDeserializer(config, deserDef);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    JsonDeserializer<Object> _constructDeserializer(DeserializationConfig config, Object deserDef)
    {
        if (deserDef instanceof JsonDeserializer) {
            return (JsonDeserializer<Object>) deserDef;
        }
        /* Alas, there's no way to force return type of "either class
         * X or Y" -- need to throw an exception after the fact
         */
        if (!(deserDef instanceof Class)) {
            throw new IllegalStateException("AnnotationIntrospector returned deserializer definition of type "+deserDef.getClass().getName()+"; expected type JsonDeserializer or Class<JsonDeserializer> instead");
        }
        Class<?> cls = (Class<?>) deserDef;
        if (!JsonDeserializer.class.isAssignableFrom(cls)) {
            throw new IllegalStateException("AnnotationIntrospector returned Class "+cls.getName()+"; expected Class<JsonDeserializer>");
        }
        return (JsonDeserializer<Object>) ClassUtil.createInstance(cls, config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS));
    }

    /**
     * Method called to see if given method has annotations that indicate
     * a more specific type than what the argument specifies.
     * If annotations are present, they must specify compatible Class;
     * instance of which can be assigned using the method. This means
     * that the Class has to be raw class of type, or its sub-class
     * (or, implementing class if original Class instance is an interface).
     *
     * @param a Method or field that the type is associated with
     * @param type Type derived from the setter argument
     * @param propName Name of property that refers to type, if any; null
     *   if no property information available (when modify type declaration
     *   of a class, for example)
     *
     * @return Original type if no annotations are present; or a more
     *   specific type derived from it if type annotation(s) was found
     *
     * @throws JsonMappingException if invalid annotation is found
     */
    @SuppressWarnings("unchecked")
    protected <T extends JavaType> T modifyTypeByAnnotation(DeserializationConfig config,
                                                            Annotated a, T type,
                                                            String propName)
        throws JsonMappingException
    {
        // first: let's check class for the instance itself:
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        Class<?> subclass = intr.findDeserializationType(a, type, propName);
        if (subclass != null) {
            try {
                type = (T) type.narrowBy(subclass);
            } catch (IllegalArgumentException iae) {
                throw new JsonMappingException("Failed to narrow type "+type+" with concrete-type annotation (value "+subclass.getName()+"), method '"+a.getName()+"': "+iae.getMessage(), null, iae);
            }
        }

        // then key class
        if (type.isContainerType()) {
            Class<?> keyClass = intr.findDeserializationKeyType(a, type.getKeyType(), propName);
            if (keyClass != null) {
                // illegal to use on non-Maps
                if (!(type instanceof MapType)) {
                    throw new JsonMappingException("Illegal key-type annotation: type "+type+" is not a Map type");
                }
                try {
                    type = (T) ((MapType) type).narrowKey(keyClass);
                } catch (IllegalArgumentException iae) {
                    throw new JsonMappingException("Failed to narrow key type "+type+" with key-type annotation ("+keyClass.getName()+"): "+iae.getMessage(), null, iae);
                }
            }
            
            // and finally content class; only applicable to structured types
            Class<?> cc = intr.findDeserializationContentType(a, type.getContentType(), propName);
            if (cc != null) {
                try {
                    type = (T) type.narrowContentsBy(cc);
                } catch (IllegalArgumentException iae) {
                    throw new JsonMappingException("Failed to narrow content type "+type+" with content-type annotation ("+cc.getName()+"): "+iae.getMessage(), null, iae);
                }
            }
        }
        return type;
    }

    /**
     * Helper method used to resolve method return types and field
     * types. The main trick here is that the containing bean may
     * have type variable binding information (when deserializing
     * using generic type passed as type reference), which is
     * needed in some cases.
     *<p>
     * Starting with version 1.3, this method will also resolve instances
     * of key and content deserializers if defined by annotations.
     */
    protected JavaType resolveType(DeserializationConfig config,
                                   BasicBeanDescription beanDesc, Type rawType,
                                   Annotated a)
    {
        JavaType type = TypeFactory.type(rawType, beanDesc.bindingsForBeanType());
        // [JACKSON-154]: Also need to handle keyUsing, contentUsing
        if (type.isContainerType()) {
            AnnotationIntrospector intr = config.getAnnotationIntrospector();
            boolean canForceAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
            JavaType keyType = type.getKeyType();
            if (keyType != null) {
                Class<? extends KeyDeserializer> kdClass = intr.findKeyDeserializer(a);
                if (kdClass != null && kdClass != KeyDeserializer.None.class) {
                    KeyDeserializer kd = ClassUtil.createInstance(kdClass, canForceAccess);
                    keyType.setValueHandler(kd);
                }
            }
            // and all container types have content types...
            Class<? extends JsonDeserializer<?>> cdClass = intr.findContentDeserializer(a);
            if (cdClass != null && cdClass != JsonDeserializer.None.class) {
                JsonDeserializer<?> cd = ClassUtil.createInstance(cdClass, canForceAccess);
                type.getContentType().setValueHandler(cd);
            }
            /* 04-Feb-2010, tatu: Need to figure out JAXB annotations that indicate type
             *    information to use for polymorphic members; and specifically types for
             *    collection values (contents).
             *    ... but only applies to members (fields, methods), not classes
             */
            if (a instanceof AnnotatedMember) {
            	TypeDeserializer contentTypeDeser = findPropertyContentTypeDeserializer(config, type, (AnnotatedMember) a);            	
            	if (contentTypeDeser != null) {
            	    type.getContentType().setTypeHandler(contentTypeDeser);
            	}
            }
        }
    	TypeDeserializer valueTypeDeser;
    	
        if (a instanceof AnnotatedMember) { // JAXB allows per-property annotations
            valueTypeDeser = findPropertyTypeDeserializer(config, type, (AnnotatedMember) a);
        } else { // classes just have Jackson annotations
            valueTypeDeser = findTypeDeserializer(config, type);
        }
    	if (valueTypeDeser != null) {
            type.setTypeHandler(valueTypeDeser);
    	}
        return type;
    }

    /*
    /****************************************************
    /* Helper methods, dealing with Creators
    /****************************************************
     */

    /**
     * Method used to find non-default constructors and factory 
     * methods that are marked to be used as Creators for a Map type.
     */
    CreatorContainer findMapCreators(DeserializationConfig config,
                                     BasicBeanDescription beanDesc)
        throws JsonMappingException
    {
        Class<?> mapClass = beanDesc.getBeanClass();
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        boolean fixAccess = config.isEnabled(DeserializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS);
        CreatorContainer creators =  new CreatorContainer(mapClass, fixAccess);
        // First, let's find if we have a constructor creator:
        for (AnnotatedConstructor ctor : beanDesc.getConstructors()) {
            int argCount = ctor.getParameterCount();
            if (argCount < 1 || !intr.hasCreatorAnnotation(ctor)) { // default ctor, or not marked with JsonCreator, skip
                continue;
            }
            // For Map types property name is not optional for ctor params
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            int nameCount = 0;
            for (int i = 0; i < argCount; ++i) {
                AnnotatedParameter param = ctor.getParameter(i);
                String name = (param == null) ? null : intr.findPropertyNameForParam(param);
                // At this point, name annotation is NOT optional
                if (name == null || name.length() == 0) {
                    throw new IllegalArgumentException("Parameter #"+i+" of constructor "+ctor+" has no property name annotation: must have for @JsonCreator for a Map type");
                }
                ++nameCount;
                properties[i] = constructCreatorProperty(config, beanDesc, name, i, param);
            }
            creators.addPropertyConstructor(ctor, properties);
        }

        // And then if there's a factory creator
        for (AnnotatedMethod factory : beanDesc.getFactoryMethods()) {
            int argCount = factory.getParameterCount();
            if (argCount < 1 || !intr.hasCreatorAnnotation(factory)) { // no args, or not marked with JsonCreator, skip
                continue;
            }
            // Property name is not optional for factory method params
            SettableBeanProperty[] properties = new SettableBeanProperty[argCount];
            int nameCount = 0;
            for (int i = 0; i < argCount; ++i) {
                AnnotatedParameter param = factory.getParameter(i);
                String name = (param == null) ? null : intr.findPropertyNameForParam(param);
                // At this point, name annotation is NOT optional
                if (name == null || name.length() == 0) {
                    throw new IllegalArgumentException("Parameter #"+i+" of factory method "+factory+" has no property name annotation: must have for @JsonCreator for a Map type");
                }
                ++nameCount;
                properties[i] = constructCreatorProperty(config, beanDesc, name, i, param);
            }
            creators.addPropertyFactory(factory, properties);
        }
        return creators;
    }

    /**
     * Method that will construct a property object that represents
     * a logical property passed via Creator (constructor or static
     * factory method)
     */
    protected SettableBeanProperty constructCreatorProperty(DeserializationConfig config,
                                                            BasicBeanDescription beanDesc,
                                                            String name,
                                                            int index,
                                                            AnnotatedParameter param)
        throws JsonMappingException
    {
        JavaType type = resolveType(config, beanDesc, param.getParameterType(), param);
        // Is there an annotation that specifies exact deserializer?
        JsonDeserializer<Object> deser = findDeserializerFromAnnotation(config, param);
        // If yes, we are mostly done:
        type = modifyTypeByAnnotation(config, param, type, name);
        TypeDeserializer typeDeser = findTypeDeserializer(config, type);
        SettableBeanProperty prop = new SettableBeanProperty.CreatorProperty(name, type, typeDeser,
                beanDesc.getBeanClass(), index);
        if (deser != null) {
            prop.setValueDeserializer(deser);
        }
       return prop;
    }
}
