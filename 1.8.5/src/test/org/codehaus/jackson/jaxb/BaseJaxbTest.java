package org.codehaus.jackson.jaxb;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public abstract class BaseJaxbTest
    extends org.codehaus.jackson.map.BaseMapTest
{
    protected BaseJaxbTest() { }
    
    /*
     **************************************************************
     * Helper methods
     **************************************************************
     */

     protected ObjectMapper getJaxbMapper()
     {
         ObjectMapper mapper = new ObjectMapper();
         AnnotationIntrospector intr = new JaxbAnnotationIntrospector();
         mapper.setAnnotationIntrospector(intr);
         return mapper;
     }

     protected ObjectMapper getJaxbAndJacksonMapper()
     {
         ObjectMapper mapper = new ObjectMapper();
         AnnotationIntrospector intr = new AnnotationIntrospector.Pair(new JaxbAnnotationIntrospector(),
        		 new JacksonAnnotationIntrospector());
         mapper.setAnnotationIntrospector(intr);
         return mapper;
     }

}
