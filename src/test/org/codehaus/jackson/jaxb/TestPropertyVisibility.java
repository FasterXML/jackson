package org.codehaus.jackson.jaxb;

import java.io.IOException;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Those JAXB support unit tests that fail, mostly because our JAXB
 * introspector is not all that good... But fixing that is easiest
 * done once we rewrite the method introspector (can't do it with
 * current version 1.8)
 */
public class TestPropertyVisibility
    extends BaseJaxbTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    @XmlAccessorType(XmlAccessType.NONE)
    protected static class Bean354
    {
        protected String name = "foo";
    
        @XmlElement
        protected String getName() { return name; }

        public void setName(String s) { name = s; }
    }

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

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // Verify serialization wrt [JACKSON-354]
    //
    // NOTE: fails currently because we use Bean Introspector which only sees public methods -- need to rewrite
    public void testJackson354Serialization() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        assertEquals("{\"name\":\"foo\"}", mapper.writeValueAsString(new Bean354()));
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
