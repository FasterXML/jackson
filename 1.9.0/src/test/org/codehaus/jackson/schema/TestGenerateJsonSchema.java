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
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

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

    public class TrivialBean {
        public String name;
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    /**
     * tests generating json-schema stuff.
     */
    public void testGeneratingJsonSchema()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema jsonSchema = mapper.generateJsonSchema(SimpleBean.class);
        assertNotNull(jsonSchema);

        // test basic equality, and that equals() handles null, other obs
        assertTrue(jsonSchema.equals(jsonSchema));
        assertFalse(jsonSchema.equals(null));
        assertFalse(jsonSchema.equals("foo"));

        // other basic things
        assertNotNull(jsonSchema.toString());
        assertNotNull(JsonSchema.getDefaultSchemaNode());

	ObjectNode root = jsonSchema.getSchemaNode();
        assertEquals("object", root.get("type").asText());
        assertEquals(false, root.path("required").getBooleanValue());
        JsonNode propertiesSchema = root.get("properties");
        assertNotNull(propertiesSchema);
        JsonNode property1Schema = propertiesSchema.get("property1");
        assertNotNull(property1Schema);
        assertEquals("integer", property1Schema.get("type").asText());
        assertEquals(false, property1Schema.path("required").getBooleanValue());
        JsonNode property2Schema = propertiesSchema.get("property2");
        assertNotNull(property2Schema);
        assertEquals("string", property2Schema.get("type").asText());
        assertEquals(false, property2Schema.path("required").getBooleanValue());
        JsonNode property3Schema = propertiesSchema.get("property3");
        assertNotNull(property3Schema);
        assertEquals("array", property3Schema.get("type").asText());
        assertEquals(false, property3Schema.path("required").getBooleanValue());
        assertEquals("string", property3Schema.get("items").get("type").asText());
        JsonNode property4Schema = propertiesSchema.get("property4");
        assertNotNull(property4Schema);
        assertEquals("array", property4Schema.get("type").asText());
        assertEquals(false, property4Schema.path("required").getBooleanValue());
        assertEquals("number", property4Schema.get("items").get("type").asText());
    }

    /**
     * Additional unit test for verifying that schema object itself
     * can be properly serialized
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
	// only add 'required' if it is true...
	assertNull(result.get("required"));
	assertNotNull(result.get("properties"));
    }

    public void testInvalidCall()
        throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // not ok to pass null
        try {
            mapper.generateJsonSchema(null);
        } catch (IllegalArgumentException iae) { }
    }

    /**
     * Test for [JACKSON-454]
     */
    public void testThatObjectsHaveNoItems() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema jsonSchema = mapper.generateJsonSchema(TrivialBean.class);
        String json = jsonSchema.toString().replaceAll("\"", "'");
        // can we count on ordering being stable? I think this is true with current ObjectNode impl
        // as perh [JACKSON-563]; 'required' is only included if true
        assertEquals("{'type':'object','properties':{'name':{'type':'string'}}}",
                json);
    }
}
