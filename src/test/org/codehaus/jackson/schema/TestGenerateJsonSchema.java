package org.codehaus.jackson.schema;

import junit.framework.TestCase;

import java.util.Collection;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.JsonNode;

/**
 * @author Ryan Heaton
 */
public class TestGenerateJsonSchema
        extends TestCase
{

    /**
     * tests generating json-schema stuff.
     */
    public void testGeneratingJsonSchema()
            throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        JsonSchema jsonSchema = mapper.generateJsonSchema(SimpleBean.class);
//        System.out.println(jsonSchema.toString());
        assertNotNull(jsonSchema);
        assertEquals("object", jsonSchema.getSchemaNode().get("type").getValueAsText());
        assertEquals(true, jsonSchema.getSchemaNode().get("optional").getBooleanValue());
        JsonNode propertiesSchema = jsonSchema.getSchemaNode().get("properties");
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
