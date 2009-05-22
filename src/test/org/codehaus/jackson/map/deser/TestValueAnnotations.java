package org.codehaus.jackson.map.deser;

import main.BaseTest;

import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

/**
 * This unit test suite tests use of "value" Annotations;
 * annotations that define actual type (Class) to use for
 * deserialization.
 */
public class TestValueAnnotations
    extends BaseTest
{
    /*
    ///////////////////////////////////////////////////
    // Annotated helper classes for @JsonDeserialize#as
    ///////////////////////////////////////////////////
     */

    /* Class for testing valid {@link JsonDeserialize} annotation
     * with 'as' parameter to define concrete class to deserialize to
     */
    final static class CollectionHolder
    {
        Collection<String> _strings;

        /* Default for 'Collection' would probably be ArrayList or so;
         * let's try to make it a TreeSet instead.
         */
        @JsonDeserialize(as=TreeSet.class)
        public void setStrings(Collection<String> s)
        {
            _strings = s;
        }
    }

    /* Another class for testing valid {@link JsonDeserialize} annotation
     * with 'as' parameter to define concrete class to deserialize to
     */
    final static class MapHolder
    {
        // Let's also coerce numbers into Strings here
        Map<String,String> _data;

        /* Default for 'Collection' would be HashMap,
         * let's try to make it a TreeMap instead.
         */
        @JsonDeserialize(as=TreeMap.class)
        public void setStrings(Map<String,String> s)
        {
            _data = s;
        }
    }

    /* Another class for testing valid {@link JsonDeserialize} annotation
     * with 'as' parameter, but with array
     */
    final static class ArrayHolder
    {
        String[] _strings;

        @JsonDeserialize(as=String[].class)
        public void setStrings(Object[] o)
        {
            // should be passed instances of proper type, as per annotation
            _strings = (String[]) o;
        }
    }

    /* Another class for testing broken {@link JsonDeserialize} annotation
     * with 'as' parameter; one with incompatible type
     */
    final static class BrokenCollectionHolder
    {
        @JsonDeserialize(as=String.class) // not assignable to Collection
        public void setStrings(Collection<String> s) { }
    }

    /*
    //////////////////////////////////////////////////////
    // Annotated helper classes for @JsonDeserialize.keyAs
    //////////////////////////////////////////////////////
     */

    final static class StringWrapper
    {
        final String _string;

        public StringWrapper(String s) { _string = s; }
    }

    final static class MapKeyHolder
    {
        Map<Object, String> _map;

        @JsonDeserialize(keyAs=StringWrapper.class)
        public void setMap(Map<Object,String> m)
        {
            // type should be ok, but no need to cast here (won't matter)
            _map = m;
        }
    }

    final static class BrokenMapKeyHolder
    {
        // Invalid: Integer not a sub-class of String
        @JsonDeserialize(keyAs=Integer.class)
            public void setStrings(Map<String,String> m) { }
    }

    /*
    ///////////////////////////////////////////////////////////
    // Annotated helper classes for @JsonDeserialize#contentAs
    ///////////////////////////////////////////////////////////
     */

    final static class ListContentHolder
    {
        List<?> _list;

        @JsonDeserialize(contentAs=StringWrapper.class)
        public void setList(List<?> l) {
            _list = l;
        }
    }

    final static class InvalidContentClass
    {
        /* Such annotation not allowed, since it makes no sense;
         * non-container classes have no contents to annotate (but
         * note that it is possible to first use @JsonDesiarialize.as
         * to mark Object as, say, a List, and THEN use
         * @JsonDeserialize.contentAs!)
         */
        @JsonDeserialize(contentAs=String.class)
            public void setValue(Object x) { }
    }

    final static class ArrayContentHolder
    {
        Object[] _data;

        @JsonDeserialize(contentAs=Long.class)
        public void setData(Object[] o)
        { // should have proper type, but no need to coerce here
            _data = o;
        }
    }

    final static class MapContentHolder
    {
        Map<Object,Object> _map;

        @JsonDeserialize(contentAs=Integer.class)
        public void setMap(Map<Object,Object> m)
        {
            _map = m;
        }
    }

    /*
    //////////////////////////////////////////////
    // Test methods for @JsonClass
    //////////////////////////////////////////////
     */

    public void testOverrideClassValid() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        CollectionHolder result = m.readValue
            ("{ \"strings\" : [ \"test\" ] }", CollectionHolder.class);

        Collection<String> strs = result._strings;
        assertEquals(1, strs.size());
        assertEquals(TreeSet.class, strs.getClass());
        assertEquals("test", strs.iterator().next());
    }

    public void testOverrideMapValid() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // note: expecting conversion from number to String, as well
        MapHolder result = m.readValue
            ("{ \"strings\" :  { \"a\" : 3 } }", MapHolder.class);

        Map<String,String> strs = result._data;
        assertEquals(1, strs.size());
        assertEquals(TreeMap.class, strs.getClass());
        String value = strs.get("a");
        assertEquals("3", value);
    }

    public void testOverrideArrayClass() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        ArrayHolder result = m.readValue
            ("{ \"strings\" : [ \"test\" ] }", ArrayHolder.class);

        String[] strs = result._strings;
        assertEquals(1, strs.length);
        assertEquals(String[].class, strs.getClass());
        assertEquals("test", strs[0]);
    }

    public void testOverrideClassInvalid() throws Exception
    {
        // should fail due to incompatible Annotation
        try {
            BrokenCollectionHolder result = new ObjectMapper().readValue
                ("{ \"strings\" : [ ] }", BrokenCollectionHolder.class);
            fail("Expected a failure, but got results: "+result);
        } catch (JsonMappingException jme) {
            verifyException(jme, "is not assignable to");
        }
    }

    /*
    //////////////////////////////////////////////
    // Test methods for @JsonDeserialize#keyAs
    //////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
	public void testOverrideKeyClassValid() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MapKeyHolder result = m.readValue("{ \"map\" : { \"xxx\" : \"yyy\" } }", MapKeyHolder.class);
        Map<StringWrapper, String> map = (Map<StringWrapper,String>)(Map<?,?>)result._map;
        assertEquals(1, map.size());
        Map.Entry<StringWrapper, String> en = map.entrySet().iterator().next();

        StringWrapper key = en.getKey();
        assertEquals(StringWrapper.class, key.getClass());
        assertEquals("xxx", key._string);
        assertEquals("yyy", en.getValue());
    }

    public void testOverrideKeyClassInvalid() throws Exception
    {
        // should fail due to incompatible Annotation
        try {
            BrokenMapKeyHolder result = new ObjectMapper().readValue
                ("{ \"123\" : \"xxx\" }", BrokenMapKeyHolder.class);
            fail("Expected a failure, but got results: "+result);
        } catch (JsonMappingException jme) {
            verifyException(jme, "is not assignable to");
        }
    }

    /*
    //////////////////////////////////////////////
    // Test methods for @JsonDeserialize#contentAs
    //////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
	public void testOverrideContentClassValid() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        ListContentHolder result = m.readValue("{ \"list\" : [ \"abc\" ] }", ListContentHolder.class);
        List<StringWrapper> list = (List<StringWrapper>)result._list;
        assertEquals(1, list.size());
        Object value = list.get(0);
        assertEquals(StringWrapper.class, value.getClass());
        assertEquals("abc", ((StringWrapper) value)._string);
    }

    /**
     * This test checks that @JsonDeserialize#contentAs is not used
     * for non-container
     * types; those make no sense, and can be detected easily.
     */
    public void testContentClassValid() throws Exception
    {
        // should fail due to incompatible Annotation
        try {
            InvalidContentClass result = new ObjectMapper().readValue
                ("{ \"value\" : \"x\" }", InvalidContentClass.class);
            fail("Expected a failure, but got results: "+result);
        } catch (JsonMappingException jme) {
            verifyException(jme, "Illegal content-type annotation");
        }
    }

    public void testOverrideArrayContents() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        ArrayContentHolder result = m.readValue("{ \"data\" : [ 1, 2, 3 ] }",
                                                ArrayContentHolder.class);
        Object[] data = result._data;
        assertEquals(3, data.length);
        assertEquals(Long[].class, data.getClass());
        assertEquals(1L, data[0]);
        assertEquals(2L, data[1]);
        assertEquals(3L, data[2]);
    }

    public void testOverrideMapContents() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        MapContentHolder result = m.readValue("{ \"map\" : { \"a\" : 9 } }",
                                                MapContentHolder.class);
        Map<Object,Object> map = result._map;
        assertEquals(1, map.size());
        Object ob = map.values().iterator().next();
        assertEquals(Integer.class, ob.getClass());
        assertEquals(Integer.valueOf(9), ob);
    }
}
