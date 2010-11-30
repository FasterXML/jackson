package org.codehaus.jackson.xml;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.StdDeserializerProvider;
import org.codehaus.jackson.util.VersionUtil;

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
    /*
    /**********************************************************
    /* Life-cycle: construction, configuration
    /**********************************************************
     */

    public XmlMapper()
    {
        // Need to override serializer provider, so:
        super(new XmlFactory(),
                new XmlSerializerProvider(new RootNameLookup()),
                new StdDeserializerProvider());
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
