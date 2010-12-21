package org.codehaus.jackson.xml;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.util.VersionUtil;
import org.codehaus.jackson.xml.util.XmlRootNameLookup;

/**
 * Customized {@link ObjectMapper} that will read and write XML instead of JSON,
 * using XML-backed {@link JsonFactory} implementation ({@link XmlFactory}).
 *<p>
 * Mapper itself overrides some aspects of functionality to try to handle
 * data binding aspects as similar to JAXB as possible.
 * 
 * @since 1.7
 */
public class XmlMapper extends ObjectMapper
{
    private final static AnnotationIntrospector XML_ANNOTATION_INTROSPECTOR = new JacksonXmlAnnotationIntrospector();
    
    /*
    /**********************************************************
    /* Life-cycle: construction, configuration
    /**********************************************************
     */

    public XmlMapper()
    {
        this(new XmlFactory());
    }
    
    public XmlMapper(XmlFactory xmlFactory)
    {
        /* Need to override serializer provider (due to root name handling);
         * deserializer provider fine as is
         */
        super(xmlFactory, new XmlSerializerProvider(new XmlRootNameLookup()), null);
        
        // Bean serializers are somewhat customized as well:
        _serializerFactory = new XmlBeanSerializerFactory(null);
        // as is introspector
        _deserializationConfig.setAnnotationIntrospector(XML_ANNOTATION_INTROSPECTOR);
        _serializationConfig.setAnnotationIntrospector(XML_ANNOTATION_INTROSPECTOR);
    }

    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     */
    @Override
    public Version version() {
        return VersionUtil.versionFor(getClass());
    }

    /*
    /**********************************************************
    /* Access to configuration settings
    /**********************************************************
     */
    
}
