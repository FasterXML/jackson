package org.codehaus.jackson.map;

import java.io.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.*;

public class TestObjectMapper extends BaseMapTest
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

        assertTrue(m.canDeserialize(TypeFactory.defaultInstance().constructType(String.class)));
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

    // Test to ensure that we can check property ordering defaults...
    public void testConfigForPropertySorting() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        
        // sort-alphabetically is disabled by default:
        assertFalse(m.isEnabled(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY));
        SerializationConfig sc = m.copySerializationConfig();
        assertFalse(sc.isEnabled(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY));
        assertFalse(sc.shouldSortPropertiesAlphabetically());
        DeserializationConfig dc = m.copyDeserializationConfig();
        assertFalse(dc.shouldSortPropertiesAlphabetically());

        // but when enabled, should be visible:
        m.enable(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY);
        sc = m.copySerializationConfig();
        assertTrue(sc.isEnabled(SerializationConfig.Feature.SORT_PROPERTIES_ALPHABETICALLY));
        assertTrue(sc.shouldSortPropertiesAlphabetically());
        dc = m.copyDeserializationConfig();
        // and not just via SerializationConfig, but also via DeserializationConfig
        assertTrue(dc.shouldSortPropertiesAlphabetically());
    }
}
