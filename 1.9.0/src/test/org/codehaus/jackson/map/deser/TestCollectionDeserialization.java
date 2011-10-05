package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.deser.std.StdDeserializer;
import org.codehaus.jackson.type.TypeReference;

public class TestCollectionDeserialization
    extends BaseMapTest
{
    enum Key {
        KEY1, KEY2, WHATEVER;
    }

    @SuppressWarnings("serial")
    @JsonDeserialize(using=ListDeserializer.class)
    static class CustomList extends LinkedList<String> { }

    static class ListDeserializer extends StdDeserializer<CustomList>
    {
        public ListDeserializer() { super(CustomList.class); }

        @Override
        public CustomList deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException
        {
            CustomList result = new CustomList();
            result.add(jp.getText());
            return result;
        }
    }

    static class XBean {
        public int x;
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */
    
    public void testUntypedList() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get "untyped" default List, pass Object.class
        String JSON = "[ \"text!\", true, null, 23 ]";

        /* Not a guaranteed cast theoretically, but will work:
         * (since we know that Jackson will construct an ArrayList here...)
         */
        Object value = mapper.readValue(JSON, Object.class);
        assertNotNull(value);
        assertTrue(value instanceof ArrayList<?>);
        List<?> result = (List<?>) value;

        assertEquals(4, result.size());

        assertEquals("text!", result.get(0));
        assertEquals(Boolean.TRUE, result.get(1));
        assertNull(result.get(2));
        assertEquals(Integer.valueOf(23), result.get(3));
    }

    public void testExactStringCollection() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        // to get typing, must use type reference
        String JSON = "[ \"a\", \"b\" ]";
        List<String> result = mapper.readValue(JSON, new TypeReference<ArrayList<String>>() { });

        assertNotNull(result);
        assertEquals(ArrayList.class, result.getClass());
        assertEquals(2, result.size());

        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    public void testHashSet() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String JSON = "[ \"KEY1\", \"KEY2\" ]";

        EnumSet<Key> result = mapper.readValue(JSON, new TypeReference<EnumSet<Key>>() { });
        assertNotNull(result);
        assertTrue(EnumSet.class.isAssignableFrom(result.getClass()));
        assertEquals(2, result.size());

        assertTrue(result.contains(Key.KEY1));
        assertTrue(result.contains(Key.KEY2));
        assertFalse(result.contains(Key.WHATEVER));
    }

    /**
     * Test to verify that @JsonDeserialize.using works as expected
     */
    public void testCustomDeserializer() throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        CustomList result = mapper.readValue(quote("abc"), CustomList.class);
        assertEquals(1, result.size());
        assertEquals("abc", result.get(0));
    }

    /* Testing [JACKSON-526], "implicit JSON array" for single-element arrays,
     * mostly produced by Jettison, Badgerfish conversions (from XML)
     */
    @SuppressWarnings("unchecked")
    public void testImplicitArrays() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        // first with simple scalar types (numbers), with collections
        List<Integer> ints = mapper.readValue("4", List.class);
        assertEquals(1, ints.size());
        assertEquals(Integer.valueOf(4), ints.get(0));
        List<String> strings = mapper.readValue(quote("abc"), new TypeReference<ArrayList<String>>() { });
        assertEquals(1, strings.size());
        assertEquals("abc", strings.get(0));
        // and arrays:
        int[] intArray = mapper.readValue("-7", int[].class);
        assertEquals(1, intArray.length);
        assertEquals(-7, intArray[0]);
        String[] stringArray = mapper.readValue(quote("xyz"), String[].class);
        assertEquals(1, stringArray.length);
        assertEquals("xyz", stringArray[0]);

        // and then with Beans:
        List<XBean> xbeanList = mapper.readValue("{\"x\":4}", new TypeReference<List<XBean>>() { });
        assertEquals(1, xbeanList.size());
        assertEquals(XBean.class, xbeanList.get(0).getClass());

        Object ob = mapper.readValue("{\"x\":29}", XBean[].class);
        XBean[] xbeanArray = (XBean[]) ob;
        assertEquals(1, xbeanArray.length);
        assertEquals(XBean.class, xbeanArray[0].getClass());
    }

    // [JACKSON-620]: allow "" to mean 'null' for Maps
    public void testFromEmptyString() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(DeserializationConfig.Feature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        List<?> result = m.readValue(quote(""), List.class);
        assertNull(result);
    }
}
