package org.codehaus.jackson.impl;

import org.codehaus.jackson.*;

public class TestParserNonStandard
    extends main.BaseTest
{
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

    public void testSingleQuotesDefault() throws Exception
    {
        _testSingleQuotesDefault(false);
        _testSingleQuotesDefault(true);
    }

    public void testSingleQuotesEnabled() throws Exception
    {
        _testSingleQuotesEnabled(false);
        _testSingleQuotesEnabled(true);
    }

    // Test for [JACKSON-267], allowing '@' as name char, for unquoted names
    public void testNonStandardNameChars() throws Exception
    {
        _testNonStandardNameChars(false);
        _testNonStandardNameChars(true);
    }
    
    // Test for [JACKSON-300]
    public void testNonStandardAnyCharQuoting() throws Exception
    {
        _testNonStandarBackslashQuoting(false);
        _testNonStandarBackslashQuoting(true);
    }

    // Test for [JACKSON-358]
    public void testLeadingZeroesUTF8() throws Exception {
        _testLeadingZeroes(true);
    }

    public void testLeadingZeroesReader() throws Exception {
        _testLeadingZeroes(false);
    }
    
    /*
    /****************************************************************
    /* Secondary test methods
    /****************************************************************
     */

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

    /**
     * Test to verify that the default parser settings do not
     * accept single-quotes for String values (field names,
     * textual values)
     */
    private void _testSingleQuotesDefault(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        // First, let's see that by default they are not allowed
        String JSON = "[ 'text' ]";
        JsonParser jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        }

        JSON = "{ 'a':1 }";
        jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expected exception");
        } catch (JsonParseException e) {
            verifyException(e, "Unexpected character ('''");
        }
    }

    /**
     * Test to verify [JACKSON-173], optional handling of
     * single quotes, to allow handling invalid (but, alas, common)
     * JSON.
     */
    private void _testSingleQuotesEnabled(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);

        String JSON = "{ 'a' : 1, \"foobar\": 'b', '_abcde1234':'d', '\"' : '\"\"', '':'' }";
        JsonParser jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")
            : createParserUsingReader(f, JSON);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("a", jp.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals("1", jp.getText());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("foobar", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("b", jp.getText());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("_abcde1234", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("d", jp.getText());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("\"", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        //assertEquals("\"\"", jp.getText());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("", jp.getText());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
    }
    
    private void _testNonStandardNameChars(boolean useStream) throws Exception
    {
        JsonFactory f = new JsonFactory();
        f.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
        String JSON = "{ @type : \"mytype\", #color : 123, *error* : true, "
            +" hyphen-ated : \"yes\", me+my : null"
            +"}";
        JsonParser jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")
                : createParserUsingReader(f, JSON);

        assertToken(JsonToken.START_OBJECT, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("@type", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("mytype", jp.getText());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("#color", jp.getText());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(123, jp.getIntValue());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("*error*", jp.getText());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("hyphen-ated", jp.getText());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("yes", jp.getText());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("me+my", jp.getText());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
    
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.close();
    }

    private void _testNonStandarBackslashQuoting(boolean useStream) throws Exception
    {
        // first: verify that we get an exception
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
        final String JSON = quote("\\'");
        JsonParser jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")                
                : createParserUsingReader(f, JSON);
        try {      
            jp.nextToken();
            jp.getText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "unrecognized character escape");
        }
        // and then verify it's ok...
        f.configure(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER, true);
        assertTrue(f.isEnabled(JsonParser.Feature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER));
        jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")                
                : createParserUsingReader(f, JSON);
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("'", jp.getText());
    }

    private void _testLeadingZeroes(boolean useStream) throws Exception
    {
        // first: verify that we get an exception
        JsonFactory f = new JsonFactory();
        assertFalse(f.isEnabled(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS));
        String JSON = "00003";
        JsonParser jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")                
                : createParserUsingReader(f, JSON);
        try {      
            jp.nextToken();
            jp.getText();
            fail("Should have thrown an exception for doc <"+JSON+">");
        } catch (JsonParseException e) {
            verifyException(e, "invalid numeric value");
        }
        
        // and then verify it's ok when enabled
        f.configure(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS, true);
        assertTrue(f.isEnabled(JsonParser.Feature.ALLOW_NUMERIC_LEADING_ZEROS));
        jp = useStream ? createParserUsingStream(f, JSON, "UTF-8")                
                : createParserUsingReader(f, JSON);
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(3, jp.getIntValue());
        assertEquals("3", jp.getText());
        jp.close();
    
        // Plus, also: verify that leading zero magnitude is ok:
        JSON = "0"+Integer.MAX_VALUE;
        jp = useStream ? createParserUsingStream(f, JSON, "UTF-8") : createParserUsingReader(f, JSON);
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(String.valueOf(Integer.MAX_VALUE), jp.getText());
        assertEquals(Integer.MAX_VALUE, jp.getIntValue());
        Number nr = jp.getNumberValue();
        assertSame(Integer.class, nr.getClass());
        jp.close();
    }
    
}
