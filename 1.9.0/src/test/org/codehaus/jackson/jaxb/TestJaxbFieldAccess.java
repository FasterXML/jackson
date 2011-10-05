package org.codehaus.jackson.jaxb;

import java.io.IOException;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public class TestJaxbFieldAccess
    extends org.codehaus.jackson.map.BaseMapTest
{
    /*
    /**********************************************************
    /* Helper beans
    /**********************************************************
     */

    @XmlAccessorType(XmlAccessType.FIELD)
    static class Fields {
        protected int x;

        public Fields() { }
        Fields(int x) { this.x = x; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    // Verify serialization wrt [JACKSON-202]
    public void testFieldSerialization() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        assertEquals("{\"x\":3}", serializeAsString(mapper, new Fields(3)));
    }

    // Verify deserialization wrt [JACKSON-202]
    public void testFieldDeserialization() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        Fields result = mapper.readValue("{ \"x\":3 }", Fields.class);
        assertEquals(3, result.x);
    }
}
