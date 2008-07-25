package map;

import main.BaseTest;

import java.io.*;
import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tries to verify that the "Native" java type
 * mapper can properly serialize Java core objects to JSON.
 */
public class TestFromJavaType
    extends BaseTest
{
    public void testFromArray()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);

        ArrayList<Object> doc = new ArrayList<Object>();
        doc.add("Elem1");
        doc.add(Integer.valueOf(3));
        Map<String,Object> struct = new LinkedHashMap<String, Object>();
        struct.put("first", Boolean.TRUE);
        struct.put("Second", new ArrayList<Object>());
        doc.add(struct);
        doc.add(Boolean.FALSE);

        new JavaTypeMapper().writeAny(gen, doc);
        gen.close();

        JsonParser jp = new JsonFactory().createJsonParser(new StringReader(sw.toString()));

        assertEquals(JsonToken.START_ARRAY, jp.nextToken());

        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("Elem1", getAndVerifyText(jp));

        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(3, jp.getIntValue());

        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("first", getAndVerifyText(jp));

        assertEquals(JsonToken.VALUE_TRUE, jp.nextToken());
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("Second", getAndVerifyText(jp));

        assertEquals(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        assertEquals(JsonToken.END_OBJECT, jp.nextToken());

        assertEquals(JsonToken.VALUE_FALSE, jp.nextToken());

        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
    }

    public void testFromMap()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);

        LinkedHashMap<String,Object> doc = new LinkedHashMap<String,Object>();

        doc.put("a1", "\"text\"");
        doc.put("int", Integer.valueOf(137));
        doc.put("foo bar", Long.valueOf(1234567890L));

        new JavaTypeMapper().writeAny(gen, doc);
        gen.close();

        JsonParser jp = new JsonFactory().createJsonParser(new StringReader(sw.toString()));

        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("a1", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("\"text\"", getAndVerifyText(jp));

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("int", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(137, jp.getIntValue());

        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("foo bar", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1234567890L, jp.getLongValue());

        assertEquals(JsonToken.END_OBJECT, jp.nextToken());

        assertNull(jp.nextToken());
    }

    /**
     * Unit test to catch bug [JACKSON-x].
     */
    public void testBigDecimal()
        throws Exception
    {
        StringWriter sw = new StringWriter();
        JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);

        Map<String, Object> map = new HashMap<String, Object>();
        String PI_STR = "3.14159265";
        map.put("pi", new BigDecimal(PI_STR));
        new JavaTypeMapper().writeAny(gen, map);
        gen.close();

        assertEquals("{\"pi\":3.14159265}", sw.toString());
    }
}
