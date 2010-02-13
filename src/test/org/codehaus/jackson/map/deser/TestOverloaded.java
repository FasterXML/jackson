package org.codehaus.jackson.map.deser;

import java.util.*;

import static org.junit.Assert.*;

import org.codehaus.jackson.*;
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
    static class OverloadBean
    {
        String a;

        public OverloadBean() { }

        public void setA(int value) { a = String.valueOf(value); }
        public void setA(String value) { a = value; }
    }

    /**
     * Unit test related to [JACKSON-189]
     */
    public void testSimpleOverload() throws Exception
    {
        OverloadBean bean = new ObjectMapper().readValue
            ("{ \"a\" : 13 }", OverloadBean.class);
        assertEquals("13", bean.a);
    }
}
