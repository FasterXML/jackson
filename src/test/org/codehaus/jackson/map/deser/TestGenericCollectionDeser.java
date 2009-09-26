package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.*;

import java.util.*;

import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.type.TypeReference;

public class TestGenericCollectionDeser
    extends BaseMapTest
{
    /*
    ***************************************************
    * Test classes, enums
    ***************************************************
     */

    static class StringWrapper {
        final String str;
        @JsonCreator StringWrapper(String value) {
            str = value;
        }
    }

    static class ListSubClass extends ArrayList<StringWrapper> { }

    /**
     * Map class that should behave like {@link MapSubClass}, but by
     * using annotations.
     */
    @JsonDeserialize(contentAs=StringWrapper.class)
        static class AnnotatedList extends ArrayList<Object> { }

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
    public void testListSubClass() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        ListSubClass result = mapper.readValue("[ \"123\" ]", ListSubClass.class);
        assertEquals(1, result.size());
        Object value = result.get(0);
        assertEquals(StringWrapper.class, value.getClass());
        StringWrapper bw = (StringWrapper) value;
        assertEquals("123", bw.str);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Tests for annotations
    ////////////////////////////////////////////////////////////
     */

    /**
     * Verifying that sub-classing works ok wrt generics information
     */
    public void testAnnotatedList() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotatedList result = mapper.readValue("[ \"...\" ]", AnnotatedList.class);
        assertEquals(1, result.size());
        Object ob = result.get(0);
        assertEquals(StringWrapper.class, ob.getClass());
        assertEquals("...", ((StringWrapper) ob).str);
    }
}
