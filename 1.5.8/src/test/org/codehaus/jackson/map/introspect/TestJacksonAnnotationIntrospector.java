package org.codehaus.jackson.map.introspect;

import junit.framework.TestCase;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonTypeResolver;
import org.codehaus.jackson.map.deser.StdDeserializer;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdTypeResolverBuilder;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * @author Ryan Heaton
 */
public class TestJacksonAnnotationIntrospector
        extends TestCase
{
    public static enum EnumExample {
        VALUE1;
    }

    public static class JacksonExample
    {
        private String attributeProperty;
        private String elementProperty;
        private List<String> wrappedElementProperty;
        private EnumExample enumProperty;
        private QName qname;

        @JsonSerialize(using=QNameSerializer.class)
        public QName getQname()
        {
            return qname;
        }

        @JsonDeserialize(using=QNameDeserializer.class)
        public void setQname(QName qname)
        {
            this.qname = qname;
        }

        @JsonProperty("myattribute")
        public String getAttributeProperty()
        {
            return attributeProperty;
        }

        @JsonProperty("myattribute")
        public void setAttributeProperty(String attributeProperty)
        {
            this.attributeProperty = attributeProperty;
        }

        @JsonProperty("myelement")
        public String getElementProperty()
        {
            return elementProperty;
        }

        @JsonProperty("myelement")
        public void setElementProperty(String elementProperty)
        {
            this.elementProperty = elementProperty;
        }

        @JsonProperty("mywrapped")
        public List<String> getWrappedElementProperty()
        {
            return wrappedElementProperty;
        }

        @JsonProperty("mywrapped")
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


    public static class QNameDeserializer extends StdDeserializer<QName>
    {
        public QNameDeserializer() { super(QName.class); }
        @Override
        public QName deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException, JsonProcessingException
        {
            return QName.valueOf(jp.readValueAs(String.class));
        }
    }

    public static class DummyBuilder extends StdTypeResolverBuilder
    //<DummyBuilder>
    {
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS)
    @JsonTypeResolver(DummyBuilder.class)
    static class TypeResolverBean { }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
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
        JacksonExample readEx = mapper.readValue(json, JacksonExample.class);

        assertEquals(ex.qname, readEx.qname);
        assertEquals(ex.attributeProperty, readEx.attributeProperty);
        assertEquals(ex.elementProperty, readEx.elementProperty);
        assertEquals(ex.wrappedElementProperty, readEx.wrappedElementProperty);
        assertEquals(ex.enumProperty, readEx.enumProperty);
    }

    public void testJsonTypeResolver() throws Exception
    {
        JacksonAnnotationIntrospector ai = new JacksonAnnotationIntrospector();
        AnnotatedClass ac = AnnotatedClass.constructWithoutSuperTypes(TypeResolverBean.class, ai, null);
        JavaType baseType = TypeFactory.type(TypeResolverBean.class);
        TypeResolverBuilder<?> rb = ai.findTypeResolver(ac, baseType);
        assertNotNull(rb);
        assertSame(DummyBuilder.class, rb.getClass());
    }    
}