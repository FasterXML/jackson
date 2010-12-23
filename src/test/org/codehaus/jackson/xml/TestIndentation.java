package org.codehaus.jackson.xml;

import java.util.*;

import org.codehaus.jackson.map.SerializationConfig;

public class TestIndentation extends XmlTestBase
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class Bean {
        public StringWrapper string;
        
        public Bean() { }
        public Bean(String s) { string = new StringWrapper(s); }
    }
    
    /*
    /**********************************************************
    /* Set up
    /**********************************************************
     */

    protected XmlMapper _xmlMapper;

    // let's actually reuse XmlMapper to make things bit faster
    @Override
    public void setUp() throws Exception {
        super.setUp();
        _xmlMapper = new XmlMapper();
        _xmlMapper.configure(SerializationConfig.Feature.INDENT_OUTPUT, true);
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // Verify [JACKSON-444]
    public void testSimpleBean() throws Exception
    {
        String xml = _xmlMapper.writeValueAsString(new Bean("abc")); 
//System.out.println("XML = "+xml);

        // Let's verify we get similar stuff back, first:
        Bean result = _xmlMapper.readValue(xml, Bean.class);
        assertNotNull(result);
        assertEquals("abc", result.string.str);

    }

    public void testSimpleMap() throws Exception
    {
        Map<String,String> map = new HashMap<String,String>();
        map.put("a", "b");
        String xml = _xmlMapper.writeValueAsString(map);

        // Let's verify we get similar stuff back, first:
        Map<?,?> result = _xmlMapper.readValue(xml, Map.class);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("b", map.get("a"));
    }

}
