package org.codehaus.jackson.failing;

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
public class TestJAXBMethodVisibility
    extends org.codehaus.jackson.map.BaseMapTest
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
}
