package main;

import org.codehaus.jackson.*;

import java.io.*;
import java.util.*;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
public class TestJsonParser
    extends BaseTest
{
    /**
     * This basic unit test verifies that example given in the Json
     * specification (RFC-4627 or later) is properly parsed at
     * high-level, without verifying values.
     */
    public void testSpecExampleSkipping()
        throws Exception
    {
        doTestSpec(false);
    }

    /**
     * Unit test that verifies that the spec example JSON is completely
     * parsed, and proper values are given for contents of all
     * events/tokens.
     */
    public void testSpecExampleFully()
        throws Exception
    {
        doTestSpec(true);
    }

    /**
     * Unit test that verifies that 3 basic keywords (null, true, false)
     * are properly parsed in various contexts.
     */
    public void testKeywords()
        throws Exception
    {
        final String DOC = "{\n"
            +"\"key1\" : null,\n"
            +"\"key2\" : true,\n"
            +"\"key3\" : false,\n"
            +"\"key4\" : [ false, null, true ]\n"
            +"}"
            ;

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");

        JsonReadContext ctxt = jp.getParsingContext();
        assertTrue(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertFalse(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        /* Before advancing to content, we should have following
         * default state...
         */
        assertFalse(jp.hasCurrentToken());
        assertNull(jp.getText());
        assertNull(jp.getTextCharacters());
        assertEquals(0, jp.getTextLength());
        // not sure if this is defined but:
        assertEquals(0, jp.getTextOffset());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());

        assertTrue(jp.hasCurrentToken());
        JsonLocation loc = jp.getTokenLocation();
        assertNotNull(loc);
        assertEquals(1, loc.getLineNr());
        assertEquals(1, loc.getColumnNr());

        ctxt = jp.getParsingContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(0, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key1");
        assertEquals(2, jp.getTokenLocation().getLineNr());

        ctxt = jp.getParsingContext();
        assertFalse(ctxt.inRoot());
        assertFalse(ctxt.inArray());
        assertTrue(ctxt.inObject());
        assertEquals(1, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        assertToken(JsonToken.VALUE_NULL, jp.nextToken());

        ctxt = jp.getParsingContext();
        assertEquals(1, ctxt.getEntryCount());
        assertEquals(0, ctxt.getCurrentIndex());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key2");
        ctxt = jp.getParsingContext();
        assertEquals(2, ctxt.getEntryCount());
        assertEquals(1, ctxt.getCurrentIndex());

        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key3");
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());

        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        verifyFieldName(jp, "key4");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
        assertToken(JsonToken.VALUE_NULL, jp.nextToken());
        assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        assertToken(JsonToken.END_OBJECT, jp.nextToken());
    }

    public void testInvalidKeywords()
        throws Exception
    {
        doTestInvalidKeyword1("nul");
        doTestInvalidKeyword2("nulla", JsonToken.VALUE_NULL);
        doTestInvalidKeyword1("fal");
        doTestInvalidKeyword3("False");
        doTestInvalidKeyword2("falsett0", JsonToken.VALUE_FALSE);
        doTestInvalidKeyword1("tr");
        doTestInvalidKeyword1("truE");
        doTestInvalidKeyword2("trueenough", JsonToken.VALUE_TRUE);
    }

    public void testSkipping()
        throws Exception
    {
        String DOC =
            "[ 1, 3, [ true, null ], 3, { \"a\":\"b\" }, [ [ ] ], { } ]";
            ;
        JsonParser jp = createParserUsingStream(DOC, "UTF-8");

        // First, skipping of the whole thing
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        jp.skipChildren();
        assertEquals(JsonToken.END_ARRAY, jp.getCurrentToken());
        JsonToken t = jp.nextToken();
        if (t != null) {
            fail("Expected null at end of doc, got "+t);
        }
        jp.close();

        // Then individual ones
        jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        jp.skipChildren();
        // shouldn't move
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.getCurrentToken());
        assertEquals(1, jp.getIntValue());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        // then skip array
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_ARRAY, jp.getCurrentToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_ARRAY, jp.getCurrentToken());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        jp.skipChildren();
        assertToken(JsonToken.END_OBJECT, jp.getCurrentToken());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());

        jp.close();
    }

    public void testNameEscaping() throws IOException
    {
        _testNameEscaping(false);
        _testNameEscaping(true);
    }

    private void _testNameEscaping(boolean useStream) throws IOException
    {
        final Map<String,String> NAME_MAP = new LinkedHashMap<String,String>();
        NAME_MAP.put("", "");
        NAME_MAP.put("\\\"funny\\\"", "\"funny\"");
        NAME_MAP.put("\\\\", "\\");
        NAME_MAP.put("\\r", "\r");
        NAME_MAP.put("\\n", "\n");
        NAME_MAP.put("\\t", "\t");
        NAME_MAP.put("\\r\\n", "\r\n");
        NAME_MAP.put("Line\\nfeed", "Line\nfeed");
        NAME_MAP.put("Yet even longer \\\"name\\\"!", "Yet even longer \"name\"!");

        JsonFactory jf = new JsonFactory();
        int entry = 0;
        for (Map.Entry<String,String> en : NAME_MAP.entrySet()) {
            ++entry;
            String input = en.getKey();
            String expResult = en.getValue();
            final String DOC = "{ \""+input+"\":null}";
            JsonParser jp = useStream ?
                jf.createJsonParser(new ByteArrayInputStream(DOC.getBytes("UTF-8")))
                : jf.createJsonParser(new StringReader(DOC));

            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            // first, sanity check (field name == getText()
            String act = jp.getCurrentName();
            assertEquals(act, getAndVerifyText(jp));
            if (!expResult.equals(act)) {
                String msg = "Failed for name #"+entry+"/"+NAME_MAP.size();
                if (expResult.length() != act.length()) {
                    /*
System.err.println("DEBUG -> '"+act+"', jp = "+jp);
for (int i = 0; i < act.length(); ++i) System.err.println("Char: "+((int) act.charAt(i)));
                    */
                    fail(msg+": exp length "+expResult.length()+", actual "+act.length());
                }
                assertEquals(msg, expResult, act);
            }
            assertToken(JsonToken.VALUE_NULL, jp.nextToken());
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            jp.close();
        }
    }
    
    /**
     * Unit test that verifies that long text segments are handled
     * correctly; mostly to stress-test underlying segment-based
     * text buffer(s).
     */
    public void testLongText() throws Exception
    {
        final int LEN = 96000;
        StringBuilder sb = new StringBuilder(LEN + 100);
        Random r = new Random(99);
        while (sb.length() < LEN) {
            sb.append(r.nextInt());
            sb.append(" xyz foo");
            if (r.nextBoolean()) {
                sb.append(" and \"bar\"");
            } else if (r.nextBoolean()) {
                sb.append(" [whatever].... ");
            } else {
                // Let's try some more 'exotic' chars
                sb.append(" UTF-8-fu: try this {\u00E2/\u0BF8/\uA123!} (look funny?)");
            }
            if (r.nextBoolean()) {
                if (r.nextBoolean()) {
                    sb.append('\n');
                } else if (r.nextBoolean()) {
                    sb.append('\r');
                } else {
                    sb.append("\r\n");
                }
            }
        }
        final String VALUE = sb.toString();

        JsonFactory jf = new JsonFactory();

        // Let's use real generator to get json done right
        StringWriter sw = new StringWriter(LEN + (LEN >> 2));
        JsonGenerator jg = jf.createJsonGenerator(sw);
        jg.writeStartObject();
        jg.writeFieldName("doc");
        jg.writeString(VALUE);
        jg.writeEndObject();
        jg.close();

        final String DOC = sw.toString();

        for (int type = 0; type < 3; ++type) {
            JsonParser jp;

            switch (type) {
            default:
                jp = jf.createJsonParser(DOC.getBytes("UTF-8"));
                break;
            case 1:
                jp = jf.createJsonParser(DOC);
                break;
            case 2: // NEW: let's also exercise UTF-32...
                jp = jf.createJsonParser(encodeInUTF32BE(DOC));
                break;
            }
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals("doc", jp.getCurrentName());
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());

            String act = getAndVerifyText(jp);
            if (act.length() != VALUE.length()) {
                fail("Expected length "+VALUE.length()+", got "+act.length());
            }
            if (!act.equals(VALUE)) {
                fail("Long text differs");
            }

            // should still know the field name
            assertEquals("doc", jp.getCurrentName());
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            jp.close();
        }
    }

    /*
    /////////////////////////////////////////////
    // Helper methods
    /////////////////////////////////////////////
    */

    private void doTestSpec(boolean verify)
        throws IOException
    {
        // First, using a StringReader:
        doTestSpecIndividual(null, verify);

        // Then with streams using supported encodings:
        doTestSpecIndividual("UTF-8", verify);
        doTestSpecIndividual("UTF-16BE", verify);
        doTestSpecIndividual("UTF-16LE", verify);

        /* Hmmh. UTF-32 is harder only because JDK doesn't come with
         * a codec for it. Can't test it yet using this method
         */
        doTestSpecIndividual("UTF-32", verify);
    }

    private void doTestSpecIndividual(String enc, boolean verify)
        throws IOException
    {
        String doc = SAMPLE_DOC_JSON_SPEC;
        JsonParser jp;

        if (enc == null) {
            jp = createParserUsingReader(doc);
        } else {
            jp = createParserUsingStream(doc, enc);
        }

        assertToken(JsonToken.START_OBJECT, jp.nextToken()); // main object

        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Image'
        if (verify) {
            verifyFieldName(jp, "Image");
        }

        assertToken(JsonToken.START_OBJECT, jp.nextToken()); // 'image' object

        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Width'
        if (verify) {
            verifyFieldName(jp, "Width");
        }

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        if (verify) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_WIDTH);
        }

        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Height'
        if (verify) {
            verifyFieldName(jp, "Height");
        }

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_HEIGHT);
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Title'
        if (verify) {
            verifyFieldName(jp, "Title");
        }
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(SAMPLE_SPEC_VALUE_TITLE, getAndVerifyText(jp));
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Thumbnail'
        if (verify) {
            verifyFieldName(jp, "Thumbnail");
        }

        assertToken(JsonToken.START_OBJECT, jp.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Url'
        if (verify) {
            verifyFieldName(jp, "Url");
        }
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        if (verify) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, getAndVerifyText(jp));
        }
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Height'
        if (verify) {
            verifyFieldName(jp, "Height");
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_HEIGHT);
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Width'
        if (verify) {
            verifyFieldName(jp, "Width");
        }
        // Width value is actually a String in the example
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, getAndVerifyText(jp));

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // 'thumbnail' object

        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'IDs'
        assertToken(JsonToken.START_ARRAY, jp.nextToken()); // 'ids' array
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[0]
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID1);
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[1]
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID2);
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[2]
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID3);
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[3]
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID4);
        assertToken(JsonToken.END_ARRAY, jp.nextToken()); // 'ids' array

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // 'image' object

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // main object
    }

    private void verifyFieldName(JsonParser jp, String expName)
        throws IOException
    {
        assertEquals(expName, jp.getText());
        assertEquals(expName, jp.getCurrentName());
    }

    private void verifyIntValue(JsonParser jp, long expValue)
        throws IOException
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), jp.getText());
    }

    private void doTestInvalidKeyword1(String value)
        throws IOException
    {
        JsonParser jp = createParserUsingStream("{ \"key1\" : "+value+" }", "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        /* 24-Nov-2008, tatu: Note that depending on parser impl, we may
         *   get the exception early or late...
         */
        try {
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            jp.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unrecognized token");
        }
    }

    private void doTestInvalidKeyword2(String value, JsonToken firstValue)
        throws IOException
    {
        JsonParser jp = createParserUsingStream("{ \"key1\" : "+value+" }", "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertToken(firstValue, jp.nextToken());
        try {
            jp.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "Unexpected character");
        }
    }

    private void doTestInvalidKeyword3(String value)
        throws IOException
    {
        JsonParser jp = createParserUsingStream("{ \"key1\" : "+value+" }", "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        /* 24-Nov-2008, tatu: Note that depending on parser impl, we may
         *   get the exception early or late...
         */
        try {
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            jp.nextToken();
            fail("Expected an exception for malformed value keyword");
        } catch (JsonParseException jex) {
            verifyException(jex, "expected a valid value");
        }
    }
}

