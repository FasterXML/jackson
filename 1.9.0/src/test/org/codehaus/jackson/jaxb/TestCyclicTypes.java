package org.codehaus.jackson.jaxb;

import java.util.*;

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

    @XmlAccessorType(XmlAccessType.PUBLIC_MEMBER)
    static class Bean
    {
        private Bean _next;
        final String _name;

        public Bean(Bean next, String name) {
            _next = next;
            _name = name;
        }

        public Bean getNext() { return _next; }
        public String getName() { return _name; }

        public void assignNext(Bean n) { _next = n; }
    }

    /*
    /**********************************************************
    /* Types
    /**********************************************************
     */

    /* Added to check for [JACKSON-171], i.e. that type its being
     * cyclic is not a problem (instances are).
     */
    public void testWithJAXB() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new JaxbAnnotationIntrospector());
        Bean bean =  new Bean(null, "abx");

        Map<String,Object> results = writeAndMap(mapper, bean);
        assertEquals(2, results.size());
        assertEquals("abx", results.get("name"));
        assertTrue(results.containsKey("next"));
        assertNull(results.get("next"));
    }
}
