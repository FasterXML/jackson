package org.codehaus.jackson.jaxb;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSchema;

/**
 * Test(s) to see that JAXB annotations-based information is properly
 * accessible and used by JSON Schema generation
 * 
 * @author tatu
 */
public class TestSchemaGeneration extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */
    
    @XmlAccessorType(XmlAccessType.FIELD)
    protected static class Person {
        public String firstName;
        public String lastName;
    
        @XmlElement(type=Address.class)
        public IAddress address;
    }

    protected interface IAddress {     
        public String getCity();
        public void setCity(String city);
    }

    protected static class Address implements IAddress {
        private String city;
        private String state;
        
        @Override
        public String getCity() { return city; }
        @Override
        public void setCity(String city) { this.city = city; }
        public String getState() { return state; }
        public void setState(String state) { this.state = state; }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */
    
    /**
     * Test for [JACKSON-415]
     * 
     * @since 1.7
     */
    public void testWithJaxb() throws Exception
    {
        ObjectMapper mapper = getJaxbMapper();
        JsonSchema jsonSchema = mapper.generateJsonSchema(Address.class);
        ObjectNode root = jsonSchema.getSchemaNode();
        // should find two properties ("city", "state"), not just one...
        JsonNode itemsNode = root.findValue("properties");
        assertNotNull("Missing 'state' field", itemsNode.get("state"));
        assertNotNull("Missing 'city' field", itemsNode.get("city"));
        assertEquals(2, itemsNode.size());
    }
}
