package org.codehaus.jackson.jaxrs;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.annotate.JsonView;

public class TestJsonView extends main.BaseTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */
    
    static class MyView1 { }
    static class MyView2 { }

    static class Bean {
        @JsonView(MyView1.class)
        public int value1 = 1;

        @JsonView(MyView2.class)
        public int value2 = 2;
    }


    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [JACKSON-578]
    public void testViews() throws Exception
    {
        JacksonJsonProvider prov = new JacksonJsonProvider();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Bean bean = new Bean();
        Annotation ann = new JsonView() {
            @Override
            public Class<?>[] value() { return new Class[] { MyView1.class }; }

            @Override
            public Class<? extends Annotation> annotationType() {
                return JsonView.class;
            }
        };
        prov.writeTo(bean, bean.getClass(), bean.getClass(), new Annotation[] { ann },
                MediaType.APPLICATION_JSON_TYPE, null, out);
        assertEquals("{\"value1\":1}", out.toString("UTF-8"));
    }
    

}
