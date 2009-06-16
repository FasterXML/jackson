package org.codehaus.jackson.map.deser;

import java.io.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.TypeReference;

public class TestGenerics
    extends BaseMapTest
{
    static abstract class BaseNumberBean<T extends Number>
    {
        public abstract void setNumber(T value);
    }

    static class NumberBean
        extends BaseNumberBean<Integer>
    {
        int _number;

        @Override
        public void setNumber(Integer value)
        {
            _number = value.intValue();
        }
    }

    /**
     * Very simple bean class
     */
    static class SimpleBean
    {
        public int x;
    }

    static class Wrapper<T>
    {
        public T value;
    }

    /*
    //////////////////////////////////////////////////////////
    // Test cases
    //////////////////////////////////////////////////////////
     */

    public void testSimpleNumberBean() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        NumberBean result = mapper.readValue("{\"number\":17}", NumberBean.class);
        assertEquals(17, result._number);
    }

    /**
     * Unit test for verifying fix to [JACKSON-109].
     */
    public void testGenericWrapper() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper<SimpleBean> result = mapper.readValue
            ("{\"value\": { \"x\" : 13 } }",
             new TypeReference<Wrapper<SimpleBean>>() { });
        assertNotNull(result);
        assertEquals(Wrapper.class, result.getClass());
        Object contents = result.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(13, bean.x);
    }
}
