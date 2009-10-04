package org.codehaus.jackson.schema;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * Trivial test to ensure {@link JsonSchema} can be also deserialized
 */
public class TestReadJsonSchema
    extends org.codehaus.jackson.map.BaseMapTest
{
    static class Schemable {
        public String name;

        public boolean[] states;

        public List<String> extra;

        public Map<String,Double> sizes;
    }

    /**
     * Verifies that a simple schema that is serialized can be
     * deserialized back to equal schema instance
     */
    public void testDeserializeSimple() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema schema = mapper.generateJsonSchema(Schemable.class);
        assertNotNull(schema);

        String schemaStr = mapper.writeValueAsString(schema);
        assertNotNull(schemaStr);
        JsonSchema result = mapper.readValue(schemaStr, JsonSchema.class);
        assertEquals("Trying to read from '"+schemaStr+"'", schema, result);
    }
}
