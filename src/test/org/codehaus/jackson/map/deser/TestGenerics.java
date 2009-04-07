package org.codehaus.jackson.map.deser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestGenerics
    extends BaseMapTest
{
    static abstract class BaseBean<T extends Number>
    {
        public abstract void setNumber(T value);
    }

    static class NumberBean
        extends BaseBean<Integer>
    {
        int _number;

        @Override
        public void setNumber(Integer value)
        {
            _number = value.intValue();
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Test cases
    //////////////////////////////////////////////////////////
     */

    public void testBooleanPrimitive() throws Exception
    {
        // first, simple case:
        ObjectMapper mapper = new ObjectMapper();
        NumberBean result = mapper.readValue(new StringReader("{\"number\":17}"), NumberBean.class);
        assertEquals(17, result._number);
    }
}
