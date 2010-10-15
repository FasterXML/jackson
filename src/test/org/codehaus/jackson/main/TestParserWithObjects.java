package org.codehaus.jackson.main;

import java.io.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.MappingJsonFactory;

/**
 * Unit tests for verifying that object mapping functionality can
 * be accessed using JsonParser.
 */
public class TestParserWithObjects
    extends main.BaseTest
{
    final static class Pojo
    {
        int _x;

        public void setX(int x) { _x = x; }
    }

    /*
    /**********************************************************
    /* Test for simple traversal with data mapping
    /**********************************************************
     */

    public void testNextValue() throws IOException
    {
        // Let's test both byte-backed and Reader-based one
        _testNextValueBasic(false);
        _testNextValueBasic(true);
    }

    // [JACKSON-395]
    public void testNextValueNested() throws IOException
    {
        // Let's test both byte-backed and Reader-based one
        _testNextValueNested(false);
        _testNextValueNested(true);
    }
    
    public void testPojoReading() throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        final String JSON = "{ \"x\" : 9 }";
        JsonParser jp = jf.createJsonParser(new StringReader(JSON));

        // let's try first by advancing:
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        Pojo p = jp.readValueAs(Pojo.class);
        assertEquals(9, p._x);
        jp.close();

        // and without
        jp = jf.createJsonParser(new StringReader(JSON));
        p = jp.readValueAs(Pojo.class);
        assertEquals(9, p._x);
        jp.close();
    }

    public void testReadingArrayAsTree() throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        final String JSON = "[ 1, 2, false ]";

        for (int i = 0; i < 2; ++i) {
            JsonParser jp = jf.createJsonParser(new StringReader(JSON));
            // whether to try advancing first or not? Try both
            if (i == 0) {
                assertToken(JsonToken.START_ARRAY, jp.nextToken());
            }
            JsonNode root = jp.readValueAsTree();
            jp.close();
            assertTrue(root.isArray());
            assertEquals(3, root.size());
            assertEquals(1, root.get(0).getIntValue());
            assertEquals(2, root.get(1).getIntValue());
            assertFalse(root.get(2).getBooleanValue());
        }
    }

    public void testIsClosed()
        throws IOException
    {
        for (int i = 0; i < 4; ++i) {
            String JSON = "[ 1, 2, 3 ]";
            boolean stream = ((i & 1) == 0);
            JsonParser jp = stream ?
                createParserUsingStream(JSON, "UTF-8")
                : createParserUsingReader(JSON);
            boolean partial = ((i & 2) == 0);

            assertFalse(jp.isClosed());
            assertToken(JsonToken.START_ARRAY, jp.nextToken());

            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertFalse(jp.isClosed());

            if (partial) {
                jp.close();
                assertTrue(jp.isClosed());
            } else {
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertToken(JsonToken.END_ARRAY, jp.nextToken());
                assertNull(jp.nextToken());
                assertTrue(jp.isClosed());
            }
        }
    }

    /*
    /**********************************************************
    /* Tests for data binding
    /**********************************************************
     */

    /**
     * Test similar to above, but instead reads a sequence of values
     */
    public void testIncrementalPojoReading()
        throws IOException
    {
        JsonFactory jf = new MappingJsonFactory();
        final String JSON = "[ 1, true, null, \"abc\" ]";
        JsonParser jp = jf.createJsonParser(new StringReader(JSON));

        // let's advance past array start to prevent full binding
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(Integer.valueOf(1), jp.readValueAs(Integer.class));
        assertEquals(Boolean.TRUE, jp.readValueAs(Boolean.class));
        /* note: null can be returned both when there is no more
         * data in current scope, AND when Json null literal is
         * bound!
         */
        assertNull(jp.readValueAs(Object.class));
        // but we can verify that it was Json null by:
        assertEquals(JsonToken.VALUE_NULL, jp.getLastClearedToken());

        assertEquals("abc", jp.readValueAs(String.class));

        // this null is for actually hitting the END_ARRAY
        assertNull(jp.readValueAs(Object.class));
        assertEquals(JsonToken.END_ARRAY, jp.getLastClearedToken());

        // afrer which there should be nothing to advance to:
        assertNull(jp.nextToken());

        jp.close();
    }

    public void testPojoReadingFailing()
        throws IOException
    {
        // regular factory can't do it, without a call to setCodec()
        JsonFactory jf = new JsonFactory();
        try {
            final String JSON = "{ \"x\" : 9 }";
            JsonParser jp = jf.createJsonParser(new StringReader(JSON));
            Pojo p = jp.readValueAs(Pojo.class);
            fail("Expected an exception: got "+p);
        } catch (IllegalStateException e) {
            verifyException(e, "No ObjectCodec defined");
        }
    }

    /*
    /**********************************************************
    /* Supporting methods
    /**********************************************************
     */

    private void  _testNextValueBasic(boolean useStream) throws IOException
    {
        // first array, no change to default
        JsonParser jp = _getParser("[ 1, 2, 3, 4 ]", useStream);
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        for (int i = 1; i <= 4; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
            assertEquals(i, jp.getIntValue());
        }
        assertToken(JsonToken.END_ARRAY, jp.nextValue());
        assertNull(jp.nextValue());
        jp.close();

        // then Object, is different
        jp = _getParser("{ \"3\" :3, \"4\": 4, \"5\" : 5 }", useStream);
        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        for (int i = 3; i <= 5; ++i) {
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
            assertEquals(String.valueOf(i), jp.getCurrentName());
            assertEquals(i, jp.getIntValue());
        }
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertNull(jp.nextValue());
        jp.close();

        // and then mixed...
        jp = _getParser("[ true, [ ], { \"a\" : 3 } ]", useStream);

        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertToken(JsonToken.VALUE_TRUE, jp.nextValue());
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());

        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());

        assertNull(jp.nextValue());
        jp.close();
    }

    // [JACKSON-395]
    private void  _testNextValueNested(boolean useStream) throws IOException
    {
        // first array, no change to default
        JsonParser jp;
    
        // then object with sub-objects...
        jp = _getParser("{\"a\": { \"b\" : true, \"c\": false }, \"d\": 3 }", useStream);

        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, jp.nextValue());
        assertEquals("b", jp.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, jp.nextValue());
        assertEquals("c", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        // ideally we should match closing marker with field, too:
        assertEquals("a", jp.getCurrentName());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextValue());
        assertEquals("d", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertNull(jp.nextValue());
        jp.close();

        // and arrays
        jp = _getParser("{\"a\": [ false ] }", useStream);

        assertToken(JsonToken.START_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.START_ARRAY, jp.nextValue());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.VALUE_FALSE, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertToken(JsonToken.END_ARRAY, jp.nextValue());
        // ideally we should match closing marker with field, too:
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.END_OBJECT, jp.nextValue());
        assertNull(jp.getCurrentName());
        assertNull(jp.nextValue());
        jp.close();
    }

    private JsonParser _getParser(String doc, boolean useStream)
        throws IOException
    {
        JsonFactory jf = new JsonFactory();
        if (useStream) {
            return jf.createJsonParser(doc.getBytes("UTF-8"));
        }
        return jf.createJsonParser(new StringReader(doc));
    }
}
