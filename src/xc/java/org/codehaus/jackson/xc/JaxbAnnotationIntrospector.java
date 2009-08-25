package org.codehaus.jackson.xc;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.*;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import javax.activation.DataHandler;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Member;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;

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
 * <li>{@link XmlID} because jackson' doesn't support referential integrity.
 * <li>{@link XmlIDREF} because jackson' doesn't support referential integrity.
 * <li>{@link javax.xml.bind.annotation.XmlInlineBinaryData}
 * <li>{@link javax.xml.bind.annotation.XmlList} because jackson doesn't support serializing collections to a single string.
 * <li>{@link javax.xml.bind.annotation.XmlMimeType}
 * <li>{@link javax.xml.bind.annotation.XmlMixed}
 * <li>{@link XmlNs}
 * <li>{@link XmlRegistry}
 * <li>{@link XmlRootElement} because there isn't an equivalent element name for a JSON object.
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
    final String _jaxbPackageName;

    public JaxbAnnotationIntrospector()
    {
        _jaxbPackageName = XmlElement.class.getPackage().getName();
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
    ///////////////////////////////////////////////////////
    // General class annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public Boolean findCachability(AnnotatedClass ac)
    {
        return null;
    }

    @Override
    public Boolean findFieldAutoDetection(AnnotatedClass ac)
    {
        return isFieldsAccessible(ac);
    }

    /*
    ///////////////////////////////////////////////////////
    // General method annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public boolean isIgnorableMethod(AnnotatedMethod m)
    {
        if (m.getAnnotation(XmlTransient.class) != null) {
            return true;
        }
        for (Annotation annotation : m.getAnnotated().getDeclaredAnnotations()) {
            if (isHandled(annotation)) {
                //if any annotations are present, it is NOT ignorable.
                return false;
            }
        }

        if (isPropertiesAccessible(m)) {
            //jaxb only accounts for getter/setter pairs.
            PropertyDescriptor pd = findPropertyDescriptor(m);
            return pd == null || pd.getReadMethod() == null || pd.getWriteMethod() == null; 
        }

        return true;
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
    ////////////////////////////////////////////////////
    // General field annotations
    ////////////////////////////////////////////////////
     */

    @Override
    public boolean isIgnorableField(AnnotatedField f)
    {
        if (f.getAnnotation(XmlTransient.class) != null) {
            return true;
        }

        /**
         * 2009-08-19 (heatonra) The following annotation check is necessary,
         * see http://jira.codehaus.org/browse/JACKSON-151
         */

        for (Annotation annotation : f.getAnnotated().getDeclaredAnnotations()) {
            if (isHandled(annotation)) {
                //if any JAXB annotations are present, it is NOT ignorable.
                return false;
            }
        }

        XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
        XmlAccessorType at = findAnnotation(XmlAccessorType.class, f, true, true, true);
        if (at != null) {
            accessType = at.value();
        }
        
        return accessType != XmlAccessType.FIELD &&
            !(accessType == XmlAccessType.PUBLIC_MEMBER && Modifier.isPublic(f.getAnnotated().getModifiers()));
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: general annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public JsonSerializer<?> findSerializer(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am);
        if (adapter != null) {
            return new XmlAdapterJsonSerializer(adapter);
        }

        /* [JACKSON-150]: add support for additional core XML
         * types needed by JAXB
         */
        Class<?> type = am.getType();
        if (type != null) {
            if (DataHandler.class.isAssignableFrom(type)) {
                return new DataHandlerJsonSerializer();
            }
        }


        return null;
    }

    @Override
    public Class<?> findSerializationType(Annotated a)
    {
        return null;
    }

    /**
     * By default only non-null properties are written (per the JAXB spec.)
     *
     * @return JsonSerialize.Inclusion.NON_NULL
     */
    @Override
    public JsonSerialize.Inclusion findSerializationInclusion(Annotated a, JsonSerialize.Inclusion defValue)
    {
        if ((a instanceof AnnotatedField) || (a instanceof AnnotatedMethod)) {
            boolean nillable = a.getAnnotation(XmlElementWrapper.class) != null ? a.getAnnotation(XmlElementWrapper.class).nillable() :
                    a.getAnnotation(XmlElement.class) != null && a.getAnnotation(XmlElement.class).nillable();
            return nillable ? JsonSerialize.Inclusion.ALWAYS : JsonSerialize.Inclusion.NON_NULL;
        }
        return JsonSerialize.Inclusion.NON_NULL;
    }

    public JsonSerialize.Typing findSerializationTyping(Annotated a)
    {
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: class annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public Boolean findGetterAutoDetection(AnnotatedClass ac)
    {
        return isPropertiesAccessible(ac);
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: method annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public String findGettablePropertyName(AnnotatedMethod am)
    {
        String propertyName = findJaxbSpecifiedPropertyName(am);
        // null -> no annotation found
        return (propertyName == null) ? null : propertyName;
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        //since jaxb says @XmlValue can exist with attributes, this won't map as a json value.
        return false;
    }

    @Override
    public String findEnumValue(Enum<?> e)
    {
        String enumValue = e.name();
        XmlEnumValue xmlEnumValue;
        try {
            xmlEnumValue = e.getDeclaringClass().getDeclaredField(e.name()).getAnnotation(XmlEnumValue.class);
        } catch (NoSuchFieldException e1) {
            throw new IllegalStateException(e1);
        }
        enumValue = xmlEnumValue != null ? xmlEnumValue.value() : enumValue;
        return enumValue;
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: field annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public String findSerializablePropertyName(AnnotatedField af)
    {
        Field field = af.getAnnotated();
        return findJaxbPropertyName(field, field.getType(), field.getName());
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: general annotations
    ///////////////////////////////////////////////////////
    */


    @Override
    public JsonDeserializer<?> findDeserializer(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am);
        if (adapter != null) {
            return new XmlAdapterJsonDeserializer(adapter);
        }

        /* [JACKSON-150]: add support for additional core XML
         * types needed by JAXB
         */
        Class<?> type = am.getType();
        if (type != null) {
            if (DataHandler.class.isAssignableFrom(type)) {
                return new DataHandlerJsonDeserializer();
            }
        }

        return null;
    }

    @Override
    public Class<?> findDeserializationType(Annotated am)
    {
        return null;
    }

    @Override
    public Class<?> findDeserializationKeyType(Annotated am)
    {
        return null;
    }

    @Override
    public Class<?> findDeserializationContentType(Annotated am)
    {
        XmlElement annotation = findAnnotation(XmlElement.class, am, false, false, false);
        if (annotation != null && annotation.type() != XmlElement.DEFAULT.class) {
            return annotation.type();
        }
        return null;
    }

    @Override
    public Boolean findSetterAutoDetection(AnnotatedClass ac)
    {
        return isPropertiesAccessible(ac);
    }

    @Override
    public Boolean findCreatorAutoDetection(AnnotatedClass ac)
    {
        return null;
    }

    @Override
    public String findSettablePropertyName(AnnotatedMethod am)
    {
        String propertyName = findJaxbSpecifiedPropertyName(am);
        // null -> no annotation found
        return (propertyName == null) ? null : propertyName;
    }

    @Override
    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        //(ryan) JAXB has @XmlAnyAttribute and @XmlAnyElement annotations, but they're not applicable in this case
        // because JAXB says those annotations are only applicable to methods with specific signatures
        // that Jackson doesn't support. Yet.
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
        Field field = af.getAnnotated();
        return findJaxbPropertyName(field, field.getType(), field.getName());
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: parameters annotations
    ///////////////////////////////////////////////////////
    */

    @Override
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
            if (annotated.getAnnotated() instanceof Member) {
                memberClass = ((Member) annotated.getAnnotated()).getDeclaringClass();
                if (includeClass) {
                    annotation = (A) memberClass.getAnnotation(annotationClass);
                }
            } else if (annotated.getAnnotated() instanceof Class) {
                memberClass = ((Class) annotated.getAnnotated());
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
        XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
        XmlAccessorType at = findAnnotation(XmlAccessorType.class, ac, true, true, true);
        if (at != null) {
            accessType = at.value();
        }
        return accessType == XmlAccessType.PUBLIC_MEMBER || accessType == XmlAccessType.PROPERTY;
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
            return findJaxbPropertyName(new AnnotatedProperty(pd), pd.getPropertyType(), pd.getName());
        }

        return null;
    }

    /**
     * Find the JAXB property name for the given annotated element.
     *
     * @param ae The annotated element.
     * @param aeType The type of the annotated element.
     * @param defaultName The default name if nothing is specified.
     * @return The JAXB property name.
     */
    protected String findJaxbPropertyName(AnnotatedElement ae, Class<?> aeType, String defaultName)
    {
        XmlElementWrapper elementWrapper = ae.getAnnotation(XmlElementWrapper.class);
        if (elementWrapper != null) {
            String name = elementWrapper.name();
            if (!"##default".equals(name)) {
                return name;
            }
            return defaultName;
        }

        XmlAttribute attribute = ae.getAnnotation(XmlAttribute.class);
        if (attribute != null) {
            String name = attribute.name();
            if (!"##default".equals(name)) {
                return name;
            }
            return defaultName;
        }

        XmlElement element = ae.getAnnotation(XmlElement.class);
        if (element != null) {
            String name = element.name();
            if (!"##default".equals(name)) {
                return name;
            }
            return defaultName;
        }

        XmlElementRef elementRef = ae.getAnnotation(XmlElementRef.class);
        if (elementRef != null) {
            String name = elementRef.name();
            if (!"##default".equals(name)) {
                return name;
            }
            XmlRootElement rootElement = (XmlRootElement) aeType.getAnnotation(XmlRootElement.class);
            if (rootElement != null) {
                name = rootElement.name();
                if (!"##default".equals(name)) {
                    return name;
                }
                return Introspector.decapitalize(aeType.getSimpleName());
            }
        }

        XmlValue valueInfo = ae.getAnnotation(XmlValue.class);
        if (valueInfo != null) {
            return "value";
        }

        return defaultName;
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

        //@Override
        public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass)
        {
            return (pd.getReadMethod() != null && pd.getReadMethod().isAnnotationPresent(annotationClass))
                    || (pd.getWriteMethod() != null && pd.getWriteMethod().isAnnotationPresent(annotationClass));
        }

        //@Override
        public <T extends Annotation> T getAnnotation(Class<T> annotationClass)
        {
            T ann = pd.getReadMethod() != null ? pd.getReadMethod().getAnnotation(annotationClass) : null;
            if (ann == null) {
                ann = pd.getWriteMethod() != null ? pd.getWriteMethod().getAnnotation(annotationClass) : null;
            }
            return ann;
        }

        //@Override
        public Annotation[] getAnnotations()
        {
            //not used. we don't need to support this yet.
            throw new UnsupportedOperationException();
        }

        //@Override
        public Annotation[] getDeclaredAnnotations()
        {
            //not used. we don't need to support this yet.
            throw new UnsupportedOperationException();
        }
    }

}
