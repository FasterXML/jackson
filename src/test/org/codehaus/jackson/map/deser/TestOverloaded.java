package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.*;

/**
 * Unit tests related to handling of overloaded methods;
 * and specifically addressing problem [JACKSON-189].
 *
 * @since 1.5
 */
public class TestOverloaded
    extends BaseMapTest
{
    static class BaseListBean
    {
        List<String> list;

        BaseListBean() { }

        public void setList(List<String> l) { list = l; }
    }

    static class ArrayListBean extends BaseListBean
    {
        ArrayListBean() { }

        public void setList(ArrayList<String> l) { super.setList(l); }
    }

    // 27-Feb-2010, tatus: Won't fix immediately, need to comment out
    /*
    static class OverloadBean
    {
        String a;

        public OverloadBean() { }

        public void setA(int value) { a = String.valueOf(value); }
        public void setA(String value) { a = value; }
    }
    */

    /*
    /************************************************************
    /* Unit tests
    /************************************************************
    */

    /**
     * Unit test related to [JACKSON-189]
     */
    // 27-Feb-2010, tatus: Won't fix immediately, need to comment out
    /*
    public void testSimpleOverload() throws Exception
    {
        OverloadBean bean;
        try {
            bean = new ObjectMapper().readValue("{ \"a\" : 13 }", OverloadBean.class);
        } catch (JsonMappingException e) {
            fail("Did not expect an exception, got: "+e.getMessage());
            return;
        }
        assertEquals("13", bean.a);
    }
    */

    /**
     * It should be ok to overload with specialized 
     * version; more specific method should be used.
     */
    public void testSpecialization() throws Exception
    {
        ArrayListBean bean = new ObjectMapper().readValue
            ("{\"list\":[\"a\",\"b\",\"c\"]}", ArrayListBean.class);
        assertNotNull(bean.list);
        assertEquals(3, bean.list.size());
        assertEquals(ArrayList.class, bean.list.getClass());
        assertEquals("a", bean.list.get(0));
        assertEquals("b", bean.list.get(1));
        assertEquals("c", bean.list.get(2));
    }
}
