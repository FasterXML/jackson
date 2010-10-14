package org.codehaus.jackson.map.deser;

import java.io.IOException;

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
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

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
}
