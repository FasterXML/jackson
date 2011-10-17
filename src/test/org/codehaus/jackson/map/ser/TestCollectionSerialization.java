package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

public class TestCollectionSerialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes
    /**********************************************************
     */

    enum Key { A, B, C };

    // Field-based simple bean with a single property, "values"
    final static class CollectionBean
    {
        @JsonProperty // not required
            public Collection<Object> values;

        public CollectionBean(Collection<Object> c) { values = c; }
    }

    static class EnumMapBean
    {
        EnumMap<Key,String> _map;

        public EnumMapBean(EnumMap<Key,String> m)
        {
            _map = m;
        }

        public EnumMap<Key,String> getMap() { return _map; }
    }

    final static class IterableWrapper
        implements Iterable<Integer>
    {
        List<Integer> _ints = new ArrayList<Integer>();

        public IterableWrapper(int[] values) {
            for (int i : values) {
                _ints.add(Integer.valueOf(i));
            }
        }

        @Override
        public Iterator<Integer> iterator() {
            return _ints.iterator();
        }
    }

    /**
     * Class needed for testing [JACKSON-220]
     */
    @SuppressWarnings("serial")
    @JsonSerialize(using=ListSerializer.class)    
    static class PseudoList extends ArrayList<String>
    {
        public PseudoList(String... values) {
            super(Arrays.asList(values));
        }
    }

    static class ListSerializer extends JsonSerializer<List<String>>
    {
        @Override
        public void serialize(List<String> value,
                              JsonGenerator jgen,
                              SerializerProvider provider)
            throws IOException
        {
            // just use standard List.toString(), output as JSON String
            jgen.writeString(value.toString());
        }
    }

    // for [JACKSON-254], suppression of empty collections
    static class EmptyListBean {
        public List<String> empty = new ArrayList<String>();
    }

    static class EmptyArrayBean {
        public String[] empty = new String[0];
    }

    // [JACKSIN-689]
    static class BeanWithIterable {
        private final ArrayList<String> values = new ArrayList<String>();
        {
            values.add("value");
        }

        public Iterable<String> getValues() { return values; }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testCollections()
        throws IOException
    {
        // Let's try different collections, arrays etc
        final int entryLen = 98;
        ObjectMapper mapper = new ObjectMapper();

        for (int type = 0; type < 4; ++type) {
            Object value;

            if (type == 0) { // first, array
                int[] ints = new int[entryLen];
                for (int i = 0; i < entryLen; ++i) {
                    ints[i] = Integer.valueOf(i);
                }
                value = ints;
            } else {
                Collection<Integer> c;

                switch (type) {
                case 1:
                    c = new LinkedList<Integer>();
                    break;
                case 2:
                    c = new TreeSet<Integer>(); // has to be ordered
                    break;
                default:
                    c = new ArrayList<Integer>();
                    break;
                }
                for (int i = 0; i < entryLen; ++i) {
                    c.add(Integer.valueOf(i));
                }
                value = c;
            }
            StringWriter sw = new StringWriter();
            mapper.writeValue(sw, value);
            
            // and then need to verify:
            JsonParser jp = new JsonFactory().createJsonParser(sw.toString());
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            for (int i = 0; i < entryLen; ++i) {
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
        }
    }

    public void testBigCollection()
        throws IOException
    {
        final int COUNT = 9999;
        ArrayList<Integer> value = new ArrayList<Integer>();
        for (int i = 0; i <= COUNT; ++i) {
            value.add(i);
        }
        ObjectMapper mapper = new ObjectMapper();
        // Let's test using 3 main variants...
        for (int mode = 0; mode < 3; ++mode) {
            JsonParser jp = null;
            switch (mode) {
            case 0:
                {
                    byte[] data = mapper.writeValueAsBytes(value);
                    jp = new JsonFactory().createJsonParser(data);
                }
                break;
            case 1:
                {
                    StringWriter sw = new StringWriter(value.size());
                    mapper.writeValue(sw, value);
                    jp = createParserUsingReader(sw.toString());
                }
                break;
            case 2:
                {
                    String str = mapper.writeValueAsString(value);
                    jp = createParserUsingReader(str);
                }
                break;
            }

            // and verify
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            for (int i = 0; i <= COUNT; ++i) {
                assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
            }
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }

    public void testEnumMap()
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        EnumMap<Key,String> map = new EnumMap<Key,String>(Key.class);
        map.put(Key.B, "xyz");
        map.put(Key.C, "abc");
        // assuming EnumMap uses enum entry order, which I think is true...
        mapper.writeValue(sw, map);
        assertEquals("{\"B\":\"xyz\",\"C\":\"abc\"}", sw.toString().trim());
    }

    public void testIterator()
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        ArrayList<Integer> l = new ArrayList<Integer>();
        l.add(1);
        l.add(-9);
        l.add(0);
        mapper.writeValue(sw, l.iterator());
        assertEquals("[1,-9,0]", sw.toString().trim());
    }

    public void testIterable()
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        StringWriter sw = new StringWriter();
        mapper.writeValue(sw, new IterableWrapper(new int[] { 1, 2, 3 }));
        assertEquals("[1,2,3]", sw.toString().trim());
    }

    // Test that checks that empty collections are properly serialized
    // when they are Bean properties
    @SuppressWarnings("unchecked")
    public void testEmptyBeanCollection()
        throws IOException
    {
        Collection<Object> x = new ArrayList<Object>();
        x.add("foobar");
        CollectionBean cb = new CollectionBean(x);
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, cb);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("values"));
        Collection<Object> x2 = (Collection<Object>) result.get("values");
        assertNotNull(x2);
        assertEquals(x, x2);
    }

    public void testNullBeanCollection()
        throws IOException
    {
        CollectionBean cb = new CollectionBean(null);
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, cb);
        assertEquals(1, result.size());
        assertTrue(result.containsKey("values"));
        assertNull(result.get("values"));
    }

    @SuppressWarnings("unchecked")
    public void testEmptyBeanEnumMap()
        throws IOException
    {
        EnumMap<Key,String> map = new EnumMap<Key,String>(Key.class);
        EnumMapBean b = new EnumMapBean(map);
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, b);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("map"));
        // we deserialized to untyped, not back to bean, so:
        Map<Object,Object> map2 = (Map<Object,Object>) result.get("map");
        assertNotNull(map2);
        assertEquals(0, map2.size());
    }

    // Should also be able to serialize null EnumMaps as expected
    public void testNullBeanEnumMap()
        throws IOException
    {
        EnumMapBean b = new EnumMapBean(null);
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, b);

        assertEquals(1, result.size());
        assertTrue(result.containsKey("map"));
        assertNull(result.get("map"));
    }

    // Test [JACKSON-220]
    public void testListSerializer() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("\"[ab, cd, ef]\"", m.writeValueAsString(new PseudoList("ab", "cd", "ef")));
    }

    // [JACKSON-254]
    public void testEmptyListOrArray() throws IOException
    {
        // by default, empty lists serialized normally
        EmptyListBean list = new EmptyListBean();
        EmptyArrayBean array = new EmptyArrayBean();
        ObjectMapper m = new ObjectMapper();
        assertTrue(m.getSerializationConfig().isEnabled(SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS));
        assertEquals("{\"empty\":[]}", m.writeValueAsString(list));
        assertEquals("{\"empty\":[]}", m.writeValueAsString(array));

        // note: value of setting may be cached when constructing serializer, need a new instance
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.WRITE_EMPTY_JSON_ARRAYS, false);
        assertEquals("{}", m.writeValueAsString(list));
        assertEquals("{}", m.writeValueAsString(array));
    }
    
    // [JACKSON-689]
    public void testWithIterable() throws IOException
    {
        ObjectMapper m = new ObjectMapper();
        assertEquals("{\"values\":[\"value\"]}", m.writeValueAsString(new BeanWithIterable()));
    }
}
