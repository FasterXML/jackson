package org.codehaus.jackson.map.deser;

import java.util.*;

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

        public Wrapper() { }

        public Wrapper(T v) { value = v; }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Wrapper) && (((Wrapper) o).value.equals(value));
        }
    }

    static class BoundedWrapper<A extends SimpleBean>
    {
        public List<A> values;
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

    /**
     * Test related to type bound handling problem within
     * [JACKSON-190]
     */
    public void testBounded() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        BoundedWrapper<SimpleBean> result = mapper.readValue
            ("{\"values\":[ {\"x\":3} ] } ", new TypeReference<BoundedWrapper<SimpleBean>>() {});
        List<?> list = result.values;
        assertEquals(1, list.size());
        Object ob = list.get(0);
        assertEquals(BoundedWrapper.class, ob.getClass());
        assertEquals(3, result.values.get(0).x);
    }

    /**
     * Unit test for verifying that we can use different
     * type bindings for individual generic types;
     * problem with [JACKSON-190]
     */
    public void testMultipleWrappers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();

        // First, numeric wrapper
        Wrapper<Boolean> result = mapper.readValue
            ("{\"value\": true}", new TypeReference<Wrapper<Boolean>>() { });
        assertEquals(new Wrapper<Boolean>(Boolean.TRUE), result);

        // Then string one
        Wrapper<String> result2 = mapper.readValue
            ("{\"value\": \"abc\"}", new TypeReference<Wrapper<String>>() { });
        assertEquals(new Wrapper<String>("abc"), result2);

        // And then number
        Wrapper<Long> result3 = mapper.readValue
            ("{\"value\": 7}", new TypeReference<Wrapper<Long>>() { });
        assertEquals(new Wrapper<Long>(7L), result3);
    }

    /**
     * Unit test for verifying fix to [JACKSON-109].
     */
    public void testArrayOfGenericWrappers() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        Wrapper<SimpleBean>[] result = mapper.readValue
            ("[ {\"value\": { \"x\" : 9 } } ]",
             new TypeReference<Wrapper<SimpleBean>[]>() { });
        assertNotNull(result);
        assertEquals(Wrapper[].class, result.getClass());
        assertEquals(1, result.length);
        Wrapper<SimpleBean> elem = result[0];
        Object contents = elem.value;
        assertNotNull(contents);
        assertEquals(SimpleBean.class, contents.getClass());
        SimpleBean bean = (SimpleBean) contents;
        assertEquals(9, bean.x);
    }
}
