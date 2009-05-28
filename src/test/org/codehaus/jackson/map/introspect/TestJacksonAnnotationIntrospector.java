package org.codehaus.jackson.map.introspect;

import junit.framework.TestCase;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.annotate.JsonGetter;
import org.codehaus.jackson.annotate.JsonSetter;
import org.codehaus.jackson.annotate.JsonUseDeserializer;
import org.codehaus.jackson.annotate.JsonUseSerializer;
import org.codehaus.jackson.map.*;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

/**
 * @author Ryan Heaton
 */
public class TestJacksonAnnotationIntrospector
        extends TestCase
{

    /**
     * tests getting serializer/deserializer instances.
     */
    public void testSerializeDeserializeWithJaxbAnnotations() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getSerializationConfig().set(SerializationConfig.Feature.INDENT_OUTPUT, true);
        JacksonExample ex = new JacksonExample();
        QName qname = new QName("urn:hi", "hello");
        ex.setQname(qname);
        ex.setAttributeProperty("attributeValue");
        ex.setElementProperty("elementValue");
        ex.setWrappedElementProperty(Arrays.asList("wrappedElementValue"));
        ex.setEnumProperty(EnumExample.VALUE1);
        StringWriter writer = new StringWriter();
        mapper.writeValue(writer, ex);
        writer.flush();
        writer.close();

        String json = writer.toString();
//        System.out.println(json);
        JacksonExample readEx = mapper.readValue(json, JacksonExample.class);

        assertEquals(ex.qname, readEx.qname);
        assertEquals(ex.attributeProperty, readEx.attributeProperty);
        assertEquals(ex.elementProperty, readEx.elementProperty);
        assertEquals(ex.wrappedElementProperty, readEx.wrappedElementProperty);
        assertEquals(ex.enumProperty, readEx.enumProperty);
    }

    public static enum EnumExample {

        VALUE1
    }

    public static class JacksonExample
    {

        private String attributeProperty;
        private String elementProperty;
        private List<String> wrappedElementProperty;
        private EnumExample enumProperty;
        private QName qname;

        @JsonUseSerializer(QNameSerializer.class)
        public QName getQname()
        {
            return qname;
        }

        @JsonUseDeserializer(QNameDeserializer.class)
        public void setQname(QName qname)
        {
            this.qname = qname;
        }

        @JsonGetter("myattribute")
        public String getAttributeProperty()
        {
            return attributeProperty;
        }

        @JsonSetter("myattribute")
        public void setAttributeProperty(String attributeProperty)
        {
            this.attributeProperty = attributeProperty;
        }

        @JsonGetter("myelement")
        public String getElementProperty()
        {
            return elementProperty;
        }

        @JsonSetter("myelement")
        public void setElementProperty(String elementProperty)
        {
            this.elementProperty = elementProperty;
        }

        @JsonGetter("mywrapped")
        public List<String> getWrappedElementProperty()
        {
            return wrappedElementProperty;
        }

        @JsonSetter("mywrapped")
        public void setWrappedElementProperty(List<String> wrappedElementProperty)
        {
            this.wrappedElementProperty = wrappedElementProperty;
        }

        public EnumExample getEnumProperty()
        {
            return enumProperty;
        }

        public void setEnumProperty(EnumExample enumProperty)
        {
            this.enumProperty = enumProperty;
        }
    }

    public static class QNameSerializer extends JsonSerializer<QName> {

        @Override
        public void serialize(QName value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException
        {
            jgen.writeString(value.toString());
        }
    }


    public static class QNameDeserializer extends JsonDeserializer<QName>
    {

        @Override
        public QName deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return QName.valueOf(jp.readValueAs(String.class));
        }
    }

}