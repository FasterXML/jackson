package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * Tests related to [JACKSON-139]
 */
public class TestNumbers
    extends BaseMapTest
{
    /*
    /**********************************************************************
    /* Helper classes, beans
    /**********************************************************************
     */

    static class MyBeanHolder {
        public Long id;
        public MyBeanDefaultValue defaultValue;
    }

    static class MyBeanDefaultValue
    {
        public MyBeanValue value;
    }

    @JsonDeserialize(using=MyBeanDeserializer.class)
    static class MyBeanValue {
        public BigDecimal decimal;
        public MyBeanValue() { this(null); }
        public MyBeanValue(BigDecimal d) { this.decimal = d; }
    }

    /*
    /**********************************************************************
    /* Helper classes, serializers/deserializers/resolvers
    /**********************************************************************
     */
    
    static class MyBeanDeserializer extends JsonDeserializer<MyBeanValue>
    {
        @Override
        public MyBeanValue deserialize(JsonParser jp, DeserializationContext ctxt)
                throws IOException
        {
            return new MyBeanValue(jp.getDecimalValue());
        }
    }

    /*
    /**********************************************************************
    /* Unit tests
    /**********************************************************************
     */
    
    public void testFloatNaN() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Float result = m.readValue(" \"NaN\"", Float.class);
        assertEquals(Float.valueOf(Float.NaN), result);
    }

    public void testDoubleInf() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Double result = m.readValue(" \""+Double.POSITIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.POSITIVE_INFINITY), result);

        result = m.readValue(" \""+Double.NEGATIVE_INFINITY+"\"", Double.class);
        assertEquals(Double.valueOf(Double.NEGATIVE_INFINITY), result);
    }

    // [JACKSON-349]
    public void testEmptyAsNumber() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        assertNull(m.readValue(quote(""), Integer.class));
        assertNull(m.readValue(quote(""), Long.class));
        assertNull(m.readValue(quote(""), Float.class));
        assertNull(m.readValue(quote(""), Double.class));
        assertNull(m.readValue(quote(""), BigInteger.class));
        assertNull(m.readValue(quote(""), BigDecimal.class));
    }

    // // Tests for [JACKSON-668]
    
    public void testDeserializeDecimalHappyPath() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"defaultValue\": { \"value\": 123 } }";
        MyBeanHolder result = mapper.readValue(json, MyBeanHolder.class);
        assertEquals(BigDecimal.valueOf(123), result.defaultValue.value.decimal);
    }

    public void testDeserializeDecimalProperException() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"defaultValue\": { \"value\": \"123\" } }";
        try {
            mapper.readValue(json, MyBeanHolder.class);
            fail("should have raised exception");
        } catch (JsonParseException e) {
            verifyException(e, "not numeric");
        }
    }

    public void testDeserializeDecimalProperExceptionWhenIdSet() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"id\": 5, \"defaultValue\": { \"value\": \"123\" } }";
        try {
            MyBeanHolder result = mapper.readValue(json, MyBeanHolder.class);
            fail("should have raised exception instead value was set to " + result.defaultValue.value.decimal.toString());
        } catch (JsonParseException e) {
            verifyException(e, "not numeric");
        }
    }
}
