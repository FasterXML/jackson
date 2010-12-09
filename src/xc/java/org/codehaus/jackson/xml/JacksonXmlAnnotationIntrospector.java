package org.codehaus.jackson.xml;

import javax.xml.namespace.QName;

import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.AnnotatedParameter;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;

import org.codehaus.jackson.xml.annotate.JacksonXmlElementWrapper;
import org.codehaus.jackson.xml.annotate.JacksonXmlProperty;

/**
 * Extension of {@link JacksonAnnotationIntrospector} that is needed to support
 * additional xml-specific annotation that Jackson provides. Note, however, that
 * there is no JAXB annotation support here; that is provided with
 * separate introspector (see {@link org.codehaus.jackson.xc.JaxbAnnotationIntrospector}).
 * 
 * @since 1.7
 */
public class JacksonXmlAnnotationIntrospector
    extends JacksonAnnotationIntrospector
    implements XmlAnnotationIntrospector
{    
    /*
    /**********************************************************
    /* XmlAnnotationIntrospector
    /**********************************************************
     */

    //@Override
    public Boolean isOutputAsAttribute(Annotated ann)
    {
        JacksonXmlProperty prop = ann.getAnnotation(JacksonXmlProperty.class);
        if (prop != null) {
            return prop.isAttribute() ? Boolean.TRUE : Boolean.FALSE;
        }
        return null;
    }

    @Override
    public String findNamespace(Annotated ann)
    {
        JacksonXmlProperty prop = ann.getAnnotation(JacksonXmlProperty.class);
        if (prop != null) {
            return prop.namespace();
        }
        return null;
    }

    @Override
    public QName findWrapperElement(Annotated ann)
    {
        JacksonXmlElementWrapper w = ann.getAnnotation(JacksonXmlElementWrapper.class);
        if (w != null) {
            return new QName(w.namespace(), w.localName());
        }
        return null;
    }
    
    /*
    /**********************************************************
    /* Overrides for name, property detection
    /**********************************************************
     */
    
    @Override
    public String findSerializablePropertyName(AnnotatedField af)
    {
        JacksonXmlProperty pann = af.getAnnotation(JacksonXmlProperty.class);
        if (pann != null) {
            return pann.localName();
        }
        return super.findSerializablePropertyName(af);
    }

    @Override
    public String findSettablePropertyName(AnnotatedMethod am)
    {
        JacksonXmlProperty pann = am.getAnnotation(JacksonXmlProperty.class);
        if (pann != null) {
            return pann.localName();
        }
        return super.findSettablePropertyName(am);
    }

    @Override
    public String findDeserializablePropertyName(AnnotatedField af)
    {
        JacksonXmlProperty pann = af.getAnnotation(JacksonXmlProperty.class);
        if (pann != null) {
            return pann.localName();
        }
        return super.findDeserializablePropertyName(af);
    }

    @Override
    public String findPropertyNameForParam(AnnotatedParameter ap)
    {
        JacksonXmlProperty pann = ap.getAnnotation(JacksonXmlProperty.class);
        // can not return empty String here, so:
        if (pann != null) {
            String name = pann.localName();
            if (name.length() > 0) {
                return name;
            }
        }
        return super.findPropertyNameForParam(ap);
    }
}
