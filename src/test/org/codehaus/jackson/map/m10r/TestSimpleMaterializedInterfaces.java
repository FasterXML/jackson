package org.codehaus.jackson.map.m10r;

import org.codehaus.jackson.map.BaseMapTest;
import org.codehaus.jackson.map.ObjectMapper;

public class TestSimpleMaterializedInterfaces
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Test classes, enums
    /**********************************************************
     */

    interface Bean {
        public int getX();
        public String getA();
    }
    
    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testSimpleInteface() throws Exception
    {
        /*
        ObjectMapper mapper = new ObjectMapper();
        Bean bean = mapper.readValue("{\"a\":\"value\",\"b\":123", Bean.class);
        assertNotNull(bean);
        assertEquals("value", bean.getA());
        assertEquals(123, bean.getX());
*/
    
    }
}
