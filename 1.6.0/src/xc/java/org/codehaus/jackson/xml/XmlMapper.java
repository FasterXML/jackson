package org.codehaus.jackson.xml;

import org.codehaus.jackson.map.*;

/**
 * Customized {@link ObjectMapper} that will read and write XML instead of JSON.
 * 
 * @since 1.6
 */
public class XmlMapper extends ObjectMapper
{
    public XmlMapper() {
        super(new XmlFactory());
    }
}
