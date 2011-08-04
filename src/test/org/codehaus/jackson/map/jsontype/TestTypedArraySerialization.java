package org.codehaus.jackson.map.jsontype;

import java.util.*;

import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonView;

/**
 * Unit tests for verifying that types that serialize as JSON Arrays
 * get properly serialized with types (esp. for contents, and
 * gracefully handling Lists themselves too)
 * 
 * @author tatus
 * @since 1.5
 */
public class TestTypedArraySerialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Let's claim we need type here too (although we won't
     * really use any sub-classes)
     */
    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_ARRAY)
    static class TypedList<T> extends ArrayList<T> { }

    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY)
    static class TypedListAsProp<T> extends ArrayList<T> { }
    
    @SuppressWarnings("serial")
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    static class TypedListAsWrapper<T> extends LinkedList<T> { }
    
    // Mix-in to force wrapper for things like primitive arrays
    @JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    interface WrapperMixIn { }

    // for [JACKSON-341]
    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.WRAPPER_OBJECT)
    @JsonSubTypes({ @JsonSubTypes.Type(B.class) })
    interface A { }

    @JsonTypeName("BB")
    static class B implements A {
        public int value = 2;
    }

    @JsonTypeInfo(use=JsonTypeInfo.Id.NAME, include=JsonTypeInfo.As.PROPERTY)
    @JsonTypeName("bean")
    static class Bean {
        public int x = 0;
    }

    static class BeanListWrapper {
        @JsonView({Object.class})
        public List<Bean> beans = new ArrayList<Bean>();
        {
            beans.add(new Bean());
        }
    }

    /*
    /**********************************************************
    /* Unit tests, Lists
    /**********************************************************
     */

    public void testListWithPolymorphic() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        BeanListWrapper beans = new BeanListWrapper();
        assertEquals("{\"beans\":[{\"@type\":\"bean\",\"x\":0}]}", mapper.writeValueAsString(beans));
        // Related to [JACKSON-364]
        ObjectWriter w = mapper.writerWithView(Object.class);
        assertEquals("{\"beans\":[{\"@type\":\"bean\",\"x\":0}]}", w.writeValueAsString(beans));
    }
    
    public void testIntList() throws Exception
    {
        TypedList<Integer> input = new TypedList<Integer>();
        input.add(5);
        input.add(13);
        // uses WRAPPER_ARRAY inclusion:
        assertEquals("[\""+TypedList.class.getName()+"\",[5,13]]", serializeAsString(input));
    }
    
    // Similar to above, but this time let's request adding type info
    // as property. That would not work (since there's no JSON Object to
    // add property in), so it should revert to method used with
    // ARRAY_WRAPPER method.
    public void testStringListAsProp() throws Exception
    {
        TypedListAsProp<String> input = new TypedListAsProp<String>();
        input.add("a");
        input.add("b");
        assertEquals("[\""+TypedListAsProp.class.getName()+"\",[\"a\",\"b\"]]",
                serializeAsString(input));
    }

    public void testStringListAsObjectWrapper() throws Exception
    {
        TypedListAsWrapper<Boolean> input = new TypedListAsWrapper<Boolean>();
        input.add(true);
        input.add(null);
        input.add(false);
        // Can wrap in JSON Object for wrapped style... also, will use
        // non-qualified class name as type name, since there are no
        // annotations
        String expName = "TestTypedArraySerialization$TypedListAsWrapper";
        assertEquals("{\""+expName+"\":[true,null,false]}",
                serializeAsString(input));
    }

    /*
    /**********************************************************
    /* Unit tests, primitive arrays
    /**********************************************************
     */

    public void testIntArray() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.getSerializationConfig().addMixInAnnotations(int[].class, WrapperMixIn.class);
        int[] input = new int[] { 1, 2, 3 };
        String clsName = int[].class.getName();
        assertEquals("{\""+clsName+"\":[1,2,3]}", serializeAsString(m, input));
    }

    /*
    /**********************************************************
    /* Unit tests, generic arrays
    /**********************************************************
     */

    public void testGenericArray() throws Exception
    {
        ObjectMapper m;
        final A[] input = new A[] { new B() };
        final String EXP = "[{\"BB\":{\"value\":2}}]";

        // first, with defaults
        m = new ObjectMapper();
        assertEquals(EXP, m.writeValueAsString(input));

        // then with static typing enabled:
        m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        assertEquals(EXP, m.writeValueAsString(input));
    }
}
