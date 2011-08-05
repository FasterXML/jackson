package org.codehaus.jackson.map.deser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.deser.std.StdDeserializer;

/**
 * Test to check that customization using {@link CustomDeserializerFactory}
 * works as expected.
 */
public class TestCustomFactory
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class DummyDeserializer<T>
        extends StdDeserializer<T>
    {
        final T value;

        public DummyDeserializer(T v, Class<T> cls) {
            super(cls);
            value = v;
        }

        @Override
        public T deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            // need to skip, if structured...
            jp.skipChildren();
            return value;
        }
    }

    static class TestBeans {
        public List<TestBean> beans;
    }
    static class TestBean {
        public CustomBean c;
        public String d;
    }
    @JsonDeserialize(using=CustomBeanDeserializer.class)
    static class CustomBean {
        protected final int a, b;
        public CustomBean(int a, int b) {
            this.a = a;
            this.b = b;
        }
    }

    static class CustomBeanDeserializer extends JsonDeserializer<CustomBean>
    {
        @Override
        public CustomBean deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException
        {
            int a = 0, b = 0;
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.START_OBJECT) {
                t = jp.nextToken();
            } else if (t != JsonToken.FIELD_NAME) {
                throw new Error();
            }
            while(t == JsonToken.FIELD_NAME) {
                final String fieldName = jp.getCurrentName();
                t = jp.nextToken();
                if (t != JsonToken.VALUE_NUMBER_INT) {
                    throw new JsonParseException("expecting number got "+ t, jp.getCurrentLocation());
                }
                if (fieldName.equals("a")) {
                    a = jp.getIntValue();
                } else if (fieldName.equals("b")) {
                    b = jp.getIntValue();
                } else {
                    throw new Error();
                }
                t = jp.nextToken();
            }
            return new CustomBean(a, b);
        }
    }

    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */
    
    @SuppressWarnings("deprecation")
    public void testDateOverride() throws Exception
    {
        Date expResult = new Date(3L);
        ObjectMapper mapper = new ObjectMapper();
        CustomDeserializerFactory sf = new CustomDeserializerFactory();
        sf.addSpecificMapping(Date.class, new DummyDeserializer<Date>(expResult, Date.class));
        mapper.setDeserializerProvider(new StdDeserializerProvider(sf));

        Date result = mapper.readValue("123", Date.class);
        assertEquals(expResult.getTime(), result.getTime());
    }

    public void testCustomBeanDeserializer() throws Exception
    {

        final ObjectMapper map = new ObjectMapper();
        String json = "{\"beans\":[{\"c\":{\"a\":10,\"b\":20},\"d\":\"hello, tatu\"}]}";
        TestBeans beans = map.readValue(json, TestBeans.class);

        assertNotNull(beans);
        List<TestBean> results = beans.beans;
        assertNotNull(results);
        assertEquals(1, results.size());
        TestBean bean = results.get(0);
        assertEquals("hello, tatu", bean.d);
        CustomBean c = bean.c;
        assertNotNull(c);
        assertEquals(10, c.a);
        assertEquals(20, c.b);

        json = "{\"beans\":[{\"c\":{\"b\":3,\"a\":-4},\"d\":\"\"},"
            +"{\"d\":\"abc\", \"c\":{\"b\":15}}]}";
        beans = map.readValue(json, TestBeans.class);

        assertNotNull(beans);
        results = beans.beans;
        assertNotNull(results);
        assertEquals(2, results.size());

        bean = results.get(0);
        assertEquals("", bean.d);
        c = bean.c;
        assertNotNull(c);
        assertEquals(-4, c.a);
        assertEquals(3, c.b);

        bean = results.get(1);
        assertEquals("abc", bean.d);
        c = bean.c;
        assertNotNull(c);
        assertEquals(0, c.a);
        assertEquals(15, c.b);
    }
}
