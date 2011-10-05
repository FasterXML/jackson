package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * Unit tests for verifying that simple exceptions can be deserialized.
 */
public class TestExceptionDeserialization
    extends BaseMapTest
{
    @SuppressWarnings("serial")
    static class MyException extends Exception
    {
        protected int value;

        protected String myMessage;
        protected HashMap<String,Object> stuff = new HashMap<String, Object>();
        
        @JsonCreator
        MyException(@JsonProperty("message") String msg, @JsonProperty("value") int v)
        {
            super(msg);
            myMessage = msg;
            value = v;
        }

        public int getValue() { return value; }
        
        public String getFoo() { return "bar"; }

        @JsonAnySetter public void setter(String key, Object value)
        {
            stuff.put(key, value);
        }
    }

    @SuppressWarnings("serial")
    static class MyNoArgException extends Exception
    {
        @JsonCreator MyNoArgException() { }
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testIOException() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        IOException ioe = new IOException("TEST");
        String json = mapper.writeValueAsString(ioe);
        IOException result = mapper.readValue(json, IOException.class);
        assertEquals(ioe.getMessage(), result.getMessage());
    }

    // As per [JACKSON-377]
    public void testWithCreator() throws IOException
    {
        final String MSG = "the message";
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(new MyException(MSG, 3));

        MyException result = mapper.readValue(json, MyException.class);
        assertEquals(MSG, result.getMessage());
        assertEquals(3, result.value);
        assertEquals(1, result.stuff.size());
        assertEquals(result.getFoo(), result.stuff.get("foo"));
    }

    // [JACKSON-388]
    public void testWithNullMessage() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        String json = mapper.writeValueAsString(new IOException((String) null));
        IOException result = mapper.readValue(json, IOException.class);
        assertNotNull(result);
        assertNull(result.getMessage());
    }

    public void testNoArgsException() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        MyNoArgException exc = mapper.readValue("{}", MyNoArgException.class);
        assertNotNull(exc);
    }
}
