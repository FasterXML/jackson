package org.codehaus.jackson.jaxb;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public class BaseJaxbTest
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
         mapper.getDeserializationConfig().setAnnotationIntrospector(intr);
         mapper.getSerializationConfig().setAnnotationIntrospector(intr);
         return mapper;
     }
}
