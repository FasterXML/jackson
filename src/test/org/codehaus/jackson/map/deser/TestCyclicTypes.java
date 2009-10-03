package org.codehaus.jackson.map.deser;

import javax.xml.bind.annotation.*;

import org.codehaus.jackson.map.BaseMapTest;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

/**
 * Simple unit tests to verify that it is possible to handle
 * potentially cyclic structures, as long as object graph itself
 * is not cyclic. This is the case for directed hierarchies like
 * trees and DAGs.
 */
public class TestCyclicTypes
    extends BaseMapTest
{
    /*
    //////////////////////////////////////////////
    // Helper bean classes
    //////////////////////////////////////////////
     */

    static class Bean
    {
        Bean _next;
        String _name;

        public Bean() { }

        public void setNext(Bean b) { _next = b; }
        public void setName(String n) { _name = n; }

    }

    // Also another one to ensure JAXB annotation introspector has no problems
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "badTestCase", propOrder = { "id", "cicrular" })
    static class JaxbBean
    {
        @XmlElement(required = true)
        protected int id;

        @XmlElement(required = false)
        protected JaxbBean circular;
    }

    /*
    //////////////////////////////////////////////
    // Unit tests
    //////////////////////////////////////////////
     */

    public void testLinked() throws Exception
    {
        Bean first = new ObjectMapper().readValue
            ("{\"name\":\"first\", \"next\": { "
             +" \"name\":\"last\", \"next\" : null }}",
             Bean.class);

        assertNotNull(first);
        assertEquals("first", first._name);
        Bean last = first._next;
        assertNotNull(last);
        assertEquals("last", last._name);
        assertNull(last._next);
    }

    public void testWithJAXB() throws Exception
    {
        String jsonData = "{\"id\":1}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        mapper.readValue(jsonData, JaxbBean.class);

    }
}
