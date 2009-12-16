package org.codehaus.jackson.map.deser;

import main.BaseTest;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JsonDeserialize;

public class TestEnumDeserialization
    extends BaseTest
{
    /*
    //////////////////////////////////////////////////////////
    // Helper classes, enums
    //////////////////////////////////////////////////////////
     */

    enum TestEnum { JACKSON, RULES, OK; }

    /**
     * Alternative version that annotates which deserializer to use
     */
    @JsonDeserialize(using=DummySerializer.class)
    enum AnnotatedTestEnum {
        JACKSON, RULES, OK;
    }

    public static class DummySerializer extends JsonDeserializer<Object>
    {
        public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        {
            return AnnotatedTestEnum.OK;
        }
    }

    /*
    //////////////////////////////////////////////////////////
    // Tests
    //////////////////////////////////////////////////////////
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

    // Test [WSTX-214]
    public void testSubclassedEnums() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        EnumWithSubclass value = mapper.readValue("\"A\"", EnumWithSubclass.class);
        assertEquals(EnumWithSubClass.A, value);
    }
}
