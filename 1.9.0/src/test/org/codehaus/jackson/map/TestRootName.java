package org.codehaus.jackson.map;

import org.codehaus.jackson.map.annotate.JsonRootName;

public class TestRootName extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    @JsonRootName("rudy")
    static class Bean {
        public int a = 3;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testRootViaMapper() throws Exception
    {
        ObjectMapper mapper = rootMapper();
        String json = mapper.writeValueAsString(new Bean());
        assertEquals("{\"rudy\":{\"a\":3}}", json);
        Bean bean = mapper.readValue(json, Bean.class);
        assertNotNull(bean);
    }

    public void testRootViaWriterAndReader() throws Exception
    {
        ObjectMapper mapper = rootMapper();
        String json = mapper.writer().writeValueAsString(new Bean());
        assertEquals("{\"rudy\":{\"a\":3}}", json);
        Bean bean = mapper.reader(Bean.class).readValue(json);
        assertNotNull(bean);
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private ObjectMapper rootMapper()
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationConfig.Feature.WRAP_ROOT_VALUE, true);
        mapper.configure(DeserializationConfig.Feature.UNWRAP_ROOT_VALUE, true);
        return mapper;
    }
}
