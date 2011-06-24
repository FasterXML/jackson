package org.codehaus.jackson.failing;

//import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlTransient;

import org.codehaus.jackson.jaxb.BaseJaxbTest;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * Tests for [JACKSON-539]
 * 
 * @author Ryan Heaton
 */
public class TestSerializationToAtomContent extends BaseJaxbTest
{
    // Note: full example would be "Content"; but let's use simpler demonstration here, easier to debug
    @XmlAccessorType(XmlAccessType.PROPERTY)
    static class Jackson539Bean
    {
        protected int type;
        
        @XmlTransient
        public String getType() {
            throw new UnsupportedOperationException();
        }

        public void setType(String type) {
            throw new UnsupportedOperationException();
        }

        @XmlAttribute(name = "type")
        public int getRawType() {
           return type;
        }

        public void setRawType(int type) {
           this.type = type;
        }
    }
    
    // For [JACKSON-539]
    public void testJacksonSerialization()
            throws Exception
    {
        /* Earlier
        Content content = new Content();
        content.setRawType("application/json");
        String json = mapper.writeValueAsString(content);
        Content content2 = mapper.readValue(json, Content.class); // deserialize
        assertNotNull(content2);
         */
        
        Jackson539Bean input = new Jackson539Bean();
        input.type = 123;
        ObjectMapper mapper = getJaxbMapper();
        String json = mapper.writeValueAsString(input);
        Jackson539Bean result = mapper.readValue(json, Jackson539Bean.class);
        assertNotNull(result);
        assertEquals(123, result.type);
    }
}
