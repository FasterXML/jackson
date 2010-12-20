package org.codehaus.jackson.xml;

import javax.xml.namespace.QName;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.AnnotatedParameter;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdTypeResolverBuilder;

import org.codehaus.jackson.type.JavaType;
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

    //@Override
    public String findNamespace(Annotated ann)
    {
        JacksonXmlProperty prop = ann.getAnnotation(JacksonXmlProperty.class);
        if (prop != null) {
            return prop.namespace();
        }
        return null;
    }

    //@Override
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

    /*
    /**********************************************************
    /* Overrides for non-public helper methods
    /**********************************************************
     */

    /**
     * We will override this method so that we can return instance
     * that cleans up type id property name to be a valid xml name.
     */
    @Override
    protected StdTypeResolverBuilder _constructStdTypeResolverBuilder()
    {
        return new XmlTypeResolverBuilder();
    }
    
    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    /**
     * Since XML names can not contain all characters JSON names can, we may
     * need to replace characters. Let's start with trivial replacement of
     * ASCII characters that can not be included.
     */
    protected static String sanitizeXmlName(String name)
    {
        StringBuilder sb = new StringBuilder(name);
        int changes = 0;
        for (int i = 0, len = name.length(); i < len; ++i) {
            char c = name.charAt(i);
            if (c > 127) continue;
            if (c >= 'a' && c <= 'z') continue;
            if (c >= 'A' && c <= 'Z') continue;
            if (c >= '0' && c <= '9') continue;
            if (c == '_' || c == '.' || c == '-') continue;
            // Ok, need to replace
            ++changes;
            sb.setCharAt(i, '_');
        }
        if (changes == 0) {
            return name;
        }
        return sb.toString();
    }

    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    /**
     * Custom specialization of {@link StdTypeResolverBuilder}; needed so that
     * type id property name can be modified as necessary to make it legal
     * xml element or attribute name.
     */
    protected static class XmlTypeResolverBuilder extends StdTypeResolverBuilder
    {
        @Override
        public StdTypeResolverBuilder init(JsonTypeInfo.Id idType, TypeIdResolver idRes)
        {
            super.init(idType, idRes);
            if (_typeProperty != null) {
                _typeProperty = sanitizeXmlName(_typeProperty);
            }
            return this;
        }

        @Override
        public StdTypeResolverBuilder typeProperty(String typeIdPropName)
        {
            // ok to have null/empty; will restore to use defaults
            if (typeIdPropName == null || typeIdPropName.length() == 0) {
                typeIdPropName = _idType.getDefaultPropertyName();
            }
            _typeProperty = sanitizeXmlName(typeIdPropName);
            return this;
        }
    }
}

