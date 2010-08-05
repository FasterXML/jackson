package org.codehaus.jackson.mrbean;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.mrbean.TestSimpleMaterializedInterfaces.Bean;

public class TestAbstractClasses
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes, enums
    /**********************************************************
     */

    public abstract static class Bean
    {
        int y;
        
        public abstract String getX();

        public String getFoo() { return "Foo!"; }
        public void setY(int value) { y = value; }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleInteface() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.getDeserializationConfig().setAbstractTypeResolver(new AbstractTypeMaterializer());
        Bean bean = mapper.readValue("{ \"x\" : \"abc\", y : 13 }", Bean.class);
        assertNotNull(bean);
        assertEquals("abc", bean.getX());
        assertEquals(13, bean.y);
        assertEquals("Foo!", bean.getFoo());
        assertEquals(123, bean.getX());
    }
    
}
