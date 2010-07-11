package org.codehaus.jackson.node;

import org.codehaus.jackson.map.BaseMapTest;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Unit tests for ensuring that it is possible to override default
 * {@link JsonNodeFactory} when doing data binding
 */
public class TestNodeFactoryConfig
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes
    /**********************************************************
     */

    /**
     * Test factory that just replaces JSON Object node with
     * custom version
     */
    static class MyNodeFactory
        extends JsonNodeFactory
    {
        public ObjectNode objectNode() { return new MyObjectNode(this); }
        
    }

    static class MyObjectNode
        extends ObjectNode
    {
        public MyObjectNode(JsonNodeFactory f) {
            super(f);
        }
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

     public void testWithObjectMapper() throws Exception
     {
         ObjectMapper m = new ObjectMapper();
         m.setNodeFactory(new MyNodeFactory());
         JsonNode n = m.readTree("{ \"a\":3 }");
         assertNotNull(n);
         assertSame(MyObjectNode.class, n.getClass());
     }

     public void testWithObjectReader() throws Exception
     {
         ObjectMapper m = new ObjectMapper();
         ObjectReader reader = m.reader(new MyNodeFactory());
         JsonNode n = reader.readTree("{ \"a\":3 }");
         assertNotNull(n);
         assertSame(MyObjectNode.class, n.getClass());
     }
}
