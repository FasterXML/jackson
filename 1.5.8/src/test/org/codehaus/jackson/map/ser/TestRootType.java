package org.codehaus.jackson.map.ser;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

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
    /***********************************************
    /* Annotated helper classes
    /***********************************************
     */

    interface BaseInterface {
        int getB();
    }
    
    static class BaseType
        implements BaseInterface
    {
        public String a = "a";

        public int getB() { return 3; }
    }

    static class SubType extends BaseType {
        public String a2 = "x";
        
        public boolean getB2() { return true; }
    }
    
    /*
    /***********************************************
    /* Main tests
    /***********************************************
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
        ObjectWriter w = mapper.typedWriter(BaseType.class);
        String json = w.writeValueAsString(bean);
        result = (Map)mapper.readValue(json, Map.class);
        assertEquals(2, result.size());
        assertEquals("a", result.get("a"));
        assertEquals(Integer.valueOf(3), result.get("b"));
    }

    public void testSuperInterface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SubType bean = new SubType();

        // let's constrain by interface:
        ObjectWriter w = mapper.typedWriter(BaseInterface.class);
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
        String json = mapper.typedWriter(BaseInterface[].class).writeValueAsString(ob);
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
        ObjectWriter w = mapper.typedWriter(HashMap.class);
        try {
            w.writeValueAsString(bean);
            fail("Should have failed due to incompatible type");
        } catch (JsonProcessingException e) {
            verifyException(e, "Incompatible types");
        }
    }
}
