package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.*;

import java.util.*;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.type.TypeReference;

public class TestGenericMapDeser
    extends BaseMapTest
{
    /*
    ***************************************************
    * Test classes, enums
    ***************************************************
     */


    static class BooleanWrapper {
        final Boolean b;

        @JsonCreator BooleanWrapper(Boolean value) {
            b = value;
        }
    }

    static class StringWrapper {
        final String str;
        @JsonCreator StringWrapper(String value) {
            str = value;
        }
    }

    static class MapSubClass extends HashMap<String,BooleanWrapper> { }

    /**
     * Map class that should behave like {@link MapSubClass}, but by
     * using annotations.
     */
    @JsonDeserialize(keyAs=StringWrapper.class, contentAs=BooleanWrapper.class)
        static class AnnotatedMap extends HashMap<Object,Object> { }

    /*
    ***************************************************
    * Test methods
    ***************************************************
     */
    /*
    ////////////////////////////////////////////////////////////
    // Tests for sub-classing
    ////////////////////////////////////////////////////////////
     */
    /**
     * Verifying that sub-classing works ok wrt generics information
     */
    public void testMapSubClass() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        MapSubClass result = mapper.readValue
            ("{\"a\":true }", MapSubClass.class);
        assertEquals(1, result.size());
        Object value = result.get("a");
        assertEquals(BooleanWrapper.class, value.getClass());
        BooleanWrapper bw = (BooleanWrapper) value;
        assertEquals(Boolean.TRUE, bw.b);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Tests for annotations
    ////////////////////////////////////////////////////////////
     */

    /**
     * Verifying that sub-classing works ok wrt generics information
     */
    public void testAnnotatedMap() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotatedMap result = mapper.readValue
            ("{\"a\":true }", AnnotatedMap.class);
        assertEquals(1, result.size());
        Map.Entry<Object,Object> en = result.entrySet().iterator().next();
        assertEquals(StringWrapper.class, en.getKey().getClass());
        assertEquals(BooleanWrapper.class, en.getValue().getClass());
        assertEquals("a", ((StringWrapper) en.getKey()).str);
        assertEquals(Boolean.TRUE, ((BooleanWrapper) en.getValue()).b);
    }

}
