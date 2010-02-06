package org.codehaus.jackson.map.ser;
import java.io.IOException;
import java.io.StringWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;

/**
 * Test for verifying [JACKSON-238]
 *
 * @author Pablo Lalloni <plalloni@gmail.com>
 * @since 04/02/2010 19:50:00
 */
public class TestCustomSerializers
    extends org.codehaus.jackson.map.BaseMapTest
{
    static class ElementSerializer extends JsonSerializer<Element>
    {
        @Override
        public void serialize(Element value, JsonGenerator jgen, SerializerProvider provider) throws IOException, JsonProcessingException {
            jgen.writeString("element");
        }
    }
    
    @JsonSerialize(using = ElementSerializer.class)
    public static class ElementMixin {}

    public void testCustomization() throws Exception
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.getSerializationConfig().addMixInAnnotations(Element.class, ElementMixin.class);
        Element element = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument().createElement("el");
        StringWriter sw = new StringWriter();
        objectMapper.writeValue(sw, element);
        assertEquals(sw.toString(), "\"element\"");
    }
}
