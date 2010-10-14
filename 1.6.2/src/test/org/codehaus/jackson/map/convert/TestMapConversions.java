package org.codehaus.jackson.map.convert;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.TypeReference;

public class TestMapConversions
    extends org.codehaus.jackson.map.BaseMapTest
{
    final ObjectMapper mapper = new ObjectMapper();

    enum AB { A, B; }

    static class Bean {
        public Integer A;
        public String B;
    }
    
    /**
     * Test that verifies that we can go between couple of types of Maps...
     */
    public void testMapToMap()
    {
        Map<String,Integer> input = new LinkedHashMap<String,Integer>();
        input.put("A", Integer.valueOf(3));
        input.put("B", Integer.valueOf(-4));
        Map<AB,String> output = mapper.convertValue(input,
                new TypeReference<Map<AB,String>>() { });
        assertEquals(2, output.size());
        assertEquals("3", output.get(AB.A));
        assertEquals("-4", output.get(AB.B));

        // Let's try the other way too... and mix up types a bit
        Map<String,Integer> roundtrip = mapper.convertValue(input,
                new TypeReference<TreeMap<String,Integer>>() { });
        assertEquals(2, roundtrip.size());
        assertEquals(Integer.valueOf(3), roundtrip.get("A"));
        assertEquals(Integer.valueOf(-4), roundtrip.get("B"));
    }

    public void testMapToBean()
    {
        EnumMap<AB,String> map = new EnumMap<AB,String>(AB.class);
        map.put(AB.A, "   17");
        map.put(AB.B, " -1");
        Bean bean = mapper.convertValue(map, Bean.class);
        assertEquals(Integer.valueOf(17), bean.A);
        assertEquals(" -1", bean.B);
    }

    public void testBeanToMap()
    {
        Bean bean = new Bean();
        bean.A = 129;
        bean.B = "13";
        EnumMap<AB,String> result = mapper.convertValue(bean,
                new TypeReference<EnumMap<AB,String>>() { });
        assertEquals("129", result.get(AB.A));
        assertEquals("13", result.get(AB.B));
    }
}
