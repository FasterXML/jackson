package org.codehaus.jackson.xc;

import java.beans.Introspector;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.Versioned;
import org.codehaus.jackson.annotate.JsonAutoDetect.Visibility;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonCachable;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.*;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdTypeResolverBuilder;
import org.codehaus.jackson.map.util.BeanUtil;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.VersionUtil;

/**
 * Annotation introspector that leverages JAXB annotations where applicable to JSON mapping.
 * <p/>
 * The following JAXB annotations are not supported yet (but some may be supported in future)
 * <ul>
 * <li>{@link XmlAnyAttribute} not yet used (as of 1.5) but may be in future (as an alias for @JsonAnySetter?)
 * <li>{@link XmlAnyElement} not yet used, may be as per [JACKSON-253]
 * <li>{@link javax.xml.bind.annotation.XmlAttachmentRef}: JSON does not support external attachments
 * <li>{@link XmlElementDecl}
 * <li>{@link XmlElementRefs} because Jackson doesn't have any support for 'named' collection items -- however,
 *    this may become partially supported as per [JACKSON-253].
 * <li>{@link XmlID} because Jackson doesn't support referential integrity. NOTE: this too may be supported
 *   in future if/when id references are handled
 * <li>{@link XmlIDREF} same as <code>XmlID</code>
 * <li>{@link javax.xml.bind.annotation.XmlInlineBinaryData} since the underlying concepts
 *    (like XOP) do not exist in JSON -- Jackson will always use inline base64 encoding as the method
 * <li>{@link javax.xml.bind.annotation.XmlList} because JSON does have (or necessarily need)
 *    method of serializing list of values as space-separated Strings
 * <li>{@link javax.xml.bind.annotation.XmlMimeType}
 * <li>{@link javax.xml.bind.annotation.XmlMixed} since JSON has no concept of mixed content
 * <li>{@link XmlRegistry}
 * <li>{@link XmlRootElement} is recognized and used (as of 1.7) for defining root wrapper name (if used)
 * <li>{@link XmlSchema} not used, unlikely to be used
 * <li>{@link XmlSchemaType} not used, unlikely to be used
 * <li>{@link XmlSchemaTypes} not used, unlikely to be used
 * <li>{@link XmlSeeAlso} not needed for anything currently (could theoretically be useful
 *    for locating subtypes for Polymorphic Type Handling)
 * </ul>
 *
 * Note also the following limitations:
 *
 * <ul>
 * <li>Any property annotated with {@link XmlValue} will have a property named 'value' on its JSON object.
 * </ul>
 *
 * @author Ryan Heaton
 * @author Tatu Saloranta
 */
