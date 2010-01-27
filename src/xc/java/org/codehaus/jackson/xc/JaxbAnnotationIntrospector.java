package org.codehaus.jackson.xc;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.*;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;

/**
 * Annotation introspector that leverages JAXB annotations where applicable to JSON mapping.
 * <p/>
 * The following JAXB annotations were determined to be not-applicable:
 * <ul>
 * <li>{@link XmlAnyAttribute} because it applies only to Map<QName, String>, which jackson can't serialize
 * <li>{@link XmlAnyElement} because it applies only to JAXBElement, which jackson can't serialize
 * <li>{@link javax.xml.bind.annotation.XmlAttachmentRef}
 * <li>{@link XmlElementDecl}
 * <li>{@link XmlElementRefs} because Jackson doesn't have any support for 'named' collection items.
 * <li>{@link XmlElements} because Jackson doesn't have any support for 'named' collection items.
 *    <br />NOTE: it is possible that this annotation will be supported in future.
 *   </li>
 * <li>{@link XmlID} because jackson' doesn't support referential integrity.
 * <li>{@link XmlIDREF} because jackson' doesn't support referential integrity.
 * <li>{@link javax.xml.bind.annotation.XmlInlineBinaryData}
 * <li>{@link javax.xml.bind.annotation.XmlList} because jackson doesn't support serializing collections to a single string.
 * <li>{@link javax.xml.bind.annotation.XmlMimeType}
 * <li>{@link javax.xml.bind.annotation.XmlMixed}
 * <li>{@link XmlNs}
 * <li>{@link XmlRegistry}
 * <li>{@link XmlRootElement} is not currently used, but it may be used in future for XML compatibility features
 * <li>{@link XmlSchema}
 * <li>{@link XmlSchemaType}
 * <li>{@link XmlSchemaTypes}
 * <li>{@link XmlSeeAlso}
 * </ul>
 *
 * Note also the following limitations:
 *
 * <ul>
 * <li>Any property annotated with {@link XmlValue} will have a property named 'value' on its JSON object.
 * </ul>
 *
 * @author Ryan Heaton
 */
public class JaxbAnnotationIntrospector extends AnnotationIntrospector
{
    final static String MARKER_FOR_DEFAULT = "##default";

    final String _jaxbPackageName;
    final JsonSerializer<?> _dataHandlerSerializer;
    final JsonDeserializer<?> _dataHandlerDeserializer;

    public JaxbAnnotationIntrospector()
    {
        _jaxbPackageName = XmlElement.class.getPackage().getName();

        JsonSerializer<?> dataHandlerSerializer = null;
        JsonDeserializer<?> dataHandlerDeserializer = null;
        try {
            dataHandlerSerializer = (JsonSerializer<?>) Class.forName("org.codehaus.jackson.xc.DataHandlerJsonSerializer").newInstance();
            dataHandlerDeserializer = (JsonDeserializer<?>) Class.forName("org.codehaus.jackson.xc.DataHandlerJsonDeserializer").newInstance();
        } catch (Throwable e) {
            //dataHandlers not supported...
        }
        _dataHandlerSerializer = dataHandlerSerializer;
        _dataHandlerDeserializer = dataHandlerDeserializer;
    }

    /*
    ////////////////////////////////////////////////////
    // General annotation properties
    ////////////////////////////////////////////////////
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
        if (pkg != null) {
            return pkg.getName().startsWith(_jaxbPackageName);
        }
        // not sure if this is needed but...
        return cls.getName().startsWith(_jaxbPackageName);
    }

    /*
    ////////////////////////////////////////////////////
    // General annotations
    ////////////////////////////////////////////////////
     */

    @Override
    public String findNamespace(Annotated ann)
    {
        String ns = null;

        /* 10-Oct-2009, tatus: I suspect following won't work quite
         *  as well as it should, wrt. defaulting to package.
         *  But it should work well enough to get things started --
         *  currently this method is not needed, and when it is,
         *  this can be improved.
         */

        if (ann instanceof AnnotatedClass) {
            /* For classes, it must be @XmlRootElement. Also, we do
             * want to use defaults from package, base class
             */
            XmlRootElement elem = findRootElementAnnotation((AnnotatedClass) ann);
            if (elem != null) {
                ns = elem.namespace();
            }
        } else {
            // For others, XmlElement or XmlAttribute work (anything else?)
            XmlElement elem = findAnnotation(XmlElement.class, ann, false, false, false);
            if (elem != null) {
                ns = elem.namespace();
            }
            if (ns == null || MARKER_FOR_DEFAULT.equals(ns)) {
                XmlAttribute attr = findAnnotation(XmlAttribute.class, ann, false, false, false);
                if (attr != null) {
                    ns = attr.namespace();
                }
            }
        }
        // JAXB uses marker for "not defined"
        if (MARKER_FOR_DEFAULT.equals(ns)) {
            ns = null;
        }
        return ns;
    }

