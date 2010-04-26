package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.BasicBeanDescription;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.util.ArrayBuilders;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.SubTypeHelper;
import org.codehaus.jackson.type.JavaType;
 
/**
 * Factory class that can provide serializers for any regular Java beans
 * (as defined by "having at least one get method recognizable as bean
 * accessor" -- where {@link Object#getClass} does not count);
 * as well as for "standard" JDK types. Latter is achieved
 * by delegating calls to {@link BasicSerializerFactory} 
 * to find serializers both for "standard" JDK types (and in some cases,
 * sub-classes as is the case for collection classes like
 * {@link java.util.List}s and {@link java.util.Map}s) and bean (value)
 * classes.
 *<p>
 * Note about delegating calls to {@link BasicSerializerFactory}:
 * although it would be nicer to use linear delegation
 * for construction (to essentially dispatch all calls first to the
 * underlying {@link BasicSerializerFactory}; or alternatively after
 * failing to provide bean-based serializer}, there is a problem:
 * priority levels for detecting standard types are mixed. That is,
 * we want to check if a type is a bean after some of "standard" JDK
 * types, but before the rest.
 * As a result, "mixed" delegation used, and calls are NOT done using
 * regular {@link SerializerFactory} interface but rather via
 * direct calls to {@link BasicSerializerFactory}.
 *<p>
 * Finally, since all caching is handled by the serializer provider
 * (not factory) and there is no configurability, this
 * factory is stateless.
 * This means that a global singleton instance can be used.
 */
