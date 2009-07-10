package org.codehaus.jackson.schema;

import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author Ryan Heaton
 */
public class TestGenerateJsonSchema
    extends org.codehaus.jackson.map.BaseMapTest
{
    /**
     * tests generating json-schema stuff.
     */
    public void testGeneratingJsonSchema()
            throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema jsonSchema = mapper.generateJsonSchema(SimpleBean.class);
        assertNotNull(jsonSchema);
	ObjectNode root = jsonSchema.getSchemaNode();
        assertEquals("object", root.get("type").getValueAsText());
        assertEquals(true, root.get("optional").getBooleanValue());
        JsonNode propertiesSchema = root.get("properties");
        assertNotNull(propertiesSchema);
        JsonNode property1Schema = propertiesSchema.get("property1");
        assertNotNull(property1Schema);
        assertEquals("integer", property1Schema.get("type").getValueAsText());
        assertEquals(true, property1Schema.get("optional").getBooleanValue());
        JsonNode property2Schema = propertiesSchema.get("property2");
        assertNotNull(property2Schema);
        assertEquals("string", property2Schema.get("type").getValueAsText());
        assertEquals(true, property2Schema.get("optional").getBooleanValue());
        JsonNode property3Schema = propertiesSchema.get("property3");
        assertNotNull(property3Schema);
        assertEquals("array", property3Schema.get("type").getValueAsText());
        assertEquals(true, property3Schema.get("optional").getBooleanValue());
        assertEquals("string", property3Schema.get("items").get("type").getValueAsText());
        JsonNode property4Schema = propertiesSchema.get("property4");
        assertNotNull(property4Schema);
        assertEquals("array", property4Schema.get("type").getValueAsText());
        assertEquals(true, property4Schema.get("optional").getBooleanValue());
        assertEquals("number", property4Schema.get("items").get("type").getValueAsText());
    }

    /**
     * Additional unit test for verifying that schema object itself
     * is properly serializable
     *
     * @since 1.2
     */
    public void testSchemaSerialization()
            throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema jsonSchema = mapper.generateJsonSchema(SimpleBean.class);
	Map<String,Object> result = writeAndMap(mapper, jsonSchema);
	assertNotNull(result);
	// no need to check out full structure, just basics...
	assertEquals("object", result.get("type"));
	assertEquals(Boolean.TRUE, result.get("optional"));
	assertNotNull(result.get("properties"));
    }

    public static class SimpleBean
    {
        private int property1;
        private String property2;
        private String[] property3;
        private Collection<Float> property4;

        public int getProperty1()
        {
            return property1;
        }

        public void setProperty1(int property1)
        {
            this.property1 = property1;
        }

        public String getProperty2()
        {
            return property2;
        }

        public void setProperty2(String property2)
        {
            this.property2 = property2;
        }

        public String[] getProperty3()
        {
            return property3;
        }

        public void setProperty3(String[] property3)
        {
            this.property3 = property3;
        }

        public Collection<Float> getProperty4()
        {
            return property4;
        }

        public void setProperty4(Collection<Float> property4)
        {
            this.property4 = property4;
        }
    }

}
