package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.TokenBuffer;

/**
 * Unit tests for those Jackson types we want to ensure can be deserialized.
 */
public class TestJacksonTypes
    extends org.codehaus.jackson.map.BaseMapTest
{
    public void testJsonLocation() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // note: source reference is untyped, only String guaranteed to work
        JsonLocation loc = new JsonLocation("whatever",  -1, -1, 100, 13);
        // Let's use serializer here; goal is round-tripping
        String ser = serializeAsString(m, loc);
        JsonLocation result = m.readValue(ser, JsonLocation.class);
        assertEquals("Did not correctly deserialize standard serialization '"+ser+"'",
                     loc, result);
    }

    // doesn't really belong here but...
    public void testJsonLocationProps()
    {
        JsonLocation loc = new JsonLocation(null,  -1, -1, 100, 13);
        assertTrue(loc.equals(loc));
        assertFalse(loc.equals(null));
        assertFalse(loc.equals("abx"));

        // should we check it's not 0?
        loc.hashCode();
    }

    /**
     * Verify that {@link TokenBuffer} can be properly deserialized
     * automatically, using the "standard" JSON sample document
     *
     * @since 1.5
     */
    public void testTokenBufferWithSample() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // First, try standard sample doc:
        TokenBuffer result = m.readValue(SAMPLE_DOC_JSON_SPEC, TokenBuffer.class);
        verifyJsonSpecSampleDoc(result.asParser(), true);
    }

    public void testTokenBufferWithSequence() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        // and then sequence of other things
        JsonParser jp = createParserUsingReader("[ 32, [ 1 ], \"abc\", { \"a\" : true } ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        TokenBuffer buf = m.readValue(jp, TokenBuffer.class);

        // check manually...
        JsonParser bufParser = buf.asParser();
        assertToken(JsonToken.VALUE_NUMBER_INT, bufParser.nextToken());
        assertEquals(32, bufParser.getIntValue());
        assertNull(bufParser.nextToken());

        // then bind to another
        buf = m.readValue(jp, TokenBuffer.class);
        bufParser = buf.asParser();
        assertToken(JsonToken.START_ARRAY, bufParser.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, bufParser.nextToken());
        assertEquals(1, bufParser.getIntValue());
        assertToken(JsonToken.END_ARRAY, bufParser.nextToken());
        assertNull(bufParser.nextToken());

        // third one, with automatic binding
        buf = m.readValue(jp, TokenBuffer.class);
        String str = m.readValue(buf.asParser(), String.class);
        assertEquals("abc", str);

        // and ditto for last one
        buf = m.readValue(jp, TokenBuffer.class);
        Map<?,?> map = m.readValue(buf.asParser(), Map.class);
        assertEquals(1, map.size());
        assertEquals(Boolean.TRUE, map.get("a"));
        
        assertEquals(JsonToken.END_ARRAY, jp.nextToken());
        assertNull(jp.nextToken());
    }
    
    // @since 1.9
    public void testJavaType() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        TypeFactory tf = TypeFactory.defaultInstance();
        // first simple type:
        String json = mapper.writeValueAsString(tf.constructType(String.class));
        assertEquals(quote(java.lang.String.class.getName()), json);
        // and back
        JavaType t = mapper.readValue(json, JavaType.class);
        assertNotNull(t);
        assertEquals(String.class, t.getRawClass());
    }
}
