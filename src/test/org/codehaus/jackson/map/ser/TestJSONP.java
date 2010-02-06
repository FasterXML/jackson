package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.util.JSONPObject;

public class TestJSONP
    extends BaseMapTest
{
    static class Base {
        public String a;
    }
    static class Impl extends Base {
        public String b;

        public Impl(String a, String b) {
            this.a = a;
            this.b = b;
        }
    }
    
    public void testSimpleScalars() throws Exception
    {
        assertEquals("callback(\"abc\")",
                serializeAsString(new JSONPObject("callback", "abc")));
        assertEquals("calc(123)",
                serializeAsString(new JSONPObject("calc", Integer.valueOf(123))));
        assertEquals("dummy(null)",
                serializeAsString(new JSONPObject("dummy", null)));
    }

    public void testWithType() throws Exception
    {
        Object ob = new Impl("abc", "def");
        assertEquals("do({\"a\":\"abc\"})",
                serializeAsString(new JSONPObject("do", ob, Base.class)));
        
    }
}
