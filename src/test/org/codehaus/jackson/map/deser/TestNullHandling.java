package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.module.SimpleModule;

public class TestNullHandling extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    static class FunnyNullDeserializer extends JsonDeserializer<String>
    {
        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            return "text";
        }

        @Override
        public String getNullValue() { return "funny"; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // Test for [JACKSON-643]
    public void testCustomRootNulls() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        SimpleModule module = new SimpleModule("test", Version.unknownVersion());
        module.addDeserializer(String.class, new FunnyNullDeserializer());
        mapper.registerModule(module);

        // should get non-default null directly:
        String str = mapper.readValue("null", String.class);
        assertNotNull(str);
        assertEquals("funny", str);
        
        // as well as via ObjectReader
        ObjectReader reader = mapper.reader(String.class);
        str = reader.readValue("null");
        assertNotNull(str);
        assertEquals("funny", str);
    }
}
