package org.codehaus.jackson.map;

import java.io.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.*;

public class TestObjectMapper
    extends BaseMapTest
{
    public void testProps()
    {
        ObjectMapper m = new ObjectMapper();
        // should have default factory
        assertNotNull(m.getNodeFactory());
        JsonNodeFactory nf = JsonNodeFactory.instance;
        m.setNodeFactory(nf);
        assertSame(nf, m.getNodeFactory());
    }

    public void testSupport()
    {
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.canSerialize(String.class));

        assertTrue(m.canDeserialize(TypeFactory.fromClass(String.class)));
    }

    public void testTreeRead() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String JSON = "{ }";
        JsonNode n = m.readTree(JSON);
        assertTrue(n instanceof ObjectNode);

        n = m.readTree(new StringReader(JSON));
        assertTrue(n instanceof ObjectNode);

        n = m.readTree(new ByteArrayInputStream(JSON.getBytes("UTF-8")));
        assertTrue(n instanceof ObjectNode);
    }
}
