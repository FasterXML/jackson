package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.map.*;

public class TestDefaultForObject
    extends BaseMapTest
{
    /*
     ****************************************************** 
     * Helper types
     ****************************************************** 
     */

    static class StringBean { // ha, punny!
        public String name;

        public StringBean() { this(null); }
        protected StringBean(String n)  { name = n; }
    }

    /*
     ****************************************************** 
     * Unit tests
     ****************************************************** 
     */
    
    public void testBeanAsObject() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.enableDefaultTyping();
        // note: need to wrap, to get declared as Object
        String str = m.writeValueAsString(new Object[] { new StringBean("abc") });

        verifySerializationAsMap(str);
        
        // Ok: serialization seems to work as expected. Now deserialize:
        //System.err.println("DEBUG: json = "+str);
        
        Object ob = m.readValue(str, Object[].class);
        assertNotNull(ob);
        Object[] result = (Object[]) ob;
        assertNotNull(result[0]);
        assertEquals(StringBean.class, result[0].getClass());
        assertEquals("abc", ((StringBean) result[0]).name);
    }

    @SuppressWarnings("unchecked")
    private void verifySerializationAsMap(String str) throws Exception
    {
        // First: validate that structure looks correct (as Map etc)
        // note: should look something like:
        // "[["org.codehaus.jackson.map.jsontype.TestDefaultForObject$StringBean",{"name":"abc"}]]")

        // note: must have default mapper, default typer NOT enabled (to get 'plain' map)
        ObjectMapper m = new ObjectMapper();
        List<Object> list = m.readValue(str, List.class);
        assertEquals(1, list.size()); // no type for main List, just single entry
        Object entryOb = list.get(0);
        assertTrue(entryOb instanceof List<?>);
        // but then type wrapper for bean
        List<?> entryList = (List<?>)entryOb;
        assertEquals(2, entryList.size());
        assertEquals(StringBean.class.getName(), entryList.get(0));
        assertTrue(entryList.get(1) instanceof Map);
        Map map = (Map) entryList.get(1);
        assertEquals(1, map.size());
        assertEquals("abc", map.get("name"));
    }
}
