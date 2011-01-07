package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonPropertyOrder;
import org.codehaus.jackson.annotate.JsonRawValue;
import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * This unit test suite tests functioning of {@link JsonRawValue}
 * annotation with bean serialization.
 */
public class TestAnnotationJsonRawValue
    extends BaseMapTest
{
    /*
    /*********************************************************
    /* Helper bean classes
    /*********************************************************
     */

    /// Class for testing {@link JsonRawValue} annotations with getters returning String
    @JsonPropertyOrder(alphabetic=true)
    final static class ClassGetter<T>
    {
    	private final T _value;
    	
        private ClassGetter(T value) { _value = value;}
 
        public T getNonRaw() { return _value; }

        @JsonProperty("raw") @JsonRawValue public T foobar() { return _value; }
        
        @JsonProperty @JsonRawValue protected T value() { return _value; }
    }
    
    /*
    /*********************************************************
    /* Test cases
    /*********************************************************
     */

    public void testSimpleStringGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String value = "abc";
        String result = m.writeValueAsString(new ClassGetter<String>(value));
        String expected = String.format("{\"nonRaw\":\"%s\",\"raw\":%s,\"value\":%s}", value, value, value);
        assertEquals(expected, result);
    }

    public void testSimpleNonStringGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        int value = 123;
        String result = m.writeValueAsString(new ClassGetter<Integer>(value));
        String expected = String.format("{\"nonRaw\":%d,\"raw\":%d,\"value\":%d}", value, value, value);
        assertEquals(expected, result);
    }

    public void testNullStringGetter() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String value = null;
        String result = m.writeValueAsString(new ClassGetter<String>(value));
        String expected = String.format("{\"nonRaw\":%d,\"raw\":%d,\"value\":%d}", value, value, value);
        assertEquals(expected, result);
    }

}