public class JaxbAnnotationIntrospector
    extends AnnotationIntrospector
    implements Versioned
{
    protected final static String MARKER_FOR_DEFAULT = "##default";

    protected final String _jaxbPackageName;
    protected final JsonSerializer<?> _dataHandlerSerializer;
    protected final JsonDeserializer<?> _dataHandlerDeserializer;

    public JaxbAnnotationIntrospector()
    {
        _jaxbPackageName = XmlElement.class.getPackage().getName();

        JsonSerializer<?> dataHandlerSerializer = null;
        JsonDeserializer<?> dataHandlerDeserializer = null;
        /* Data handlers included dynamically, to try to prevent issues on platforms
         * with less than complete support for JAXB API
         */
        try {
            dataHandlerSerializer = (JsonSerializer<?>) Class.forName("org.codehaus.jackson.xc.DataHandlerJsonSerializer").newInstance();
            dataHandlerDeserializer = (JsonDeserializer<?>) Class.forName("org.codehaus.jackson.xc.DataHandlerJsonDeserializer").newInstance();
        } catch (Throwable e) {
            //dataHandlers not supported...
        }
        _dataHandlerSerializer = dataHandlerSerializer;
        _dataHandlerDeserializer = dataHandlerDeserializer;
    }

    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     * 
     * @since 1.6
     */
    @Override
    public Version version() {
        return VersionUtil.versionFor(getClass());
    }

    /*
    /**********************************************************
    /* General annotation properties
    /**********************************************************
     */

    /**
     * An annotation is handled if it's in the same package as @XmlElement, including subpackages.
     *
     * @param ann The annotation.
     * @return Whether the annotation is in the JAXB package.
     */
    @Override
    public boolean isHandled(Annotation ann)
    {
        /* note: class we want is the annotation class, not instance
         * (since annotation instances, like enums, may be of different
         * physical type!)
         */
        Class<?> cls = ann.annotationType();
        Package pkg = cls.getPackage();
        String pkgName = (pkg != null) ? pkg.getName() : cls.getName();
        if (pkgName.startsWith(_jaxbPackageName)) {
            return true;
        }
        // as per [JACKSON-472], also need to recognize @JsonCachable
        if (cls == JsonCachable.class) {
            return true;
        }
        return false;
    }
    
    /*
    /**********************************************************
    /* General class annotations
    /**********************************************************
     */

    @Override
    public Boolean findCachability(AnnotatedClass ac)
    {
        /* 30-Jan-2011, tatu: As per [JACKSON-472], we may want to also
         *    check Jackson annotation here, because sometimes JAXB
         *    introspector is used alone...
         */
        JsonCachable ann = ac.getAnnotation(JsonCachable.class);
        if (ann != null) {
            return ann.value() ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;
    }

    @Override
    public String findRootName(AnnotatedClass ac)
    {
        XmlRootElement elem = findRootElementAnnotation(ac);
        if (elem != null) {
            String name = elem.name();
            // default means "derive from class name"; so we'll return ""
            return MARKER_FOR_DEFAULT.equals(name) ? "" : name;
        }
        return null;
    }

    @Override
    public String[] findPropertiesToIgnore(AnnotatedClass ac) {
        // nothing in JAXB for this?
        return null;
    }

    @Override
    public Boolean findIgnoreUnknownProperties(AnnotatedClass ac) {
        /* 08-Nov-2009, tatus: This is bit trickier: by default JAXB
         * does actually ignore all unknown properties.
         * But since there is no annotation to
         * specify or change this, it seems wrong to claim such setting
         * is in effect. May need to revisit this issue in future
         */
        return null;
    }

    @Override
    public Boolean isIgnorableType(AnnotatedClass ac) {
        // Does JAXB have any such indicators? No?
        return null;
    }

    /*
    /**********************************************************
    /* General member (field, method/constructor) annotations
    /**********************************************************
     */

    @Override
    public boolean hasIgnoreMarker(AnnotatedMember m) {
        return m.getAnnotation(XmlTransient.class) != null;
    }

    /*
    /**********************************************************
    /* Property auto-detection
    /**********************************************************
     */
    
    @Override
    public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
        VisibilityChecker<?> checker)
    {
        XmlAccessType at = findAccessType(ac);
        if (at == null) {
            /* JAXB default is "PUBLIC_MEMBER"; however, here we should not
             * override settings if there is no annotation -- that would mess
             * up global baseline. Fortunately Jackson defaults are very close
             * to JAXB 'PUBLIC_MEMBER' settings (considering that setters and
             * getters must come in pairs)
             */
            return checker;
        }
        
        // Note: JAXB does not do creator auto-detection, can (and should) ignore
        switch (at) {
        case FIELD: // all fields, independent of visibility; no methods
            return checker.withFieldVisibility(Visibility.ANY)
                .withSetterVisibility(Visibility.NONE)
                .withGetterVisibility(Visibility.NONE)
                .withIsGetterVisibility(Visibility.NONE)
                ;
        case NONE: // no auto-detection
            return checker.withFieldVisibility(Visibility.NONE)
            .withSetterVisibility(Visibility.NONE)
            .withGetterVisibility(Visibility.NONE)
            .withIsGetterVisibility(Visibility.NONE)
            ;
        case PROPERTY:
            return checker.withFieldVisibility(Visibility.NONE)
            .withSetterVisibility(Visibility.PUBLIC_ONLY)
            .withGetterVisibility(Visibility.PUBLIC_ONLY)
            .withIsGetterVisibility(Visibility.PUBLIC_ONLY)
            ;
        case PUBLIC_MEMBER:       
            return checker.withFieldVisibility(Visibility.PUBLIC_ONLY)
            .withSetterVisibility(Visibility.PUBLIC_ONLY)
            .withGetterVisibility(Visibility.PUBLIC_ONLY)
            .withIsGetterVisibility(Visibility.PUBLIC_ONLY)
            ;
        }
        return checker;
    }

    /**
     * Method for locating JAXB {@link XmlAccessType} annotation value
     * for given annotated entity, if it has one, or inherits one from
     * its ancestors (in JAXB sense, package etc). Returns null if
     * nothing has been explicitly defined.
     */
    protected XmlAccessType findAccessType(Annotated ac)
    {
        XmlAccessorType at = findAnnotation(XmlAccessorType.class, ac, true, true, true);
        return (at == null) ? null : at.value();
    }
    
    /*
    /**********************************************************
    /* Class annotations for PM type handling (1.5+)
    /**********************************************************
     */
    
    @Override
    public TypeResolverBuilder<?> findTypeResolver(MapperConfig<?> config,
            AnnotatedClass ac, JavaType baseType)
    {
        // no per-class type resolvers, right?
        return null;
    }

    @Override
    public TypeResolverBuilder<?> findPropertyTypeResolver(MapperConfig<?> config,
            AnnotatedMember am, JavaType baseType)
    {
        /* First: @XmlElements and @XmlElementRefs only applies type for immediate property, if it
         * is NOT a structured type.
         */
        if (baseType.isContainerType()) return null;
        return _typeResolverFromXmlElements(am);
    }

    @Override
    public TypeResolverBuilder<?> findPropertyContentTypeResolver(MapperConfig<?> config,
            AnnotatedMember am, JavaType containerType)
    {
        /* First: let's ensure property is a container type: caller should have
         * verified but just to be sure
         */
        if (!containerType.isContainerType()) {
            throw new IllegalArgumentException("Must call method with a container type (got "+containerType+")");
        }
        return _typeResolverFromXmlElements(am);
    }

    protected TypeResolverBuilder<?> _typeResolverFromXmlElements(AnnotatedMember am)
    {
        /* If simple type, @XmlElements and @XmlElementRefs are applicable.
         * Note: @XmlElement and @XmlElementRef are NOT handled here, since they
         * are handled specifically as non-polymorphic indication
         * of the actual type
         */
        XmlElements elems = findAnnotation(XmlElements.class, am, false, false, false);
        XmlElementRefs elemRefs = findAnnotation(XmlElementRefs.class, am, false, false, false);
        if (elems == null && elemRefs == null) {
            return null;
        }

        TypeResolverBuilder<?> b = new StdTypeResolverBuilder();
        // JAXB always uses type name as id
        b = b.init(JsonTypeInfo.Id.NAME, null);
        // and let's consider WRAPPER_OBJECT to be canonical inclusion method
        b = b.inclusion(JsonTypeInfo.As.WRAPPER_OBJECT);
        return b;        
    }
    
    @Override
    public List<NamedType> findSubtypes(Annotated a)
    {
        // No package/superclass defaulting (only used with fields, methods)
        XmlElements elems = findAnnotation(XmlElements.class, a, false, false, false);
        if (elems != null) {
            ArrayList<NamedType> result = new ArrayList<NamedType>();
            for (XmlElement elem : elems.value()) {
                String name = elem.name();
                if (MARKER_FOR_DEFAULT.equals(name)) name = null;
                result.add(new NamedType(elem.type(), name));
            }
            return result;
        }
        else {
            XmlElementRefs elemRefs = findAnnotation(XmlElementRefs.class, a, false, false, false);
            if (elemRefs != null) {
                ArrayList<NamedType> result = new ArrayList<NamedType>();
                for (XmlElementRef elemRef : elemRefs.value()) {
                    Class<?> refType = elemRef.type();
                    // only good for types other than JAXBElement (which is XML based)
                    if (!JAXBElement.class.isAssignableFrom(refType)) {
                        // [JACKSON-253] first consider explicit name declaration
                        String name = elemRef.name();
                        if (name == null || MARKER_FOR_DEFAULT.equals(name)) {
                            XmlRootElement rootElement = (XmlRootElement) refType.getAnnotation(XmlRootElement.class);
                            if (rootElement != null) {
                                name = rootElement.name();
                            }
                        }
                        if (name == null || MARKER_FOR_DEFAULT.equals(name)) {
                            name = Introspector.decapitalize(refType.getSimpleName());
                        }
                        result.add(new NamedType(refType, name));
                    }
                }
                return result;
            }
        }
        return null;
    }

    @Override
    public String findTypeName(AnnotatedClass ac) {
        XmlType type = findAnnotation(XmlType.class, ac, false, false, false);
        if (type != null) {
            String name = type.name();
            if (!MARKER_FOR_DEFAULT.equals(name)) return name;
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* General method annotations
    /**********************************************************
     */

    @Override
    public boolean isIgnorableMethod(AnnotatedMethod m)
    {
        return m.getAnnotation(XmlTransient.class) != null;
    }

    @Override
    public boolean isIgnorableConstructor(AnnotatedConstructor c)
    {
        /* @XmlTransient can not be attached to constructors...
         * so there seems to be no way to do this. But then again,
         * JAXB does not use non-default constructors anyway.
         */
        return false;
    }

    /*
    /**********************************************************
    /* General field annotations
    /**********************************************************
     */

    @Override
    public boolean isIgnorableField(AnnotatedField f)
    {
        return f.getAnnotation(XmlTransient.class) != null;
    }

    /*
    /**********************************************************
    /* Serialization: general annotations
    /**********************************************************
     */

    @Override
    public JsonSerializer<?> findSerializer(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am, true);
        if (adapter != null) {
            return new XmlAdapterJsonSerializer(adapter);
        }
        // [JACKSON-150]: add support for additional core XML types needed by JAXB
        Class<?> type = am.getRawType();
        if (type != null) {
            if (_dataHandlerSerializer != null && isDataHandler(type)) {
                return _dataHandlerSerializer;
            }
        }
        return null;
    }

    /**
     * Determines whether the type is assignable to class javax.activation.DataHandler without requiring that class
     * to be on the classpath.
     *
     * @param type The type.
     * @return Whether the type is assignable to class javax.activation.DataHandler
     */
    private boolean isDataHandler(Class<?> type)
    {
        return type != null && (Object.class != type)
               && (("javax.activation.DataHandler".equals(type.getName()) || isDataHandler(type.getSuperclass())));
    }

    @Override
    public Class<?> findSerializationType(Annotated a)
    {
        // As per [JACKSON-416], need to allow coercing serialization type...
        /* false for class, package, super-class, since annotation can
         * only be attached to fields and methods
         */
        // Note: caller does necessary sub/supertype checks
        XmlElement annotation = findAnnotation(XmlElement.class, a, false, false, false);
        if (annotation == null || annotation.type() == XmlElement.DEFAULT.class) {
            return null;
        }
        /* [JACKSON-436]: Apparently collection types (array, Collection, maybe Map)
         *   require type definition to relate to contents, not collection type
         *   itself. So; we must return null here for those cases, and modify content
         *   type on another method.
         */
        Class<?> rawPropType = a.getRawType();
        if (isIndexedType(rawPropType)) {
            return null;
        }
        /* [JACKSON-288]: Further, JAXB has peculiar notion of declaring intermediate
         *  (and, for the most part, useless) type... So basically we better
         *  just ignore type if there is adapter annotation
         *  (we could check to see if intermediate type is compatible, but let's not yet
         *  bother)
         * 
         */
        Class<?> allegedType = annotation.type();
        if (a.getAnnotation(XmlJavaTypeAdapter.class) != null) {
            return null;
        }
        return allegedType;
    }

    /**
     * Implementation of this method is slightly tricky, given that JAXB defaults differ
     * from Jackson defaults. As of version 1.5 and above, this is resolved by honoring
     * Jackson defaults (which are configurable), and only using JAXB explicit annotations.
     */
    @Override
    public JsonSerialize.Inclusion findSerializationInclusion(Annotated a, JsonSerialize.Inclusion defValue)
    {
        XmlElementWrapper w = a.getAnnotation(XmlElementWrapper.class);
        if (w != null) {
            return w.nillable() ? JsonSerialize.Inclusion.ALWAYS : JsonSerialize.Inclusion.NON_NULL;
        }
        XmlElement e = a.getAnnotation(XmlElement.class);
        if (e != null) {
            return e.nillable() ? JsonSerialize.Inclusion.ALWAYS : JsonSerialize.Inclusion.NON_NULL;
        }
        /* [JACKSON-256]: better pass default value through, if no explicit direction indicating
         * otherwise
         */
        return defValue;
    }

    @Override
    public JsonSerialize.Typing findSerializationTyping(Annotated a)
    {
        return null;
    }

    @Override
    public Class<?>[] findSerializationViews(Annotated a)
    {
        // no JAXB annotations for views (can use different schemas)
        return null;
    }
    
    /*
    /**********************************************************
    /* Serialization: class annotations
    /**********************************************************
     */

    @Override
    public String[] findSerializationPropertyOrder(AnnotatedClass ac)
    {
        // @XmlType.propOrder fits the bill here:
        XmlType type = findAnnotation(XmlType.class, ac, true, true, true);
        if (type == null) {
            return null;
        }
        String[] order = type.propOrder();
        if (order == null || order.length == 0) {
            return null;
        }
        return order;
    }

    @Override
    public Boolean findSerializationSortAlphabetically(AnnotatedClass ac) {
        // Yup, XmlAccessorOrder can provide this...
        XmlAccessorOrder order = findAnnotation(XmlAccessorOrder.class, ac, true, true, true);
        return (order == null) ? null : (order.value() == XmlAccessOrder.ALPHABETICAL);
    }

    /*
    /**********************************************************
    /* Serialization: method annotations
    /**********************************************************
     */

    @Override
    public String findGettablePropertyName(AnnotatedMethod am)
    {
        if (!isVisible(am)) {
            return null;
        }
        String name = findJaxbPropertyName(am, am.getRawType(),
                BeanUtil.okNameForGetter(am));
        if (name == null) {
        }
        return name;
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        //since jaxb says @XmlValue can exist with attributes, this won't map as a JSON value.
        return false;
    }

    /**
     *<p>
     * !!! 12-Oct-2009, tatu: This is hideously slow implementation,
     *   called potentially for every single enum value being
     *   serialized. Should improve...
     */
    @Override
    public String findEnumValue(Enum<?> e)
    {
        Class<?> enumClass = e.getDeclaringClass();
        String enumValue = e.name();
        try {
            XmlEnumValue xmlEnumValue = enumClass.getDeclaredField(enumValue).getAnnotation(XmlEnumValue.class);
            return (xmlEnumValue != null) ? xmlEnumValue.value() : enumValue;
        } catch (NoSuchFieldException e1) {
            throw new IllegalStateException("Could not locate Enum entry '"+enumValue+"' (Enum class "+enumClass.getName()+")", e1);
        }
    }

    /*
    /**********************************************************
    /* Serialization: field annotations
    /**********************************************************
     */

    @Override
    public String findSerializablePropertyName(AnnotatedField af)
    {
        if (!isVisible(af)) {
            return null;
        }
        String name = findJaxbPropertyName(af, af.getRawType(), null);
        /* This may seem wrong, but since JAXB field auto-detection
         * needs to find even non-public fields (if enabled by
         * JAXB access type), we need to return name like so:
         */
        return (name == null) ? af.getName() : name;
    }

    /*
    /**********************************************************
    /* Deserialization: general annotations
    /**********************************************************
    */

    @Override
    public JsonDeserializer<?> findDeserializer(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am, false);
        if (adapter != null) {
            return new XmlAdapterJsonDeserializer(adapter);
        }

        /* [JACKSON-150]: add support for additional core XML
         * types needed by JAXB
         */
        Class<?> type = am.getRawType();
        if (type != null) {
            if (_dataHandlerDeserializer != null && isDataHandler(type)) {
                return _dataHandlerDeserializer;
            }
        }

        return null;
    }

    @Override
    public Class<KeyDeserializer> findKeyDeserializer(Annotated am)
    {
        // Is there something like this in JAXB?
        return null;
    }

    @Override
    public Class<JsonDeserializer<?>> findContentDeserializer(Annotated am)
    {
        // Is there something like this in JAXB?
        return null;
    }

    /**
     * JAXB does allow specifying (more) concrete class for
     * deserialization by using \@XmlElement annotation.
     */
    @Override
    public Class<?> findDeserializationType(Annotated a, JavaType baseType, String propName)
    {
        /* First: only applicable for non-structured types (yes, JAXB annotations
         * are tricky)
         */
        if (!baseType.isContainerType()) {
            return _doFindDeserializationType(a, baseType, propName);
        }
        return null;
    }

    @Override
    public Class<?> findDeserializationKeyType(Annotated am, JavaType baseKeyType,
            String propName)
    {
        return null;
    }

    @Override
    public Class<?> findDeserializationContentType(Annotated a, JavaType baseContentType, String propName)
    {
        /* 15-Feb-2010, tatus: JAXB usage of XmlElement/XmlElements is really
         *   confusing: sometimes it's for type (non-container types), sometimes for
         *   contents (container) types. I guess it's frugal to reuse these... but
         *   I think it's rather short-sighted. Whatever, it is what it is, and here
         *   we are being given content type explicitly.
         */
        return _doFindDeserializationType(a, baseContentType, propName);
    }

    protected Class<?> _doFindDeserializationType(Annotated a, JavaType baseType, String propName)
    {
        /* As per [JACKSON-288], @XmlJavaTypeAdapter will complicate handling of type
         * information; basically we better just ignore type we might find here altogether
         * in that case
         */
        if (a.hasAnnotation(XmlJavaTypeAdapter.class)) {
            return null;
        }
        
        /* false for class, package, super-class, since annotation can
         * only be attached to fields and methods
         */
        XmlElement annotation = findAnnotation(XmlElement.class, a, false, false, false);
        if (annotation != null) {
            Class<?> type = annotation.type();
            if (type != XmlElement.DEFAULT.class) {
                return type;
            }
        }
        /* 16-Feb-2010, tatu: May also have annotation associated with field, not method
         *    itself... and findAnnotation() won't find that (nor property descriptor)
         */
        if ((a instanceof AnnotatedMethod) && propName != null) {
            AnnotatedMethod am = (AnnotatedMethod) a;
            annotation = this.findFieldAnnotation(XmlElement.class, am.getDeclaringClass(), propName);
            if (annotation != null && annotation.type() != XmlElement.DEFAULT.class) {
                return annotation.type();
            }
        }
        return null;
    }

    @Override
    public String findSettablePropertyName(AnnotatedMethod am)
    {
        if (!isVisible(am)) {
            return null;
        }
        Class<?> rawType = am.getParameterClass(0);
        String name = findJaxbPropertyName(am, rawType, BeanUtil.okNameForSetter(am));
        return name;
    }

    @Override
    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        //(ryan) JAXB has @XmlAnyAttribute and @XmlAnyElement annotations, but they're not applicable in this case
        // because JAXB says those annotations are only applicable to methods with specific signatures
        // that Jackson doesn't support (Jackson's any setter needs 2 arguments, name and value, whereas
        // JAXB expects use of Map
        return false;
    }

    @Override
    public boolean hasCreatorAnnotation(Annotated am)
    {
        return false;
    }

    @Override
    public String findDeserializablePropertyName(AnnotatedField af)
    {
        if (!isVisible(af)) {
            return null;
        }
        String name = findJaxbPropertyName(af, af.getRawType(), null);
        /* This may seem wrong, but since JAXB field auto-detection
         * needs to find even non-public fields (if enabled by
         * JAXB access type), we need to return name like so:
         */
        return (name == null) ? af.getName() : name;
    }

    /*
    /**********************************************************
    /* Deserialization: parameters annotations
    /**********************************************************
     */

    @Override
    public String findPropertyNameForParam(AnnotatedParameter param)
    {
        // JAXB has nothing like this...
        return null;
    }

    /*
    /**********************************************************
    /* Helper methods (non-API)
    /**********************************************************
     */

    /**
     * Whether the specified field is invisible, per the JAXB visibility rules.
     *
     * @param f The field.
     * @return Whether the field is invisible.
     */
    private boolean isVisible(AnnotatedField f)
    {
        // TODO: use AnnotatedField's annotations directly
        for (Annotation annotation : f.getAnnotated().getDeclaredAnnotations()) {
            if (isHandled(annotation)) {
                return true;
            }
        }
        XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
        XmlAccessorType at = findAnnotation(XmlAccessorType.class, f, true, true, true);
        if (at != null) {
            accessType = at.value();
        }
        if (accessType == XmlAccessType.FIELD) {
            return true;
        }
        if (accessType == XmlAccessType.PUBLIC_MEMBER) {
            return Modifier.isPublic(f.getAnnotated().getModifiers());
        }
        return false;
    }

    private boolean isVisible(AnnotatedMethod m)
    {
        // TODO: use AnnotatedField's annotations directly
        for (Annotation annotation : m.getAnnotated().getDeclaredAnnotations()) {
            if (isHandled(annotation)) {
                return true;
            }
        }
        XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
        XmlAccessorType at = findAnnotation(XmlAccessorType.class, m, true, true, true);
        if (at != null) {
            accessType = at.value();
        }
        if (accessType == XmlAccessType.PROPERTY || accessType == XmlAccessType.PUBLIC_MEMBER) {
            return Modifier.isPublic(m.getModifiers());
        }
        return false;
    }
    
    /**
     * Finds an annotation associated with given annotatable thing; or if
     * not found, a default annotation it may have (from super class, package
     * and so on)
     *
     * @param annotationClass the annotation class.
     * @param annotated The annotated element.
     * @param includePackage Whether the annotation can be found on the package of the annotated element.
     * @param includeClass Whether the annotation can be found on the class of the annotated element.
     * @param includeSuperclasses Whether the annotation can be found on any superclasses of the class of the annotated element.
     * @return The annotation, or null if not found.
     */
    private <A extends Annotation> A findAnnotation(Class<A> annotationClass, Annotated annotated,
            boolean includePackage, boolean includeClass, boolean includeSuperclasses)
    {
        A annotation = annotated.getAnnotation(annotationClass);
        if (annotation != null) {
            return annotation;
        }
        Class<?> memberClass = null;
        /* 13-Feb-2011, tatu: [JACKSON-495] - need to handle AnnotatedParameter
         *   bit differently, since there is no JDK counterpart. We can still
         *   access annotations directly, just using different calls.
         */
        if (annotated instanceof AnnotatedParameter) {
            memberClass = ((AnnotatedParameter) annotated).getDeclaringClass();
        } else {
            AnnotatedElement annType = annotated.getAnnotated();
            if (annType instanceof Member) {
                memberClass = ((Member) annType).getDeclaringClass();
                if (includeClass) {
                    annotation = (A) memberClass.getAnnotation(annotationClass);
                    if (annotation != null) {
                        return annotation;
                    }
                }
            } else if (annType instanceof Class<?>) {
                memberClass = (Class<?>) annType;
            } else {
                throw new IllegalStateException("Unsupported annotated member: " + annotated.getClass().getName());
            }
        }
        if (memberClass != null) {
            if (includeSuperclasses) {
                Class<?> superclass = memberClass.getSuperclass();
                while (superclass != null && superclass != Object.class) {
                    annotation = (A) superclass.getAnnotation(annotationClass);
                    if (annotation != null) {
                        return annotation;
                    }
                    superclass = superclass.getSuperclass();
                }
            }
            if (includePackage) {
                Package pkg = memberClass.getPackage();
                if (pkg != null) {
                    return memberClass.getPackage().getAnnotation(annotationClass);
                }
            }
        }
        return null;
    }

    /**
     * Helper method for locating field on given class, checking if
     * it has specified annotation, and returning it if found.
     * 
     * @since 1.5
     */
    private <A extends Annotation> A findFieldAnnotation(Class<A> annotationType,
            Class<?> cls, String fieldName)
    {
        do {
            for (Field f : cls.getDeclaredFields()) {
                if (fieldName.equals(f.getName())) {
                    return f.getAnnotation(annotationType);
                }
            }
            if (cls.isInterface() || cls == Object.class) {
                break;
            }
            cls = cls.getSuperclass();
        } while (cls != null);
        return null;
    }

    /*
    /**********************************************************
    /* Helper methods for bean property introspection
    /**********************************************************
     */

    private static String findJaxbPropertyName(Annotated ae, Class<?> aeType, String defaultName)
    {
        XmlElementWrapper elementWrapper = ae.getAnnotation(XmlElementWrapper.class);
        if (elementWrapper != null) {
            String name = elementWrapper.name();
            if (!MARKER_FOR_DEFAULT.equals(name)) {
                return name;
            }
            return defaultName;
        }

        XmlAttribute attribute = ae.getAnnotation(XmlAttribute.class);
        if (attribute != null) {
            String name = attribute.name();
            if (!MARKER_FOR_DEFAULT.equals(name)) {
                return name;
            }
            return defaultName;
        }
        XmlElement element = ae.getAnnotation(XmlElement.class);
        if (element != null) {
            String name = element.name();
            if (!MARKER_FOR_DEFAULT.equals(name)) {
                return name;
            }
            return defaultName;
        }
        XmlElementRef elementRef = ae.getAnnotation(XmlElementRef.class);
        if (elementRef != null) {
            String name = elementRef.name();
            if (!MARKER_FOR_DEFAULT.equals(name)) {
                return name;
            }
            if (aeType != null) {
                XmlRootElement rootElement = (XmlRootElement) aeType.getAnnotation(XmlRootElement.class);
                if (rootElement != null) {
                    name = rootElement.name();
                    if (!MARKER_FOR_DEFAULT.equals(name)) {
                        return name;
                    }
                    return Introspector.decapitalize(aeType.getSimpleName());
                }
            }
        }

        XmlValue valueInfo = ae.getAnnotation(XmlValue.class);
        if (valueInfo != null) {
            return "value";
        }

        return null;
    }

    private XmlRootElement findRootElementAnnotation(AnnotatedClass ac)
    {
        // Yes, check package, no class (already included), yes superclasses
        return findAnnotation(XmlRootElement.class, ac, true, false, true);
    }

    /**
     * Finds the XmlAdapter for the specified annotation.
     *
     * @param am The annotated element.
     * @param forSerialization If true, adapter for serialization; if false, for deserialization
     * 
     * @return The adapter, or null if none.
     */
    private XmlAdapter<Object,Object> findAdapter(Annotated am, boolean forSerialization)
    {
        // First of all, are we looking for annotations for class?
        if (am instanceof AnnotatedClass) {
            return findAdapterForClass((AnnotatedClass) am, forSerialization);
        }
        // Otherwise for a member. First, let's figure out type of property
        Class<?> memberType = am.getRawType();
        // ok; except for setters...
        if (memberType == Void.TYPE && (am instanceof AnnotatedMethod)) {
            memberType = ((AnnotatedMethod) am).getParameterClass(0);
        }

        // 09-Nov-2010, tatu: Not quite sure why we are to check declaring class... but that's how code was:
        Member member = (Member) am.getAnnotated();
        // [JACKSON-495]: Will be null for AnnotatedParam -- note, probably should find declaring class for it; won't for now
        if (member != null) {
            Class<?> potentialAdaptee = member.getDeclaringClass();
            if (potentialAdaptee != null) {
                XmlJavaTypeAdapter adapterInfo = (XmlJavaTypeAdapter) potentialAdaptee.getAnnotation(XmlJavaTypeAdapter.class);
                if (adapterInfo != null) { // should we try caching this?
                    XmlAdapter<Object,Object> adapter = checkAdapter(adapterInfo, memberType);
                    if (adapter != null) {
                        return adapter;
                    }
                }
            }
        }

        XmlJavaTypeAdapter adapterInfo = findAnnotation(XmlJavaTypeAdapter.class, am, true, false, false);
        if (adapterInfo != null) {
            XmlAdapter<Object,Object> adapter = checkAdapter(adapterInfo, memberType);
            if (adapter != null) {
                return adapter;
            }
        }
        XmlJavaTypeAdapters adapters = findAnnotation(XmlJavaTypeAdapters.class, am, true, false, false);
        if (adapters != null) {
            for (XmlJavaTypeAdapter info : adapters.value()) {
                XmlAdapter<Object,Object> adapter = checkAdapter(info, memberType);
                if (adapter != null) {
                    return adapter;
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private final XmlAdapter<Object,Object> checkAdapter(XmlJavaTypeAdapter adapterInfo, Class<?> typeNeeded)
    {
        // if annotation has no type, it's applicable; if it has, must match
        Class<?> adaptedType = adapterInfo.type();
        if (adaptedType == XmlJavaTypeAdapter.DEFAULT.class
                || adaptedType.isAssignableFrom(typeNeeded)) {
            @SuppressWarnings("rawtypes")
            Class<? extends XmlAdapter> cls = adapterInfo.value();
            return ClassUtil.createInstance(cls, false);
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private XmlAdapter<Object,Object> findAdapterForClass(AnnotatedClass ac, boolean forSerialization)
    {
        /* As per [JACKSON-411], XmlJavaTypeAdapter should not be inherited from super-class.
         * It would still be nice to be able to use mix-ins; but unfortunately we seem to lose
         * knowledge of class that actually declared the annotation. Thus, we'll only accept
         * declaration from specific class itself.
         */
        XmlJavaTypeAdapter adapterInfo = ac.getAnnotated().getAnnotation(XmlJavaTypeAdapter.class);
        if (adapterInfo != null) { // should we try caching this?
            @SuppressWarnings("rawtypes")
            Class<? extends XmlAdapter> cls = adapterInfo.value();
            return ClassUtil.createInstance(cls, false);
        }
        return null;
    }

    /**
     * Helper method used to distinguish structured type, which with JAXB use different
     * rules for defining content types.
     */
    private boolean isIndexedType(Class<?> raw)
    {
        return raw.isArray() || Collection.class.isAssignableFrom(raw)
            || Map.class.isAssignableFrom(raw);
    }
}
