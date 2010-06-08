package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ObjectMapper.DefaultTyping;

public class TestDefaultForArrays
    extends BaseMapTest
{
    /*
    /****************************************************** 
    /* Helper types
    /****************************************************** 
     */

    static class ArrayBean {
        public Object[] values;

        public ArrayBean() { this(null); }
        public ArrayBean(Object[] v) { values = v; }
    }
    
    /*
    /****************************************************** 
    /* Unit tests
    /****************************************************** 
     */

    /**
     * Simple unit test for verifying that we get String array
     * back, even though declared type is Object array
     */
    public void testArrayTypingSimple() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.enableDefaultTyping(DefaultTyping.NON_CONCRETE_AND_ARRAYS);
        ArrayBean bean = new ArrayBean(new String[0]);
        String json = m.writeValueAsString(bean);
        ArrayBean result = m.readValue(json, ArrayBean.class);
        assertNotNull(result.values);
        assertEquals(String[].class, result.values.getClass());
    }

    // And let's try it with deeper array as well
    public void testArrayTypingNested() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.enableDefaultTyping(DefaultTyping.NON_CONCRETE_AND_ARRAYS);
        ArrayBean bean = new ArrayBean(new String[0][0]);
        String json = m.writeValueAsString(bean);
        ArrayBean result = m.readValue(json, ArrayBean.class);
        assertNotNull(result.values);
        assertEquals(String[][].class, result.values.getClass());
    }
}
