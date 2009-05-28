package org.codehaus.jackson.xc;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.annotate.OutputProperties;
import org.codehaus.jackson.xc.XmlAdapterJsonDeserializer;
import org.codehaus.jackson.xc.XmlAdapterJsonSerializer;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;
import java.lang.annotation.Annotation;
import java.lang.reflect.Member;
import java.lang.reflect.Modifier;
import java.beans.PropertyDescriptor;
import java.beans.Introspector;
import java.beans.BeanInfo;
import java.beans.IntrospectionException;

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

    /**
     * An annotation is handled if it's in the same package as @XmlElement, including subpackages.
     *
     * @param ann The annotation.
     * @return Whether the annotation is in the JAXB package.
     */
    @Override
    public boolean isHandled(Annotation ann)
    {
        return ann != null && ann.getClass().getPackage() != null && ann.getClass().getPackage().getName().startsWith(XmlElement.class.getPackage().getName());
    }

    @Override
    public JsonSerializer<?> getSerializerInstance(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am);
        if (adapter != null) {
            return new XmlAdapterJsonSerializer(adapter);
        }

        return null;
    }

    @Override
    public JsonDeserializer<?> getDeserializerInstance(Annotated am)
    {
        XmlAdapter<Object,Object> adapter = findAdapter(am);
        if (adapter != null) {
            return new XmlAdapterJsonDeserializer(adapter);
        }

        return null;
    }

    @Override
    public Boolean findGetterAutoDetection(AnnotatedClass ac)
    {
        return isPropertiesAccessible(ac);
    }

    @Override
    public Boolean findCachability(AnnotatedClass ac)
    {
        return null;
    }

    @Override
    public Boolean findFieldAutoDetection(AnnotatedClass ac)
    {
        // !!! 28-May-2009, tatu: Not yet implemented properly
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
     * @return OutputProperties.NON_NULL
     */
    @Override
    public OutputProperties findSerializationInclusion(Annotated a, OutputProperties defValue)
    {
        if ((a instanceof AnnotatedField) || (a instanceof AnnotatedMethod)) {
            boolean nillable = a.getAnnotation(XmlElementWrapper.class) != null ? a.getAnnotation(XmlElementWrapper.class).nillable() :
                    a.getAnnotation(XmlElement.class) != null && a.getAnnotation(XmlElement.class).nillable();
            return nillable ? OutputProperties.ALL : OutputProperties.NON_NULL;
        }
        return OutputProperties.NON_NULL;
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
    public boolean isIgnorableMethod(AnnotatedMethod m)
    {
        if (m.getAnnotation(XmlTransient.class) != null) {
            return true;
        }
        else if (m.getAnnotationCount() > 0) {
            //if any annotations are present, it is NOT ignorable.
            return false;
        }
        else if (isPropertiesAccessible(m)) {
            //jaxb only accounts for getter/setter pairs.
            PropertyDescriptor pd = findPropertyDescriptor(m);
            return pd == null || pd.getReadMethod() == null || pd.getWriteMethod() == null; 
        }

        return true;
    }

    @Override
    public String findGettablePropertyName(AnnotatedMethod am)
    {
        String propertyName = findJaxbSpecifiedPropertyName(am);
        return propertyName == null ? am.getName() : propertyName;
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        //since jaxb says @XmlValue can exist with attributes, this won't map as a json value.
        return false;
    }

    @Override
    public String findSettablePropertyName(AnnotatedMethod am)
    {
        String propertyName = findJaxbSpecifiedPropertyName(am);
        return propertyName == null ? am.getName() : propertyName;
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
    public boolean hasCreatorAnnotation(AnnotatedMethod am)
    {
        return false;
    }

    @Override
    public boolean isIgnorableField(AnnotatedField f)
    {
        if (f.getAnnotation(XmlTransient.class) != null) {
            return true;
        }
        else if (f.getAnnotationCount() > 0) {
            //if any annotations are present, it is NOT ignorable.
            return false;
        }
        else {
            XmlAccessType accessType = XmlAccessType.PUBLIC_MEMBER;
            XmlAccessorType at = findAnnotation(XmlAccessorType.class, f, true, true, true);
            if (at != null) {
                accessType = at.value();
            }

            return accessType != XmlAccessType.FIELD &&
                    !(accessType == XmlAccessType.PUBLIC_MEMBER && Modifier.isPublic(f.getAnnotated().getModifiers()));
        }
    }

    @Override
    public String findPropertyName(AnnotatedField af)
    {
        // !!! 28-May-2009, tatu: Not yet implemented properly
        return null;
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
                annotation = getAnnotation(annotationClass, pd);
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
     * Finds the property descriptor for the specified method.
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
            XmlElementWrapper elementWrapper = getAnnotation(XmlElementWrapper.class, pd);
            if (elementWrapper != null) {
                if (!"##default".equals(elementWrapper.name())) {
                    return elementWrapper.name();
                }
                else {
                    return pd.getName();
                }
            }

            XmlAttribute attribute = getAnnotation(XmlAttribute.class, pd);
            if (attribute != null) {
                if (!"##default".equals(attribute.name())) {
                    return attribute.name();
                }
                else {
                    return pd.getName();
                }
            }

            XmlElement element = getAnnotation(XmlElement.class, pd);
            if (element != null) {
                if (!"##default".equals(element.name())) {
                    return element.name();
                }
                else {
                    return pd.getName();
                }
            }

            XmlElementRef elementRef = getAnnotation(XmlElementRef.class, pd);
            if (elementRef != null) {
                if (!"##default".equals(elementRef.name())) {
                    return elementRef.name();
                }
                else {
                    XmlRootElement rootElement = pd.getPropertyType().getAnnotation(XmlRootElement.class);
                    if (rootElement != null) {
                        if (!"##default".equals(rootElement.name())) {
                            return rootElement.name();
                        }
                        else {
                            return Introspector.decapitalize(pd.getPropertyType().getSimpleName());
                        }
                    }
                }
            }

            XmlValue valueInfo = getAnnotation(XmlValue.class, pd);
            if (valueInfo != null) {
                return "value";
            }

            return pd.getName();
        }

        return null;
    }

    /**
     * Finds an annotation on a property.
     * @param annotationClass The annotation class.
     * @param pd The property descriptor.
     * @return The annotation, or null if not found.
     */
    protected <A extends Annotation> A getAnnotation(Class<A> annotationClass, PropertyDescriptor pd) {
        A annotation = null;
        if (pd.getReadMethod() != null) {
            annotation = pd.getReadMethod().getAnnotation(annotationClass);
        }

        if (annotation == null && pd.getWriteMethod() != null) {
            annotation = pd.getWriteMethod().getAnnotation(annotationClass);
        }

        return annotation;
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

}
