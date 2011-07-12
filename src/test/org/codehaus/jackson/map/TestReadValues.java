package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.*;

public class TestReadValues extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class Bean {
        public int a;
    }
    
    /*
    /**********************************************************
    /* Unit tests; root-level value sequences via Mapper
    /**********************************************************
     */

    public void testRootBeans() throws Exception
    {
        final String JSON = "{\"a\":3}{\"a\":27}  ";
        ObjectMapper mapper = new ObjectMapper();
        Iterator<Bean> it = mapper.reader(Bean.class).readValues(JSON);

        assertTrue(it.hasNext());
        Bean b = it.next();
        assertEquals(3, b.a);
        assertTrue(it.hasNext());
        b = it.next();
        assertEquals(27, b.a);
        assertFalse(it.hasNext());
    }

    public void testRootMaps() throws Exception
    {
        final String JSON = "{\"a\":3}{\"a\":27}  ";
        ObjectMapper mapper = new ObjectMapper();
        Iterator<Map<?,?>> it = mapper.reader(Map.class).readValues(JSON);

        assertTrue(it.hasNext());
        Map<?,?> map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertTrue(it.hasNext());
        map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(27), map.get("a"));
        assertFalse(it.hasNext());
    }

    public void testRootArrays() throws Exception
    {
        final String JSON = "[1][3]";
        ObjectMapper mapper = new ObjectMapper();
        Iterator<int[]> it = mapper.reader(int[].class).readValues(JSON);

        assertTrue(it.hasNext());
        int[] array = it.next();
        assertEquals(1, array.length);
        assertEquals(1, array[0]);
        assertTrue(it.hasNext());
        array = it.next();
        assertEquals(1, array.length);
        assertEquals(3, array[0]);
        assertFalse(it.hasNext());
    }

    /*
    /**********************************************************
    /* Unit tests; root-level value sequences via JsonParser
    /**********************************************************
     */

    public void testRootBeansWithParser() throws Exception
    {
        final String JSON = "{\"a\":3}{\"a\":27}  ";
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = mapper.getJsonFactory().createJsonParser(JSON);
        
        Iterator<Bean> it = jp.readValuesAs(Bean.class);

        assertTrue(it.hasNext());
        Bean b = it.next();
        assertEquals(3, b.a);
        assertTrue(it.hasNext());
        b = it.next();
        assertEquals(27, b.a);
        assertFalse(it.hasNext());
    }
    
    /*
    /**********************************************************
    /* Unit tests; non-root arrays
    /**********************************************************
     */

    public void testNonRootBeans() throws Exception
    {
        final String JSON = "{\"leaf\":[{\"a\":3},{\"a\":27}]}";
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = mapper.getJsonFactory().createJsonParser(JSON);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        
        Iterator<Bean> it = mapper.reader(Bean.class).readValues(jp);

        assertTrue(it.hasNext());
        Bean b = it.next();
        assertEquals(3, b.a);
        assertTrue(it.hasNext());
        b = it.next();
        assertEquals(27, b.a);
        assertFalse(it.hasNext());
        jp.close();
    }

    public void testNonRootMaps() throws Exception
    {
        final String JSON = "[{\"a\":3},{\"a\":27}]";
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = mapper.getJsonFactory().createJsonParser(JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        Iterator<Map<?,?>> it = mapper.reader(Map.class).readValues(jp);

        assertTrue(it.hasNext());
        Map<?,?> map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertTrue(it.hasNext());
        map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(27), map.get("a"));
        assertFalse(it.hasNext());
        jp.close();
    }

    public void testNonRootArrays() throws Exception
    {
        final String JSON = "[[1],[3]]";
        ObjectMapper mapper = new ObjectMapper();
        JsonParser jp = mapper.getJsonFactory().createJsonParser(JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        Iterator<int[]> it = mapper.readValues(jp, int[].class);

        assertTrue(it.hasNext());
        int[] array = it.next();
        assertEquals(1, array.length);
        assertEquals(1, array[0]);
        assertTrue(it.hasNext());
        array = it.next();
        assertEquals(1, array.length);
        assertEquals(3, array[0]);
        assertFalse(it.hasNext());
        jp.close();
    }
}