    /**
     *<p>
     * !!! 12-Oct-2009, tatu: This is hideously slow implementation,
     *   called potentially for every single enum value being
     *   serialized. Need to improve somehow
     */
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
    ///////////////////////////////////////////////////////
    // General class annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public Boolean findCachability(AnnotatedClass ac)
    {
        // Nothing to indicate this in JAXB, return "don't care"
        return null;
    }

    @Override
    public Boolean findFieldAutoDetection(AnnotatedClass ac)
    {
        return isFieldsAccessible(ac);
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
    public TypeResolverBuilder<?> findTypeResolver(Annotated a) {
        // @TODO
        return null;
    }

    @Override
    public void findAndAddSubtypes(AnnotatedClass ac, TypeResolverBuilder<?> b) {
        // @TODO
    }
    
    /*
    ///////////////////////////////////////////////////////
    // General method annotations
    ///////////////////////////////////////////////////////
    */

    public boolean isIgnorableMethod(AnnotatedMethod m)
    {
        return m.getAnnotation(XmlTransient.class) != null;

    }

    public boolean isIgnorableConstructor(AnnotatedConstructor c)
    {
        /* @XmlTransient can not be attached to constructors...
         * so there seems to be no way to do this. But then again,
         * JAXB does not use non-default constructors anyway.
         */
        return false;
    }

    /*
    ////////////////////////////////////////////////////
    // General field annotations
    ////////////////////////////////////////////////////
     */

    public boolean isIgnorableField(AnnotatedField f)
    {
        return f.getAnnotation(XmlTransient.class) != null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: general annotations
    ///////////////////////////////////////////////////////
    */

    public JsonSerializer<?> findSerializer(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am);
        if (adapter != null) {
            return new XmlAdapterJsonSerializer(adapter);
        }

        /* [JACKSON-150]: add support for additional core XML
         * types needed by JAXB
         */
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
        return type != null
               && !Object.class.equals(type)
               && (("javax.activation.DataHandler".equals(type.getName()) || isDataHandler(type.getSuperclass())));
    }

    public Class<?> findSerializationType(Annotated a)
    {
        return null;
    }

    /**
     * By default only non-null properties are written (per the JAXB spec.)
     *
     * @return JsonSerialize.Inclusion.NON_NULL
     */
    public JsonSerialize.Inclusion findSerializationInclusion(Annotated a, JsonSerialize.Inclusion defValue)
    {
        if ((a instanceof AnnotatedField) || (a instanceof AnnotatedMethod)) {
            boolean nillable = a.getAnnotation(XmlElementWrapper.class) != null ? a.getAnnotation(XmlElementWrapper.class).nillable() :
                    a.getAnnotation(XmlElement.class) != null && a.getAnnotation(XmlElement.class).nillable();
            return nillable ? JsonSerialize.Inclusion.ALWAYS : JsonSerialize.Inclusion.NON_NULL;
        }
        return JsonSerialize.Inclusion.NON_NULL;
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
    ///////////////////////////////////////////////////////
    // Serialization: class annotations
    ///////////////////////////////////////////////////////
    */

    public Boolean findGetterAutoDetection(AnnotatedClass ac)
    {
        /* Ok: should only return non-null if there is explicit
         * definition; default (of "PUBLIC_MEMBER") should
         * be indicated as null return value
         */
        XmlAccessType at = findAccessType(ac);
        if (at != null) {
            boolean enabled = (at == XmlAccessType.PUBLIC_MEMBER) || (at == XmlAccessType.PROPERTY);
            return enabled ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;
    }

    /**
     * @since 1.3
     */
    public Boolean findIsGetterAutoDetection(AnnotatedClass ac)
    {
        // No difference between regular getters, "is getters"
        return findGetterAutoDetection(ac);
    }

    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        // @XmlType.propOrder fits the bill here:
        XmlType type = findAnnotation(XmlType.class, ac, true, true, true);
        return (type == null) ? null : type.propOrder();
    }

    public Boolean findSerializationSortAlphabetically(AnnotatedClass ac) {
        // Yup, XmlAccessorOrder can provide this...
        XmlAccessorOrder order = findAnnotation(XmlAccessorOrder.class, ac, true, true, true);
        return (order == null) ? null : (order.value() == XmlAccessOrder.ALPHABETICAL);
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: method annotations
    ///////////////////////////////////////////////////////
    */

    public String findGettablePropertyName(AnnotatedMethod am)
    {
        if (isInvisible(am)) {
            return null;
        }
        // null if no annotation is found (can still use auto-detection)
        return findJaxbSpecifiedPropertyName(am);
    }

    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        //since jaxb says @XmlValue can exist with attributes, this won't map as a json value.
        return false;
    }

    /**
     * Whether the specified field is invisible, per the JAXB visibility rules.
     *
     * @param f The field.
     * @return Whether the field is invisible.
     */
    protected boolean isInvisible(AnnotatedField f)
    {
        boolean invisible = true;

        for (Annotation annotation : f.getAnnotated().getDeclaredAnnotations()) {
            if (isHandled(annotation)) {
                //if any JAXB annotations are present, it is NOT ignorable.
                invisible = false;
            }
        }

        if (invisible) {
            XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
            XmlAccessorType at = findAnnotation(XmlAccessorType.class, f, true, true, true);
            if (at != null) {
                accessType = at.value();
            }

            invisible = accessType != XmlAccessType.FIELD &&
                !(accessType == XmlAccessType.PUBLIC_MEMBER && Modifier.isPublic(f.getAnnotated().getModifiers()));
        }
        return invisible;
    }

    /**
     * Whether the specified method (assumed to be a property) is invisible, per the JAXB rules.
     *
     * @param m The method.
     * @return whether the method is invisible.
     */
    protected boolean isInvisible(AnnotatedMethod m)
    {
        boolean invisible = true;
        for (Annotation annotation : m.getAnnotated().getDeclaredAnnotations()) {
            if (isHandled(annotation)) {
                //if any annotations are present, it is NOT ignorable.
                invisible = false;
            }
        }

        if (isPropertiesAccessible(m)) {
            //jaxb only accounts for getter/setter pairs.
            PropertyDescriptor pd = findPropertyDescriptor(m);
            invisible = (pd == null) || pd.getReadMethod() == null || pd.getWriteMethod() == null;
        }
        return invisible;
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: field annotations
    ///////////////////////////////////////////////////////
    */

    public String findSerializablePropertyName(AnnotatedField af)
    {
        if (isInvisible(af)) {
            return null;
        }
        Field field = af.getAnnotated();
        String name = findJaxbPropertyName(field, field.getType(), "");
        /* This may seem wrong, but since JAXB field auto-detection
         * needs to find even non-public fields (if enabled by
         * JAXB access type), we need to return name like so:
         */
        return (name == null) ? field.getName() : name;
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: general annotations
    ///////////////////////////////////////////////////////
    */


    public JsonDeserializer<?> findDeserializer(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am);
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

    public Class<KeyDeserializer> findKeyDeserializer(Annotated am)
    {
        // Is there something like this in JAXB?
        return null;
    }

    public Class<JsonDeserializer<?>> findContentDeserializer(Annotated am)
    {
        // Is there something like this in JAXB?
        return null;
    }

    /**
     * JAXB does allow specifying (more) concrete class for
     * deserialization by using \@XmlElement annotation.
     */
    public Class<?> findDeserializationType(Annotated am)
    {
        /* false for class, package, super-class, since annotation can
         * only be attached to fields and methods
         */
        XmlElement annotation = findAnnotation(XmlElement.class, am, false, false, false);
        if (annotation != null && annotation.type() != XmlElement.DEFAULT.class) {
            return annotation.type();
        }
        return null;
    }

    public Class<?> findDeserializationKeyType(Annotated am)
    {
        return null;
    }

    public Class<?> findDeserializationContentType(Annotated am)
    {
        /* 16-Dec-2009, tatus: I think this is wrong: annotation
         *   really refers to type of property itself, not contents.
         *   Content type can (only?) be effected by @XmlElements (which in
         *   turn contains instances of @XmlElement)
         */
        /*
        XmlElement annotation = findAnnotation(XmlElement.class, am, false, false, false);
        if (annotation != null && annotation.type() != XmlElement.DEFAULT.class) {
            return annotation.type();
        }
        */

        return null;
    }

    public Boolean findSetterAutoDetection(AnnotatedClass ac)
    {
        return isPropertiesAccessible(ac);
    }

    public Boolean findCreatorAutoDetection(AnnotatedClass ac)
    {
        return null;
    }

    public String findSettablePropertyName(AnnotatedMethod am)
    {
        if (isInvisible(am)) {
            return null;
        }
        // null if no annotation is found (can still use auto-detection)
        return findJaxbSpecifiedPropertyName(am);
    }

    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        //(ryan) JAXB has @XmlAnyAttribute and @XmlAnyElement annotations, but they're not applicable in this case
        // because JAXB says those annotations are only applicable to methods with specific signatures
        // that Jackson doesn't support (Jackson's any setter needs 2 arguments, name and value, whereas
        // JAXB expects use of Map
        return false;
    }

    public boolean hasCreatorAnnotation(Annotated am)
    {
        return false;
    }

    @Override
    public String findDeserializablePropertyName(AnnotatedField af)
    {
        if (isInvisible(af)) {
            return null;
        }
        Field field = af.getAnnotated();
        String name = findJaxbPropertyName(field, field.getType(), "");
        /* This may seem wrong, but since JAXB field auto-detection
         * needs to find even non-public fields (if enabled by
         * JAXB access type), we need to return name like so:
         */
        return (name == null) ? field.getName() : name;
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: parameters annotations
    ///////////////////////////////////////////////////////
    */

    public String findPropertyNameForParam(AnnotatedParameter param)
    {
        // JAXB has nothing like this...
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Helper methods (non-API)
    ///////////////////////////////////////////////////////
    */

    /**
     * Finds an annotation.
     *
     * @param annotationClass the annotation class.
     * @param annotated The annotated element.
     * @param includePackage Whether the annotation can be found on the package of the annotated element.
     * @param includeClass Whether the annotation can be found on the class of the annotated element.
     * @param includeSuperclasses Whether the annotation can be found on any superclasses of the class of the annotated element.
     * @return The annotation, or null if not found.
     */
    @SuppressWarnings("unchecked")
    protected <A extends Annotation> A findAnnotation(Class<A> annotationClass, Annotated annotated,
                                                      boolean includePackage, boolean includeClass, boolean includeSuperclasses)
    {
        A annotation = null;
        if (annotated instanceof AnnotatedMethod) {
            PropertyDescriptor pd = findPropertyDescriptor((AnnotatedMethod) annotated);
            if (pd != null) {
                annotation = new AnnotatedProperty(pd).getAnnotation(annotationClass);
            }
        }

        if (annotation == null) {
             annotation = annotated.getAnnotated().getAnnotation(annotationClass);
        }
        if (annotation == null) {
            Class memberClass;
            AnnotatedElement annType = annotated.getAnnotated();
            if (annType instanceof Member) {
                memberClass = ((Member) annType).getDeclaringClass();
                if (includeClass) {
                    annotation = (A) memberClass.getAnnotation(annotationClass);
                }
            } else if (annType instanceof Class) {
                memberClass = (Class) annType;
            } else {
                throw new IllegalStateException("Unsupported annotated member: " + annotated.getClass().getName());
            }

            if (annotation == null) {
                if (includeSuperclasses) {
                    Class superclass = memberClass.getSuperclass();
                    while (superclass != null && !superclass.equals(Object.class) && annotation == null) {
                        annotation = (A) superclass.getAnnotation(annotationClass);
                        superclass = superclass.getSuperclass();
                    }
                }

                if (annotation == null && includePackage) {
                    annotation = memberClass.getPackage().getAnnotation(annotationClass);
                }
            }
        }
        return annotation;
    }

    /**
     * Whether properties are accessible to this class.
     *
     * @param ac The annotated class.
     * @return Whether properties are accessible to this class.
     */
    protected boolean isPropertiesAccessible(Annotated ac)
    {
        XmlAccessType accessType = findAccessType(ac);
        if (accessType == null) {
            // JAXB default is "PUBLIC_MEMBER"
            accessType = XmlAccessType.PUBLIC_MEMBER;
        }
        return (accessType == XmlAccessType.PUBLIC_MEMBER) || (accessType == XmlAccessType.PROPERTY);
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

    /**
     * Whether fields are accessible to this class.
     *
     * @param ac The annotated class.
     * @return Whether fields are accessible to this class.
     */
    protected boolean isFieldsAccessible(Annotated ac)
    {
        XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
        XmlAccessorType at = findAnnotation(XmlAccessorType.class, ac, true, true, true);
        if (at != null) {
            accessType = at.value();
        }
        return accessType == XmlAccessType.PUBLIC_MEMBER || accessType == XmlAccessType.FIELD;
    }

    /**
     * Finds the property descriptor (adapted to AnnotatedElement) for the specified method.
     *
     * @param m The method.
     * @return The property descriptor, or null if not found.
     */
    protected PropertyDescriptor findPropertyDescriptor(AnnotatedMethod m)
    {
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(m.getDeclaringClass());
            PropertyDescriptor[] descriptors = beanInfo.getPropertyDescriptors();
            for (int i = 0; i < descriptors.length; i++) {
                PropertyDescriptor descriptor = descriptors[i];
                if (descriptor.getReadMethod() != null && descriptor.getReadMethod().getName().equals(m.getName())) {
                    return descriptor;
                }
                if (descriptor.getWriteMethod() != null && descriptor.getWriteMethod().getName().equals(m.getName())) {
                    return descriptor;
                }
            }
        } catch (IntrospectionException e) {
            return null;
        }

        return null;
    }

    /**
     * Find the property name for the specified annotated method. Takes into account any JAXB annotation that
     * can be mapped to a JSON property.
     *
     * @param am The annotated method.
     * @return The property name, or null if no JAXB annotation specifies the property name.
     */
    protected String findJaxbSpecifiedPropertyName(AnnotatedMethod am)
    {
        PropertyDescriptor pd = findPropertyDescriptor(am);
        if (pd != null) {
            return findJaxbPropertyName(new AnnotatedProperty(pd), pd.getPropertyType(), "");
        }
        return null;
    }

    /**
     * Find the JAXB property name for the given annotated element.
     *
     * @param ae The annotated element.
     * @param aeType The type of the annotated element.
     * @param defaultName The default name if nothing is specified.
     *
     * @return The JAXB property name, if found (null if no annotations
     *    found); defaultName if annotation has value indicating default
     *    should be used.
     */
    protected String findJaxbPropertyName(AnnotatedElement ae, Class<?> aeType, String defaultName)
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
            XmlRootElement rootElement = (XmlRootElement) aeType.getAnnotation(XmlRootElement.class);
            if (rootElement != null) {
                name = rootElement.name();
                if (!MARKER_FOR_DEFAULT.equals(name)) {
                    return name;
                }
                return Introspector.decapitalize(aeType.getSimpleName());
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
     * @return The adapter, or null if none.
     */
    @SuppressWarnings("unchecked")
    protected XmlAdapter<Object,Object> findAdapter(Annotated am)
    {
        XmlAdapter adapter = null;
        Class potentialAdaptee;
        boolean isMember;
        if (am instanceof AnnotatedClass) {
            potentialAdaptee = ((AnnotatedClass) am).getAnnotated();
            isMember = false;
        }
        else {
            potentialAdaptee = ((Member) am.getAnnotated()).getDeclaringClass();
            isMember = true;
        }

        XmlJavaTypeAdapter adapterInfo = (XmlJavaTypeAdapter) potentialAdaptee.getAnnotation(XmlJavaTypeAdapter.class);
        if (adapterInfo != null) {
            try {
                adapter = adapterInfo.value().newInstance(); //todo: cache this?
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        if (adapter == null && isMember) {
            adapterInfo = findAnnotation(XmlJavaTypeAdapter.class, am, true, false, false);
            if (adapterInfo == null) {
                XmlJavaTypeAdapters adapters = findAnnotation(XmlJavaTypeAdapters.class, am, true, false, false);
                if (adapters != null) {
                    for (XmlJavaTypeAdapter info : adapters.value()) {
                        if (info.type().isAssignableFrom(((Member) am.getAnnotated()).getDeclaringClass())) {
                            adapterInfo = info;
                            break;
                        }
                    }
                }
            }

            if (adapterInfo != null) {
                try {
                    adapter = adapterInfo.value().newInstance(); //todo: cache this?
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        return adapter;
    }

    private static class AnnotatedProperty implements AnnotatedElement {

        private final PropertyDescriptor pd;

        private AnnotatedProperty(PropertyDescriptor pd)
        {
            this.pd = pd;
        }

        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
        {
            return (pd.getReadMethod() != null && pd.getReadMethod().isAnnotationPresent(annotationClass))
                    || (pd.getWriteMethod() != null && pd.getWriteMethod().isAnnotationPresent(annotationClass));
        }

        public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
        {
            T ann = pd.getReadMethod() != null ? pd.getReadMethod().getAnnotation(annotationClass) : null;
            if (ann == null) {
                ann = pd.getWriteMethod() != null ? pd.getWriteMethod().getAnnotation(annotationClass) : null;
            }
            return ann;
        }

        public Annotation[] getAnnotations()
        {
            //not used. we don't need to support this yet.
            throw new UnsupportedOperationException();
        }

        public Annotation[] getDeclaredAnnotations()
        {
            //not used. we don't need to support this yet.
            throw new UnsupportedOperationException();
        }
    }

}
