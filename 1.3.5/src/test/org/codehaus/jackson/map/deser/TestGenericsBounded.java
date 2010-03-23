package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.TypeReference;

import java.io.Serializable;

public class TestGenericsBounded
    extends BaseMapTest
{
    static class BoundedWrapper<A extends Serializable>
    {
        public List<A> values;
    }

    @SuppressWarnings("serial")
    static class IntBean implements Serializable
    {
        public int x;
    }

    /**
     * Test related to type bound handling problem within
     * [JACKSON-190]
     */
    public void testBounded() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        BoundedWrapper<IntBean> result = mapper.readValue
            ("{\"values\":[ {\"x\":3} ] } ", new TypeReference<BoundedWrapper<IntBean>>() {});
        List<?> list = result.values;
        assertEquals(1, list.size());
        Object ob = list.get(0);
        assertEquals(IntBean.class, ob.getClass());
        assertEquals(3, result.values.get(0).x);
    }
}

