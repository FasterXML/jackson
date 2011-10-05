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
    /**********************************************************
    /* Helper bean classes
    /**********************************************************
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
    static class JaxbBean
    {
        @XmlElement(required = true)
        protected int id;

        @XmlElement(required = false)
        protected JaxbBean circular;
    }

    static class LinkA {
        public LinkB next;
    }

    static class LinkB {
        private LinkA a;

        public void setA(LinkA a) { this.a = a; }
        public LinkA getA() { return a; }
    }

    static class GenericLink<T> {
        public GenericLink<T> next;
    }

    static class StringLink extends GenericLink<String> {
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
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

    public void testLinkedGeneric() throws Exception
    {
        StringLink link = new ObjectMapper().readValue
            ("{\"next\":null}", StringLink.class);
        assertNotNull(link);
        assertNull(link.next);
    }

    // Added to check for [JACKSON-171]
    public void testWithJAXB() throws Exception
    {
        String jsonData = "{\"id\":1}";
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        mapper.readValue(jsonData, JaxbBean.class);
    }

    public void testCycleWith2Classes() throws Exception
    {
        LinkA a = new ObjectMapper().readValue("{\"next\":{\"a\":null}}", LinkA.class);
        assertNotNull(a.next);
        LinkB b = a.next;
        assertNull(b.a);
    }
}
