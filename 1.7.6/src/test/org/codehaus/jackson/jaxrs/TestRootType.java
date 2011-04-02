package org.codehaus.jackson.jaxrs;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.annotate.JsonTypeInfo.Id;
import org.codehaus.jackson.type.TypeReference;

public class TestRootType
    extends main.BaseTest
{
    @JsonTypeInfo(use=Id.NAME, include=As.WRAPPER_OBJECT, property="type")
    @JsonTypeName("bean")
    static class Bean {
        public int a = 3;
    }
    
    public void testRootType() throws Exception
    {
        JacksonJsonProvider prov = new JacksonJsonProvider();
        TypeReference<?> ref = new TypeReference<List<Bean>>(){};

        Bean bean = new Bean();
        ArrayList<Bean> list = new ArrayList<Bean>();
        list.add(bean);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        prov.writeTo(list, List.class, ref.getType(), new Annotation[0], MediaType.APPLICATION_JSON_TYPE, null, out);

        String json = out.toString("UTF-8");
        assertEquals("[{\"bean\":{\"a\":3}}]", json);
    }
}
