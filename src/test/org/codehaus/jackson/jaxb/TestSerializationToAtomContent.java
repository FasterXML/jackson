package org.codehaus.jackson.jaxb;

import junit.framework.TestCase;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.MappingJsonFactory;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

import java.io.StringWriter;

/**
 * @author Ryan Heaton
 */
public class TestSerializationToAtomContent extends TestCase
{
    public void testJacksonSerialization()
            throws Exception
    {
        Content content = new Content();
        content.setRawType("application/json");
        StringWriter sw = new StringWriter();   // serialize
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector introspector = new JaxbAnnotationIntrospector();
        mapper.getDeserializationConfig().withAnnotationIntrospector(introspector);
        mapper.getSerializationConfig().withAnnotationIntrospector(introspector);
        MappingJsonFactory jsonFactory = new MappingJsonFactory();
        JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(sw);
        mapper.writeValue(jsonGenerator, content);
        sw.close();
        //System.out.println(sw.getBuffer().toString());
//        Content content2 = mapper.readValue(sw.getBuffer().toString(), Content.class); // deserialize

    }
}
