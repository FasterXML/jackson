package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * This unit test suite tests use of @JsonClass Annotation
 * with bean serialization.
 */
public class TestAnnotationJsonSerialize
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Annotated helper classes
    /**********************************************************
     */

    interface ValueInterface {
        public int getX();
    }

    static class ValueClass
        implements ValueInterface
    {
        @Override
        public int getX() { return 3; }
        public int getY() { return 5; }
    }

    /**
     * Test class to verify that <code>JsonSerialize.as</code>
     * works as expected
     */
    static class WrapperClassForAs
    {
        @JsonSerialize(as=ValueInterface.class)
        public ValueClass getValue() {
            return new ValueClass();
        }
    }

    // This should indicate that static type be used for all fields
    @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
    static class WrapperClassForStaticTyping
    {
        public ValueInterface getValue() {
            return new ValueClass();
        }
    }

    static class WrapperClassForStaticTyping2
    {
        @JsonSerialize(typing=JsonSerialize.Typing.STATIC)
        public ValueInterface getStaticValue() {
            return new ValueClass();
        }

        @JsonSerialize(typing=JsonSerialize.Typing.DYNAMIC)
        public ValueInterface getDynamicValue() {
            return new ValueClass();
        }
    }

    /**
     * Test bean that has an invalid {@link JsonClass} annotation.
     */
    static class BrokenClass
    {
        // invalid annotation: String not a supertype of Long
        @JsonSerialize(as=String.class)
        public Long getValue() {
            return Long.valueOf(4L);
        }
    }

    @SuppressWarnings("serial")
    static class ValueMap extends HashMap<String,ValueInterface> { }
    @SuppressWarnings("serial")
    static class ValueList extends ArrayList<ValueInterface> { }
    @SuppressWarnings("serial")
    static class ValueLinkedList extends LinkedList<ValueInterface> { }
    
    // Classes for [JACKSON-294]
    @SuppressWarnings("unused")
    static class Foo294
    {
        @JsonProperty private String id;
        @JsonSerialize(using = Bar294Serializer.class)
        private Bar294 bar;

        public Foo294() { }
        public Foo294(String id, String id2) {
            this.id = id;
            bar = new Bar294(id2);
        }
    }

    static class Bar294{
        @JsonProperty private String id;
        @JsonProperty private String name;

        public Bar294() { }
        public Bar294(String id) {
            this.id = id;
        }

        public String getId() { return id; }
        public String getName() { return name; }
    }

    static class Bar294Serializer extends JsonSerializer<Bar294>
    {
        @Override
        public void serialize(Bar294 bar, JsonGenerator jgen,
            SerializerProvider provider) throws IOException
        {
            jgen.writeString(bar.id);
        }
    }

    /*
    /**********************************************************
    /* Main tests
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    public void testSimpleValueDefinition() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new WrapperClassForAs());
        assertEquals(1, result.size());
        Object ob = result.get("value");
        // Should see only "x", not "y"
        result = (Map<String,Object>) ob;
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }

    public void testBrokenAnnotation() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        try {
            serializeAsString(m, new BrokenClass());
        } catch (Exception e) {
            verifyException(e, "not a super-type of");
        }
    }

    @SuppressWarnings("unchecked")
    public void testStaticTypingForClass() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new WrapperClassForStaticTyping());
        assertEquals(1, result.size());
        Object ob = result.get("value");
        // Should see only "x", not "y"
        result = (Map<String,Object>) ob;
        assertEquals(1, result.size());
        assertEquals(Integer.valueOf(3), result.get("x"));
    }

    @SuppressWarnings("unchecked")
    public void testMixedTypingForClass() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        Map<String,Object> result = writeAndMap(m, new WrapperClassForStaticTyping2());
        assertEquals(2, result.size());

        Object obStatic = result.get("staticValue");
        // Should see only "x", not "y"
        Map<String,Object> stat = (Map<String,Object>) obStatic;
        assertEquals(1, stat.size());
        assertEquals(Integer.valueOf(3), stat.get("x"));

        Object obDynamic = result.get("dynamicValue");
        // Should see both
        Map<String,Object> dyn = (Map<String,Object>) obDynamic;
        assertEquals(2, dyn.size());
        assertEquals(Integer.valueOf(3), dyn.get("x"));
        assertEquals(Integer.valueOf(5), dyn.get("y"));
    }

    public void testStaticTypingWithMap() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        ValueMap map = new ValueMap();
        map.put("a", new ValueClass());
        assertEquals("{\"a\":{\"x\":3}}", serializeAsString(m, map));
    }

    public void testStaticTypingWithArrayList() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        ValueList list = new ValueList();
        list.add(new ValueClass());
        assertEquals("[{\"x\":3}]", m.writeValueAsString(list));
    }

    public void testStaticTypingWithLinkedList() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        ValueLinkedList list = new ValueLinkedList();
        list.add(new ValueClass());
        assertEquals("[{\"x\":3}]", serializeAsString(m, list));
    }
    
    public void testStaticTypingWithArray() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        m.configure(SerializationConfig.Feature.USE_STATIC_TYPING, true);
        ValueInterface[] array = new ValueInterface[] { new ValueClass() };
        assertEquals("[{\"x\":3}]", serializeAsString(m, array));
    }

    public void testProblem294() throws Exception
    {
        assertEquals("{\"id\":\"fooId\",\"bar\":\"barId\"}",
                new ObjectMapper().writeValueAsString(new Foo294("fooId", "barId")));
    }
    
}
