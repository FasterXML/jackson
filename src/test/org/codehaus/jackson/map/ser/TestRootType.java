package org.codehaus.jackson.map.ser;

import java.io.StringWriter;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonRootName;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Unit tests for verifying functioning of [JACKSON-195], ability to
 * force specific root type for serialization (super type of value)
 * 
 * @author tatu
 * @since 1.5
 */
public class TestRootType
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    interface BaseInterface {
        int getB();
    }
    
    static class BaseType
        implements BaseInterface
    {
        public String a = "a";

        @Override
        public int getB() { return 3; }
    }

    static class SubType extends BaseType {
        public String a2 = "x";
        
        public boolean getB2() { return true; }
    }

    @JsonTypeInfo(use=Id.NAME, include=As.PROPERTY, property="beanClass")
    public abstract static class BaseClass398 { }

    public static class TestClass398 extends BaseClass398 {
       public String property = "aa";
    }
    
    @JsonRootName("root")
    static class WithRootName {
        public int a = 3;
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */
    
    @SuppressWarnings("unchecked")
    public void testSuperClass() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SubType bean = new SubType();

        // first, test with dynamically detected type
        Map<String,Object> result = writeAndMap(mapper, bean);
        assertEquals(4, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
        assertEquals("x", result.get("a2"));
        assertEquals(Boolean.TRUE, result.get("b2"));

        // and then using specified typed writer
        ObjectWriter w = mapper.writerWithType(BaseType.class);
        String json = w.writeValueAsString(bean);
        result = (Map<String,Object>)mapper.readValue(json, Map.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    public void testSuperInterface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SubType bean = new SubType();

        // let's constrain by interface:
        ObjectWriter w = mapper.writerWithType(BaseInterface.class);
        String json = w.writeValueAsString(bean);
        @SuppressWarnings("unchecked")
        Map<String,Object> result = mapper.readValue(json, Map.class);
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    public void testInArray() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // must force static typing, otherwise won't matter a lot
        mapper.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        SubType[] ob = new SubType[] { new SubType() };
        String json = mapper.writerWithType(BaseInterface[].class).writeValueAsString(ob);
        // should propagate interface type through due to root declaration; static typing
        assertEquals("[{\"b\":3}]", json);
    }
    
    /**
     * Unit test to ensure that proper exception is thrown if declared
     * root type is not compatible with given value instance.
     */
    public void testIncompatibleRootType() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SubType bean = new SubType();

        // and then let's try using incompatible type
        ObjectWriter w = mapper.writerWithType(HashMap.class);
        try {
            w.writeValueAsString(bean);
            fail("Should have failed due to incompatible type");
        } catch (JsonProcessingException e) {
            verifyException(e, "Incompatible types");
        }
    }
    
    /**
     * Unit test to verify [JACKSON-398]
     */
    public void testJackson398() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JavaType collectionType = TypeFactory.defaultInstance().constructCollectionType(ArrayList.class, BaseClass398.class);
        List<TestClass398> typedList = new ArrayList<TestClass398>();
        typedList.add(new TestClass398());

        final String EXP = "[{\"beanClass\":\"TestRootType$TestClass398\",\"property\":\"aa\"}]";
        
        // First simplest way:
        String json = mapper.writerWithType(collectionType).writeValueAsString(typedList);
        assertEquals(EXP, json);

        StringWriter out = new StringWriter();
        JsonFactory f = new JsonFactory();
        mapper.writerWithType(collectionType).writeValue(f.createJsonGenerator(out), typedList);

        assertEquals(EXP, out.toString());
    }

    // Test to verify [JACKSON-163]
    public void testRootWrapping() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
        String json = mapper.writeValueAsString(new StringWrapper("abc"));
        assertEquals("{\"StringWrapper\":{\"str\":\"abc\"}}", json);
    }

    /**
     * Test to verify that there is support for specifying root type as primitive,
     * even if wrapper value is passed (there is no way to pass primitive values as
     * Objects); this to support frameworks that may pass unprocessed
     * {@link java.lang.reflect.Type} from field or method.
     */
    public void testIssue456WrapperPart() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        assertEquals("123", mapper.writerWithType(Integer.TYPE).writeValueAsString(Integer.valueOf(123)));
        assertEquals("456", mapper.writerWithType(Long.TYPE).writeValueAsString(Long.valueOf(456L)));
    }

    // [JACKSON-630] also, allow annotation to define root name
    public void testRootNameAnnotation() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
        String json = mapper.writeValueAsString(new WithRootName());
        assertEquals("{\"root\":{\"a\":3}}", json);
    }
}
