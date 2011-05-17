package org.codehaus.jackson.failing;

import org.codehaus.jackson.jaxb.BaseJaxbTest;
import org.codehaus.jackson.jaxb.Content;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests for [JACKSON-539]
 * 
 * @author Ryan Heaton
 */
public class TestSerializationToAtomContent extends BaseJaxbTest
{
    public void testJacksonSerialization()
            throws Exception
    {
        Content content = new Content();
        content.setRawType("application/json");
        ObjectMapper mapper = getJaxbMapper();
        String json = mapper.writeValueAsString(content);
        Content content2 = mapper.readValue(json, Content.class); // deserialize
        assertNotNull(content2);
    }
}
