package org.codehaus.jackson.main;

import java.io.*;

import org.codehaus.jackson.*;

/**
 * Unit tests for verifying that additional {@link JsonParser.Feature}
 * settings work as expected.
 */
public class TestParserFeatures
    extends main.BaseTest
{
    public void testDefaultSettings()
    {
        JsonFactory f = new JsonFactory();
        assertTrue(f.isParserFeatureEnabled(JsonParser.Feature.AUTO_CLOSE_SOURCE));
        assertFalse(f.isParserFeatureEnabled(JsonParser.Feature.ALLOW_COMMENTS));
        assertFalse(f.isParserFeatureEnabled(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES));
    }

    public void testQuotesRequired() throws Exception
    {
        _testQuotesRequired(false);
        _testQuotesRequired(true);
    }

    // // // And then tests to verify [JACKSON-69]:

    public void testSimpleUnquoted() throws Exception
    {
        _testSimpleUnquoted(false);
        _testSimpleUnquoted(true);
    }

    public void testLargeUnquoted() throws Exception
    {
        _testLargeUnquoted(false);
        _testLargeUnquoted(true);
    }

    /*
    /////////////////////////////////////////////////////////////////
    // Secondary test methods
    /////////////////////////////////////////////////////////////////
     */

    private void _testQuotesRequired(boolean useStream) throws Exception
    {
        final String JSON = "{ test : 3 }";
        final String EXP_ERROR_FRAGMENT = "was expecting double-quote to start";
        JsonFactory f = new JsonFactory();
        JsonParser jp = useStream ?
            createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON)
            ;

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
        } catch (JsonParseException je) {
            verifyException(je, EXP_ERROR_FRAGMENT);
        }
    }

    private void _testSimpleUnquoted(boolean useStream) throws Exception
    {
        final String JSON = "{ a : 1, _foo:true, $:\"money!\", \" \":null }";
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        JsonParser jp = useStream ?
            createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON)
            ;

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("a", jp.getCurrentName());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("_foo", jp.getCurrentName());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("$", jp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("money!", jp.getText());

        // and then regular quoted one should still work too:
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals(" ", jp.getCurrentName());

        assertToken(JsonToken.VALUE_NULL, jp.nextToken());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
    }

    private void _testLargeUnquoted(boolean useStream) throws Exception
    {
        StringBuilder sb = new StringBuilder(5000);
        sb.append("[\n");
        //final int REPS = 2000;
        final int REPS = 1050;
        for (int i = 0; i < REPS; ++i) {
            if (i > 0) {
                sb.append(',');
                if ((i & 7) == 0) {
                    sb.append('\n');
                }
            }
            sb.append("{");
            sb.append("abc").append(i&127).append(':');
            sb.append((i & 1) != 0);
            sb.append("}\n");
        }
        sb.append("]");
        String JSON = sb.toString();
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        JsonParser jp = useStream ?
            createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON)
            ;
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        for (int i = 0; i < REPS; ++i) {
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals("abc"+(i&127), jp.getCurrentName());
            assertToken(((i&1) != 0) ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, jp.nextToken());
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }
}