public class BeanSerializerFactory
    extends BasicSerializerFactory
{
    /**
     * Like {@link BasicSerializerFactory}, this factory is stateless, and
     * thus a single shared global (== singleton) instance can be used
     * without thread-safety issues.
     */
    public final static BeanSerializerFactory instance = new BeanSerializerFactory();

    /*
    /*********************************************************
    /* Life cycle
    /*********************************************************
     */

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected BeanSerializerFactory() { }

    /*
    /*********************************************************
    /* JsonSerializerFactory impl
    /*********************************************************
     */

    /**
     * Main serializer constructor method. We will have to be careful
     * with respect to ordering of various method calls: essentially
     * we want to reliably figure out which classes are standard types,
     * and which are beans. The problem is that some bean Classes may
     * implement standard interfaces (say, {@link java.lang.Iterable}.
     *<p>
     * Note: sub-classes may choose to complete replace implementation,
     * if they want to alter priority of serializer lookups.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JsonSerializer<Object> createSerializer(JavaType type, SerializationConfig config)
    {
        /* [JACKSON-220]: Very first thing, let's check annotations to
         * see if we have explicit definition
         */
        BasicBeanDescription beanDesc = config.introspect(type.getRawClass());
        JsonSerializer<?> ser = findSerializerFromAnnotation(config, beanDesc.getClassInfo());
        if (ser == null) {
            // First, fast lookup for exact type:
            ser = super.findSerializerByLookup(type, config, beanDesc);
            if (ser == null) {
                // and then introspect for some safe (?) JDK types
                ser = super.findSerializerByPrimaryType(type, config, beanDesc);
                if (ser == null) {
                    /* And this is where this class comes in: if type is
                     * not a known "primary JDK type", perhaps it's a bean?
                     * We can still get a null, if we can't find a single
                     * suitable bean property.
                     */
                    ser = this.findBeanSerializer(type, config, beanDesc);
                    /* Finally: maybe we can still deal with it as an
                     * implementation of some basic JDK interface?
                     */
                    if (ser == null) {
                        ser = super.findSerializerByAddonType(type, config, beanDesc);
                    }
                }
            }
        }
        return (JsonSerializer<Object>) ser;
    }

    /*
    /*********************************************************
    /* Other public methods that are not part of
    /* JsonSerializerFactory API
    /*********************************************************
     */

    /**
     * Method that will try to construct a {@link BeanSerializer} for
     * given class. Returns null if no properties are found.
     */
    public JsonSerializer<Object> findBeanSerializer(JavaType type, SerializationConfig config,
                                                     BasicBeanDescription beanDesc)
    {
        // First things first: we know some types are not beans...
        if (!isPotentialBeanType(type.getRawClass())) {
            return null;
        }
        // [JACKSON-80]: Support @JsonValue, alternative to bean method introspection.
        AnnotatedMethod valueMethod = beanDesc.findJsonValueMethod();
        if (valueMethod != null) {
            /* Further, method itself may also be annotated to indicate
             * exact JsonSerializer to use for whatever value is returned...
             */
            JsonSerializer<Object> ser = findSerializerFromAnnotation(config, valueMethod);
            return new JsonValueSerializer(valueMethod.getAnnotated(), ser);
        }
        return constructBeanSerializer(config, beanDesc);
    }

    /**
     * Method called to create a type information serializer for values of given
     * non-container property
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param baseType Declared type to use as the base type for type information serializer
     * 
     * @return Type serializer to use for property values, if one is needed; null if not.
     * 
     * @since 1.5
     */
    public TypeSerializer findPropertyTypeSerializer(JavaType baseType, SerializationConfig config,
            AnnotatedMember propertyEntity)
    {
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyTypeResolver(propertyEntity, baseType);        
        // Defaulting: if no annotations on member, check value class
        if (b == null) {
            return createTypeSerializer(baseType, config);
        }
        Collection<NamedType> subtypes = SubTypeHelper.collectAndResolveSubtypes(propertyEntity, config, ai);
        return b.buildTypeSerializer(baseType, subtypes);
    }

    /**
     * Method called to create a type information serializer for values of given
     * container property
     * if one is needed. If not needed (no polymorphic handling configured), should
     * return null.
     *
     * @param containerType Declared type of the container to use as the base type for type information serializer
     * 
     * @return Type serializer to use for property value contents, if one is needed; null if not.
     * 
     * @since 1.5
     */    
    public TypeSerializer findPropertyContentTypeSerializer(JavaType containerType, SerializationConfig config,
            AnnotatedMember propertyEntity)
    {
        JavaType contentType = containerType.getContentType();
        AnnotationIntrospector ai = config.getAnnotationIntrospector();
        TypeResolverBuilder<?> b = ai.findPropertyContentTypeResolver(propertyEntity, containerType);        
        // Defaulting: if no annotations on member, check value class
        if (b == null) {
            return createTypeSerializer(contentType, config);
        }
        Collection<NamedType> subtypes = SubTypeHelper.collectAndResolveSubtypes(propertyEntity, config, ai);
        return b.buildTypeSerializer(contentType, subtypes);
    }
    
    /*
    /*********************************************************
    /* Overridable non-public methods
    /*********************************************************
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
        return (ClassUtil.canBeABeanType(type) == null) && !ClassUtil.isProxyType(type);
    }

    protected JsonSerializer<Object> constructBeanSerializer(SerializationConfig config,
                                                             BasicBeanDescription beanDesc)
    {
        // First: any detectable (auto-detect, annotations) properties to serialize?
        List<BeanPropertyWriter> props = findBeanProperties(config, beanDesc);
        if (props == null || props.size() == 0) {
            // No properties, no serializer
            /* 27-Nov-2009, tatu: Except that as per [JACKSON-201], we are
             *   ok with that as long as it has a recognized class annotation
             *  (which may come from a mix-in too)
             */
            if (beanDesc.hasKnownClassAnnotations()) {
                return BeanSerializer.createDummy(beanDesc.getBeanClass());
            }
            return null;
        }
        // Any properties to suppress?
        props = filterBeanProperties(config, beanDesc, props);
        // Do they need to be sorted in some special way?
        props = sortBeanProperties(config, beanDesc, props);
        BeanSerializer ser = new BeanSerializer(beanDesc.getBeanClass(), props);
        // One more thing: need to gather view information, if any:
        ser = processViews(config, beanDesc, ser, props);
        return ser;
    }

    /**
     * Method used to collect all actual serializable properties.
     * Can be overridden to implement custom detection schemes.
     */
    protected List<BeanPropertyWriter> findBeanProperties(SerializationConfig config, BasicBeanDescription beanDesc)
    {
        // Ok: let's aggregate visibility settings: first, baseline:
        VisibilityChecker<?> vchecker = config.getDefaultVisibilityChecker();
        if (!config.isEnabled(SerializationConfig.Feature.AUTO_DETECT_GETTERS)) {
            vchecker = vchecker.withGetterVisibility(Visibility.NONE);
        }
        // then global overrides (disabling)
        if (!config.isEnabled(SerializationConfig.Feature.AUTO_DETECT_IS_GETTERS)) {
            vchecker = vchecker.withIsGetterVisibility(Visibility.NONE);
        }
        if (!config.isEnabled(SerializationConfig.Feature.AUTO_DETECT_FIELDS)) {
            vchecker = vchecker.withFieldVisibility(Visibility.NONE);
        }
        // and finally per-class overrides:
        vchecker = config.getAnnotationIntrospector().findAutoDetectVisibility(beanDesc.getClassInfo(), vchecker);

        LinkedHashMap<String,AnnotatedMethod> methodsByProp = beanDesc.findGetters(vchecker, null);
        LinkedHashMap<String,AnnotatedField> fieldsByProp = beanDesc.findSerializableFields(vchecker, methodsByProp.keySet());

        // nothing? can't proceed (caller may or may not throw an exception)
        if (methodsByProp.isEmpty() && fieldsByProp.isEmpty()) {
            return null;
        }
        boolean staticTyping = usesStaticTyping(config, beanDesc);
        PropertyBuilder pb = constructPropertyBuilder(config, beanDesc);

        ArrayList<BeanPropertyWriter> props = new ArrayList<BeanPropertyWriter>(methodsByProp.size());
        TypeBindings typeBind = beanDesc.bindingsForBeanType();
        // [JACKSON-98]: start with field properties, if any
        for (Map.Entry<String,AnnotatedField> en : fieldsByProp.entrySet()) {            
            props.add(_constructWriter(config, typeBind, pb, staticTyping, en.getKey(), en.getValue()));
        }
        // and then add member properties
        for (Map.Entry<String,AnnotatedMethod> en : methodsByProp.entrySet()) {
            props.add(_constructWriter(config, typeBind, pb, staticTyping, en.getKey(), en.getValue()));
        }
        return props;
    }

    /**
     * Secondary helper method for constructing {@link BeanPropertyWriter} for
     * given member (field or method).
     */
    protected BeanPropertyWriter _constructWriter(SerializationConfig config, TypeBindings typeContext,
            PropertyBuilder pb, boolean staticTyping, String name, AnnotatedMember propertyMember)
    {
        if (config.isEnabled(SerializationConfig.Feature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
            propertyMember.fixAccess();
        }
        // Does member specify a serializer? If so, let's use it.
        JsonSerializer<Object> annotatedSerializer = findSerializerFromAnnotation(config, propertyMember);
        // And how about polymorphic typing? First special to cover JAXB per-field settings:
        TypeSerializer contentTypeSer = null;
        JavaType type = propertyMember.getType(typeContext);
        if (ClassUtil.isCollectionMapOrArray(type.getRawClass())) {
            contentTypeSer = findPropertyContentTypeSerializer(type, config, propertyMember);
        }

        // and if not JAXB collection/array with annotations, maybe regular type info?
        TypeSerializer typeSer = findPropertyTypeSerializer(type, config, propertyMember);
        BeanPropertyWriter pbw = pb.buildProperty(name, annotatedSerializer,
        		typeSer, contentTypeSer, propertyMember, staticTyping);
        // how about views? (1.4+)
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        pbw.setViews(intr.findSerializationViews(propertyMember));
        return pbw;
    }

    /**
     * Overridable method that can filter out properties. Default implementation
     * checks annotations class may have.
     */
    protected List<BeanPropertyWriter> filterBeanProperties(SerializationConfig config, BasicBeanDescription beanDesc, List<BeanPropertyWriter> props)
    {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        AnnotatedClass ac = beanDesc.getClassInfo();
        String[] ignored = intr.findPropertiesToIgnore(ac);
        if (ignored != null && ignored.length > 0) {
            HashSet<String> ignoredSet = ArrayBuilders.arrayToSet(ignored);
            Iterator<BeanPropertyWriter> it = props.iterator();
            while (it.hasNext()) {
                if (ignoredSet.contains(it.next().getName())) {
                    it.remove();
                }
            }
        }
        return props;
    }

    /**
     * Overridable method that will impose given partial ordering on
     * list of discovered propertied. Method can be overridden to
     * provide custom ordering of properties, beyond configurability
     * offered by annotations (whic allow alphabetic ordering, as
     * well as explicit ordering by providing array of property names).
     *<p>
     * By default Creator properties will be ordered before other
     * properties. Explicit custom ordering will override this implicit
     * default ordering.
     */
    protected List<BeanPropertyWriter> sortBeanProperties(SerializationConfig config, BasicBeanDescription beanDesc, List<BeanPropertyWriter> props)
    {
        // Ok: so far so good. But do we need to (re)order these somehow?
        /* Yes; first, for [JACKSON-90] (explicit ordering and/or alphabetic)
         * and then for [JACKSON-170] (implicitly order creator properties before others)
         */
        List<String> creatorProps = beanDesc.findCreatorPropertyNames();
        // Then how about explicit ordering?
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        AnnotatedClass ac = beanDesc.getClassInfo();
        String[] propOrder = intr.findSerializationPropertyOrder(ac);
        Boolean alpha = intr.findSerializationSortAlphabetically(ac);
        boolean sort = (alpha != null) && alpha.booleanValue();
        if (sort || !creatorProps.isEmpty() || propOrder != null) {
            props = _sortBeanProperties(props, creatorProps, propOrder, sort);
        }
        return props;
    }

    /**
     * Method called to handle view information for constructed serializer,
     * based on bean property writers.
     *<p>
     * Note that this method is designed to be overridden by sub-classes
     * if they want to provide custom view handling. As such it is not
     * considered an internal implementation detail, and will be supported
     * as part of API going forward.
     * 
     * @return Resulting bean serializer, base implementation returns
     *    serializer passed in
     */
    protected BeanSerializer processViews(SerializationConfig config, BasicBeanDescription beanDesc,
                                          BeanSerializer ser, List<BeanPropertyWriter> props)
    {
        // [JACKSON-232]: whether non-annotated fields are included by default or not is configurable
        boolean includeByDefault = config.isEnabled(SerializationConfig.Feature.DEFAULT_VIEW_INCLUSION);
        if (includeByDefault) { // non-annotated are included
            final int propCount = props.size();
            BeanPropertyWriter[] filtered = null;        
            // Simple: view information is stored within individual writers, need to combine:
            for (int i = 0; i < propCount; ++i) {
                BeanPropertyWriter bpw = props.get(i);
                Class<?>[] views = bpw.getViews();
                if (views != null) {
                    if (filtered == null) {
                        filtered = new BeanPropertyWriter[props.size()];
                    }
                    filtered[i] = constructFilteredBeanWriter(bpw, views);
                }
            }        
            // Anything missing? Need to fill in
            if (filtered != null) {
                for (int i = 0; i < propCount; ++i) {
                    if (filtered[i] == null) {
                        filtered[i] = props.get(i);
                    }
                }
                return ser.withFiltered(filtered);
            }        
            // No views, return as is
            return ser;
        }
        // Otherwise: only include fields with view definitions.
        ArrayList<BeanPropertyWriter> explicit = new ArrayList<BeanPropertyWriter>(props.size());
        for (BeanPropertyWriter bpw : props) {
            Class<?>[] views = bpw.getViews();
            if (views != null) {
                explicit.add(constructFilteredBeanWriter(bpw, views));
            }            
        }
        BeanPropertyWriter[] filtered = explicit.toArray(new BeanPropertyWriter[explicit.size()]);
        return ser.withFiltered(filtered);
    }

    /**
     * Method called to construct a filtered writer, for given view
     * definitions. Default implementation constructs filter that checks
     * active view type to views property is to be included in.
     */
    protected BeanPropertyWriter constructFilteredBeanWriter(BeanPropertyWriter writer, Class<?>[] inViews)
    {
        return FilteredBeanPropertyWriter.constructViewBased(writer, inViews);
    }
    
    protected PropertyBuilder constructPropertyBuilder(SerializationConfig config,
                                                       BasicBeanDescription beanDesc)
    {
        return new PropertyBuilder(config, beanDesc);
    }

    /*
    *****************************************************************
    * Internal helper methods
    *****************************************************************
     */

    /**
     * Helper method that will sort given List of properties according
     * to defined criteria (usually detected by annotations)
     */
    List<BeanPropertyWriter> _sortBeanProperties(List<BeanPropertyWriter> props,
                                                 List<String> creatorProps, String[] propertyOrder, boolean sort)
    {
        int size = props.size();
        Map<String,BeanPropertyWriter> all;
        // Need to (re)sort alphabetically?
        if (sort) {
            all = new TreeMap<String,BeanPropertyWriter>();
        } else {
            all = new LinkedHashMap<String,BeanPropertyWriter>(size*2);
        }

        for (BeanPropertyWriter w : props) {
            all.put(w.getName(), w);
        }
        Map<String,BeanPropertyWriter> ordered = new LinkedHashMap<String,BeanPropertyWriter>(size*2);
        // Ok: primarily by explicit order
        if (propertyOrder != null) {
            for (String name : propertyOrder) {
                BeanPropertyWriter w = all.get(name);
                if (w != null) {
                    ordered.put(name, w);
                }
            }            
        }
        // And secondly by sorting Creator properties before other unordered properties
        for (String name : creatorProps) {
            BeanPropertyWriter w = all.get(name);
            if (w != null) {
                ordered.put(name, w);
            }
        }
        // And finally whatever is left (trying to put again will not change ordering)
        ordered.putAll(all);
        return new ArrayList<BeanPropertyWriter>(ordered.values());
    }
}
