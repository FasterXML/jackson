
package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class TestCreators2
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class HashTest
    {
        final byte[] bytes;
        final String type;

        @JsonCreator
        public HashTest(@JsonProperty("bytes") @JsonDeserialize(using = BytesDeserializer.class) final byte[] bytes,
                @JsonProperty("type") final String type)
        {
            this.bytes = bytes;
            this.type = type;
        }
    }

    static class BytesDeserializer extends JsonDeserializer<byte[]>
    {
        @Override
        public byte[] deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            String str = jp.getText();
            return str.getBytes("UTF-8");
        }
    }

    static class Primitives
    {
        protected int x = 3;
        protected double d = -0.5;
        protected boolean b = true;
        
        @JsonCreator
        public Primitives(@JsonProperty("x") int x,
                @JsonProperty("d") double d,
                @JsonProperty("b") boolean b)
        {
            this.x = x;
            this.d = d;
            this.b = b;
        }
    }
    
    protected static class Test431Container {
        protected final List<Item431> items;

        @JsonCreator
        public Test431Container(@JsonProperty("items") final List<Item431> i) {
            items = i;
        }
}    

    @JsonIgnoreProperties(ignoreUnknown = true)
    protected static class Item431 {
        protected final String id;

        @JsonCreator
        public Item431(@JsonProperty("id") String id) {
            this.id = id;
        }
    }

    // Test class for verifying that creator-call failures are reported as checked exceptions
    static class BeanFor438 {
        @JsonCreator
        public BeanFor438(@JsonProperty("name") String s) {
            throw new IllegalArgumentException("I don't like that name!");
        }
    }

    // For [JACKSON-465]
    static class MapBean
    {
        protected Map<String,Long> map;
        
        @JsonCreator
        public MapBean(Map<String, Long> map) {
            this.map = map;
        }
    }

    // For [JACKSON-470]: should be appropriately detected, reported error about
    static class BrokenCreatorBean
    {
        protected String bar;
        
        @JsonCreator
        public BrokenCreatorBean(@JsonProperty("bar") String bar1, @JsonProperty("bar") String bar2) {
            bar = ""+bar1+"/"+bar2;
        }
    }
    
    // For [JACKSON-541]: should not need @JsonCreator if Feature.AUTO_DETECT_CREATORS is on.
    static class AutoDetectConstructorBean
    {
    	protected final String foo;
    	protected final String bar;

    	public AutoDetectConstructorBean(@JsonProperty("bar") String bar, @JsonProperty("foo") String foo){
    	    this.bar = bar;
    	    this.foo = foo;
    	}
    }

    static class BustedCtor {
        @JsonCreator
        BustedCtor(@JsonProperty("a") String value) {
            throw new IllegalArgumentException("foobar");
        }
    }

    // As per [JACKSON-575]
    static class IgnoredCtor
    {
        @JsonIgnore
        public IgnoredCtor(String arg) {
            throw new RuntimeException("Should never use this constructor");
        }

        public IgnoredCtor() { }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // for [JACKSON-547]
    public void testExceptionFromConstructor() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        try {
            m.readValue("{}", BustedCtor.class);
            fail("Expected exception");
        } catch (JsonMappingException e) {
            verifyException(e, ": foobar");
            // also: should have nested exception
            Throwable t = e.getCause();
            assertNotNull(t);
            assertEquals(IllegalArgumentException.class, t.getClass());
            assertEquals("foobar", t.getMessage());
        }
    }
    
    public void testSimpleConstructor() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        HashTest test = m.readValue("{\"type\":\"custom\",\"bytes\":\"abc\" }", HashTest.class);
        assertEquals("custom", test.type);
        assertEquals("abc", new String(test.bytes, "UTF-8"));
    }    

    // Test for [JACKSON-372]
    public void testMissingPrimitives() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Primitives p = m.readValue("{}", Primitives.class);
        assertFalse(p.b);
        assertEquals(0, p.x);
        assertEquals(0.0, p.d);
    }

    public void testJackson431() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        final Test431Container foo = m.readValue(
                "{\"items\":\n"
                +"[{\"bar\": 0,\n"
                +"\"id\": \"id123\",\n"
                +"\"foo\": 1\n" 
                +"}]}",
                Test431Container.class);
        assertNotNull(foo);
    }

    // [JACKSON-438]: Catch and rethrow exceptions that Creator methods throw
    public void testJackson438() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        try {
            m.readValue("{ \"name\":\"foobar\" }", BeanFor438.class);
            fail("Should have failed");
        } catch (Exception e) {
            if (!(e instanceof JsonMappingException)) {
                fail("Should have received JsonMappingException, caught "+e.getClass().getName());
            }
            verifyException(e, "don't like that name");
            // Ok: also, let's ensure root cause is directly linked, without other extra wrapping:
            Throwable t = e.getCause();
            assertNotNull(t);
            assertEquals(IllegalArgumentException.class, t.getClass());
            verifyException(e, "don't like that name");
        }
    }

    @SuppressWarnings("unchecked")
    public void testIssue465() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        final String JSON = "{\"A\":12}";

        // first, test with regular Map, non empty
        Map<String,Long> map = mapper.readValue(JSON, Map.class);
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(12), map.get("A"));
        
        MapBean bean = mapper.readValue(JSON, MapBean.class);
        assertEquals(1, bean.map.size());
        assertEquals(Long.valueOf(12L), bean.map.get("A"));

        // and then empty ones
        final String EMPTY_JSON = "{}";

        map = mapper.readValue(EMPTY_JSON, Map.class);
        assertEquals(0, map.size());
        
        bean = mapper.readValue(EMPTY_JSON, MapBean.class);
        assertEquals(0, bean.map.size());
    }

    public void testCreatorWithDupNames() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue("{\"bar\":\"x\"}", BrokenCreatorBean.class);
            fail("Should have caught duplicate creator parameters");
        } catch (JsonMappingException e) {
            verifyException(e, "duplicate creator property \"bar\"");
        }
    }
    
    public void testCreatorMultipleArgumentWithoutAnnotation() throws Exception {
    	ObjectMapper mapper = new ObjectMapper();
    	AutoDetectConstructorBean value = mapper.readValue("{\"bar\":\"bar\",\"foo\":\"foo\"}", AutoDetectConstructorBean.class);
    	assertEquals("bar", value.bar);
    	assertEquals("foo", value.foo);
    }

    // for [JACKSON-575]
    public void testIgnoredSingleArgCtor() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        try {
            mapper.readValue(quote("abc"), IgnoredCtor.class);
            fail("Should have caught missing constructor problem");
        } catch (JsonMappingException e) {
            verifyException(e, "no single-String constructor/factory method");
        }
    }
}
