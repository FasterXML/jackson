package org.codehaus.jackson.jaxrs;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * Unit test to check [JACKSON-540]
 */
public class TestCanSerialize extends main.BaseTest
{
    static class Simple {
        protected List<String> list;

        public List<String> getList( ) { return list; }
        public void setList(List<String> l) { list = l; }
    }

    public void testCanSerialize() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.WRAPPER_ARRAY);
    
        // construct test object
        List<String> l = new ArrayList<String>();
        l.add("foo");
        l.add("bar");
    
        Simple s = new Simple();
        s.setList(l);

        // this is fine:
        boolean can = mapper.canSerialize(Simple.class);
        assertTrue(can);

        // but with problem of [JACKSON-540], we get nasty surprise here...
        String json = mapper.writeValueAsString(s);
        
        Simple result = mapper.readValue(json, Simple.class);
        assertNotNull(result.list);
        assertEquals(2, result.list.size());
    }
}
