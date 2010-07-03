package org.codehaus.jackson.jaxrs;

import java.io.*;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;

public class TestJsonpWrapping
    extends main.BaseTest
{
    public void testSimple() throws Exception
    {
        JacksonJsonProvider prov = new JacksonJsonProvider();
        Object bean = new Integer[] { 1, 2, 3 };

        // First: no JSONP wrapping:
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        prov.writeTo(bean, bean.getClass(), bean.getClass(), new Annotation[0], MediaType.APPLICATION_JSON_TYPE, null, out);
        assertEquals("[1,2,3]", out.toString("UTF-8"));
        
        // then with wrapping:
        prov.setJSONPFunctionName("addAll");
        out = new ByteArrayOutputStream();
        prov.writeTo(bean, bean.getClass(), bean.getClass(), new Annotation[0], MediaType.APPLICATION_JSON_TYPE, null, out);
        assertEquals("addAll([1,2,3])", out.toString("UTF-8"));
    }
}
