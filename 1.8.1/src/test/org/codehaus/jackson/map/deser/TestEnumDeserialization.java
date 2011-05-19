package org.codehaus.jackson.map.deser;

import java.util.EnumMap;
import java.util.concurrent.TimeUnit;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonCreator;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.type.TypeReference;

public class TestEnumDeserialization
    extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper classes, enums
    /**********************************************************
     */

    enum TestEnum { JACKSON, RULES, OK; }

    /**
     * Alternative version that annotates which deserializer to use
     */
    @JsonDeserialize(using=DummySerializer.class)
    enum AnnotatedTestEnum {
        JACKSON, RULES, OK;
    }

    public static class DummySerializer extends StdDeserializer<Object>
    {
        public DummySerializer() { super(Object.class); }
        @Override
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        {
            return AnnotatedTestEnum.OK;
        }
    }

    protected enum EnumWithCreator {
        A, B;

        @JsonCreator
        public static EnumWithCreator fromEnum(String str) {
            if ("enumA".equals(str)) return A;
            if ("enumB".equals(str)) return B;
            return null;
        }
    }

    protected enum LowerCaseEnum {
        A, B, C;
        private LowerCaseEnum() { }
        @Override
        public String toString() { return name().toLowerCase(); }
    }
    
    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    public void testSimple() throws Exception
    {
        // First "good" case with Strings
        ObjectMapper mapper = new ObjectMapper();
        String JSON = "\"OK\" \"RULES\"  null";
        // multiple main-level mappings, need explicit parser:
        JsonParser jp = mapper.getJsonFactory().createJsonParser(JSON);

        assertEquals(TestEnum.OK, mapper.readValue(jp, TestEnum.class));
        assertEquals(TestEnum.RULES, mapper.readValue(jp, TestEnum.class));

        /* should be ok; nulls are typeless; handled by mapper, not by
         * deserializer
         */
        assertNull(mapper.readValue(jp, TestEnum.class));

        // and no more content beyond that...
        assertFalse(jp.hasCurrentToken());

        /* Then alternative with index (0 means first entry)
         */
        assertEquals(TestEnum.JACKSON, mapper.readValue(" 0 ", TestEnum.class));

        /* Then error case: unrecognized value
         */
        try {
            /*Object result =*/ mapper.readValue("\"NO-SUCH-VALUE\"", TestEnum.class);
            fail("Expected an exception for bogus enum value...");
        } catch (JsonMappingException jex) {
            verifyException(jex, "value not one of declared");
        }
    }

    /**
     * Enums are considered complex if they have code (and hence sub-classes)... an
     * example is TimeUnit
     */
    public void testComplexEnum() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(TimeUnit.HOURS);
        assertEquals(quote("HOURS"), json);
        TimeUnit result = mapper.readValue(json, TimeUnit.class);
        assertSame(TimeUnit.HOURS, result);
    }
    
    /**
     * Testing to see that annotation override works
     */
    public void testAnnotated() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        AnnotatedTestEnum e = mapper.readValue("\"JACKSON\"", AnnotatedTestEnum.class);
        /* dummy deser always returns value OK, independent of input;
         * only works if annotation is used
         */
        assertEquals(AnnotatedTestEnum.OK, e);
    }

    public void testEnumMaps() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        EnumMap<TestEnum,String> value = mapper.readValue("{\"OK\":\"value\"}",
                new TypeReference<EnumMap<TestEnum,String>>() { });
        assertEquals("value", value.get(TestEnum.OK));
    }
    
    // Test [JACKSON-214]
    public void testSubclassedEnums() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        EnumWithSubClass value = mapper.readValue("\"A\"", EnumWithSubClass.class);
        assertEquals(EnumWithSubClass.A, value);
    }

    // [JACKSON-193]
    public void testCreatorEnums() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        EnumWithCreator value = mapper.readValue("\"enumA\"", EnumWithCreator.class);
        assertEquals(EnumWithCreator.A, value);
    }
    
    // [JACKSON-212]
    public void testToStringEnums() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);
        LowerCaseEnum value = mapper.readValue("\"c\"", LowerCaseEnum.class);
        assertEquals(LowerCaseEnum.C, value);
    }

    // [JACKSON-212]
    public void testToStringEnumMaps() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.READ_ENUMS_USING_TO_STRING, true);
        EnumMap<LowerCaseEnum,String> value = mapper.readValue("{\"a\":\"value\"}",
                new TypeReference<EnumMap<LowerCaseEnum,String>>() { });
        assertEquals("value", value.get(LowerCaseEnum.A));
    }

    // [JACKSON-412], disallow use of numbers
    public void testNumbersToEnums() throws Exception
    {
        // by default numbers are fine:
        ObjectMapper mapper = new ObjectMapper();
        assertFalse(mapper.getDeserializationConfig().isEnabled(DeserializationConfig.Feature.FAIL_ON_NUMBERS_FOR_ENUMS));
        TestEnum value = mapper.readValue("1", TestEnum.class);
        assertSame(TestEnum.RULES, value);

        // but can also be changed to errors:
        mapper = new ObjectMapper();
        mapper.configure(DeserializationConfig.Feature.FAIL_ON_NUMBERS_FOR_ENUMS, true);
        try {
            value = mapper.readValue("1", TestEnum.class);
            fail("Expected an error");
        } catch (JsonMappingException e) {
            verifyException(e, "Not allowed to deserialize Enum value out of JSON number");
        }
    }
}
