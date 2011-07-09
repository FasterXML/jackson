package org.codehaus.jackson.impl;

import main.BaseTest;

import org.codehaus.jackson.*;

import java.io.*;
import java.util.Random;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
public class TestUtf8Parser
    extends BaseTest
{
    final static String[] UTF8_2BYTE_STRINGS = new String[] {
        /* This may look funny, but UTF8 scanner has fairly
         * elaborate decoding machinery, and it is indeed
         * necessary to try out various combinations...
         */
        "b", "A\u00D8", "abc", "c3p0",
        "12345", "......", "Long\u00FAer",
        "Latin1-fully-\u00BE-develop\u00A8d",
        "Some very long name, ridiculously long actually to see that buffer expansion works: \u00BF?"
    };

    final static String[] UTF8_3BYTE_STRINGS = new String[] {
        "\uC823?", "A\u400F", "1\u1234?",
        "Ab123\u4034",
        "Even-longer:\uC023"
    };

    public void testEmptyName()
        throws Exception
    {
        final String DOC = "{ \"\" : \"\" }";

        JsonParser jp = createParserUsingStream(DOC, "UTF-8");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("", jp.getCurrentName());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals("", jp.getText());
        assertToken(JsonToken.END_OBJECT, jp.nextToken());
        jp.close();
    }

    public void testUtf8Name2Bytes()
        throws Exception
    {
        final String[] NAMES = UTF8_2BYTE_STRINGS;

        for (int i = 0; i < NAMES.length; ++i) {
            String NAME = NAMES[i];
            String DOC = "{ \""+NAME+"\" : 0 }";
            JsonParser jp = createParserUsingStream(DOC, "UTF-8");
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals(NAME, jp.getCurrentName());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            // should retain name during value entry, too
            assertEquals(NAME, jp.getCurrentName());
            
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            jp.close();
        }
    }

    public void testUtf8Name3Bytes() throws Exception
    {
        final String[] NAMES = UTF8_3BYTE_STRINGS;

        for (int i = 0; i < NAMES.length; ++i) {
            String NAME = NAMES[i];
            String DOC = "{ \""+NAME+"\" : true }";

            JsonParser jp = createParserUsingStream(DOC, "UTF-8");
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            
            assertToken(JsonToken.FIELD_NAME, jp.nextToken());
            assertEquals(NAME, jp.getCurrentName());
            assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
            assertEquals(NAME, jp.getCurrentName());
            
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            
            jp.close();
        }
    }

    // How about tests for Surrogate-Pairs?

    public void testUtf8StringTrivial() throws Exception
    {
        String[] VALUES = UTF8_2BYTE_STRINGS;
        for (int i = 0; i < VALUES.length; ++i) {
            String VALUE = VALUES[i];
            String DOC = "[ \""+VALUE+"\" ]";
            JsonParser jp = createParserUsingStream(DOC, "UTF-8");
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            String act = getAndVerifyText(jp);
            if (act.length() != VALUE.length()) {
                fail("Failed for value #"+(i+1)+"/"+VALUES.length+": length was "+act.length()+", should be "+VALUE.length());
            }
            assertEquals(VALUE, act);
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }

        VALUES = UTF8_3BYTE_STRINGS;
        for (int i = 0; i < VALUES.length; ++i) {
            String VALUE = VALUES[i];
            String DOC = "[ \""+VALUE+"\" ]";
            JsonParser jp = createParserUsingStream(DOC, "UTF-8");
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(VALUE, getAndVerifyText(jp));
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            jp.close();
        }
    }

    public void testUtf8StringValue() throws Exception
    {
        Random r = new Random(13);
        //int LEN = 72000;
        int LEN = 720;
        StringBuilder sb = new StringBuilder(LEN + 20);
        while (sb.length() < LEN) {
            int c;
            if (r.nextBoolean()) { // ascii
                c = 32 + (r.nextInt() & 0x3F);
                if (c == '"' || c == '\\') {
                    c = ' ';
                }
            } else if (r.nextBoolean()) { // 2-byte
                c = 160 + (r.nextInt() & 0x3FF);
            } else if (r.nextBoolean()) { // 3-byte (non-surrogate)
                c = 8000 + (r.nextInt() & 0x7FFF);
            } else { // surrogates (2 chars)
                int value = r.nextInt() & 0x3FFFF; // 20-bit, ~ 1 million
                sb.append((char) (0xD800 + (value >> 10)));
                c = (0xDC00 + (value & 0x3FF));

            }
            sb.append((char) c);
        }

        ByteArrayOutputStream bout = new ByteArrayOutputStream(LEN);
        OutputStreamWriter out = new OutputStreamWriter(bout, "UTF-8");
        out.write("[\"");
        String VALUE = sb.toString();
        out.write(VALUE);
        out.write("\"]");
        out.close();

        byte[] data = bout.toByteArray();

        JsonParser jp = new JsonFactory().createJsonParser(new ByteArrayInputStream(data));
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        String act = jp.getText();

        assertEquals(VALUE.length(), act.length());
        assertEquals(VALUE, act);
        jp.close();
    }
}
