package main;

import org.codehaus.jackson.*;

import java.io.*;

/**
 * Set of basic unit tests for verifying that the basic generator
 * functionality works as expected.
 */
public class TestJsonGenerator
    extends BaseTest
{
    // // // First, tests for primitive (non-structured) values

    public void testStringWrite()
        throws Exception
    {
        JsonFactory jf = new JsonFactory();
        String[] inputStrings = new String[] { "", "X", "1234567890" };
        for (int useReader = 0; useReader < 2; ++useReader) {
            for (int writeString = 0; writeString < 2; ++writeString) {
                for (int strIx = 0; strIx < inputStrings.length; ++strIx) {
                    String input = inputStrings[strIx];
                    JsonGenerator gen;
                    ByteArrayOutputStream bout = new ByteArrayOutputStream();
                    if (useReader != 0) {
                        gen = jf.createJsonGenerator(new OutputStreamWriter(bout, "UTF-8"));
                    } else {
                        gen = jf.createJsonGenerator(bout, JsonEncoding.UTF8);
                    }
                    if (writeString > 0) {
                        gen.writeString(input);
                    } else {
                        int len = input.length();
                        char[] buffer = new char[len + 20];
                        // Let's use non-zero base offset too...
                        input.getChars(0, len, buffer, strIx);
                        gen.writeString(buffer, strIx, len);
                    }
                    gen.flush();
                    gen.close();
                
                    JsonParser jp = jf.createJsonParser(new ByteArrayInputStream(bout.toByteArray()));
                
                    JsonToken t = jp.nextToken();
                    assertNotNull("Document \""+bout.toString("UTF-8")+"\" yielded no tokens", t);
                    assertEquals(JsonToken.VALUE_STRING, t);
                    assertEquals(input, jp.getText());
                    assertEquals(null, jp.nextToken());
                    jp.close();
                }
            }
        }
    }

    public void testIntWrite()
        throws Exception
    {
        doTestIntWrite(false);
        doTestIntWrite(true);
    }

    public void testLongWrite()
        throws Exception
    {
        doTestLongWrite(false);
        doTestLongWrite(true);
    }

    public void testBooleanWrite()
        throws Exception
    {
        for (int i = 0; i < 4; ++i) {
            boolean state = (i & 1) == 0;
            boolean pad = (i & 2) == 0;
            StringWriter sw = new StringWriter();
            JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
            gen.writeBoolean(state);
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            String exp = Boolean.valueOf(state).toString();
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(state ? JsonToken.VALUE_TRUE : JsonToken.VALUE_FALSE, t);
            assertEquals(null, jp.nextToken());
            jp.close();
        }
    }

    public void testNullWrite()
        throws Exception
    {
        for (int i = 0; i < 2; ++i) {
            boolean pad = (i & 1) == 0;
            StringWriter sw = new StringWriter();
            JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
            gen.writeNull();
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            String exp = "null";
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(JsonToken.VALUE_NULL, t);
            assertEquals(null, jp.nextToken());
            jp.close();
        }
    }

    // // // Then binary value writing

    /**
     * This is really inadequate test, all in all, but should serve
     * as some kind of sanity check. Reader-side should more thoroughly
     * test things, as it does need writers to construct the data first.
     */
    public void testBinaryWrite() throws Exception
    {
        _testBinaryWrite(false);
        _testBinaryWrite(true);
    }

    private void _testBinaryWrite(boolean useCharBased) throws Exception
    {
        /* The usual sample input string, from Thomas Hobbes's "Leviathan"
         * (via Wikipedia)
         */
        final String INPUT = "Man is distinguished, not only by his reason, but by this singular passion from other animals, which is a lust of the mind, that by a perseverance of delight in the continued and indefatigable generation of knowledge, exceeds the short vehemence of any carnal pleasure.";
        final byte[] INPUT_BYTES = INPUT.getBytes("US-ASCII");
        // as per MIME variant, result minus lfs =
        final String OUTPUT =
 "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlz"
+"IHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2Yg"
+"dGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGlu"
+"dWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRo"
+"ZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4="
            ;

        /* Let's only test the standard base64 variant; but write
         * values in root, array and object contexts.
         */
        Base64Variant b64v = Base64Variants.getDefaultVariant();
        JsonFactory jf = new JsonFactory();

        for (int i = 0; i < 3; ++i) {
            JsonGenerator gen;
            ByteArrayOutputStream bout = new ByteArrayOutputStream(200);
            if (useCharBased) {
                gen = jf.createJsonGenerator(new OutputStreamWriter(bout, "UTF-8"));
            } else {
                gen = jf.createJsonGenerator(bout, JsonEncoding.UTF8);
            }

            switch (i) {
            case 0: // root
                gen.writeBinary(b64v, INPUT_BYTES, 0, INPUT_BYTES.length);
                break;
            case 1: // array
                gen.writeStartArray();
                gen.writeBinary(b64v, INPUT_BYTES, 0, INPUT_BYTES.length);
                gen.writeEndArray();
                break;
            default: // object
                gen.writeStartObject();
                gen.writeFieldName("field");
                gen.writeBinary(b64v, INPUT_BYTES, 0, INPUT_BYTES.length);
                gen.writeEndObject();
                break;
            }
            gen.close();

            JsonParser jp = jf.createJsonParser(new ByteArrayInputStream(bout.toByteArray()));
            
            // Need to skip other events before binary data:
            switch (i) {
            case 0:
                break;
            case 1:
                assertEquals(JsonToken.START_ARRAY, jp.nextToken());
                break;
            default:
                assertEquals(JsonToken.START_OBJECT, jp.nextToken());
                assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
                break;
            }
            assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
            String actualValue = jp.getText();
            jp.close();
            assertEquals(OUTPUT, actualValue);
        }
    }

    // // Then root-level output testing

     public void testRootIntsWrite()
         throws Exception
     {
         StringWriter sw = new StringWriter();
         JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
         gen.writeNumber(1);
         gen.writeNumber(2);
         gen.writeNumber(-13);
         gen.close();

         String docStr = sw.toString();

         JsonParser jp = createParserUsingReader(docStr);
         assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
         assertEquals(1, jp.getIntValue());
         assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
         assertEquals(2, jp.getIntValue());
         assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
         assertEquals(-13, jp.getIntValue());
         jp.close();
     }

    /*
    //////////////////////////////////////////////////
    // Internal methods
    //////////////////////////////////////////////////
     */
    
    private void doTestIntWrite(boolean pad)
        throws Exception
    {
        int[] VALUES = new int[] {
            0, 1, -9, 32, -32, 57, 13240, -9999, Integer.MAX_VALUE, Integer.MAX_VALUE
        };
        for (int i = 0; i < VALUES.length; ++i) {
            int VALUE = VALUES[i];
            StringWriter sw = new StringWriter();
            JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
            gen.writeNumber(VALUE);
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            assertNotNull("Document \""+docStr+"\" yielded no tokens", t);
            // Number are always available as lexical representation too
            String exp = ""+VALUE;
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(VALUE, jp.getIntValue());
            assertEquals(null, jp.nextToken());
            jp.close();
        }
    }

    private void doTestLongWrite(boolean pad)
        throws Exception
    {
        long[] VALUES = new long[] {
            0L, 1L, -1L, -12005002294L, Long.MIN_VALUE, Long.MAX_VALUE
        };
        for (int i = 0; i < VALUES.length; ++i) {
            long VALUE = VALUES[i];
            StringWriter sw = new StringWriter();
            JsonGenerator gen = new JsonFactory().createJsonGenerator(sw);
            gen.writeNumber(VALUE);
            if (pad) {
                gen.writeRaw(" ");
            }
            gen.close();
            String docStr = sw.toString();
            JsonParser jp = createParserUsingReader(docStr);
            JsonToken t = jp.nextToken();
            assertNotNull("Document \""+docStr+"\" yielded no tokens", t);
            String exp = ""+VALUE;
            if (!exp.equals(jp.getText())) {
                fail("Expected '"+exp+"', got '"+jp.getText());
            }
            assertEquals(JsonToken.VALUE_NUMBER_INT, t);
            assertEquals(VALUE, jp.getLongValue());
            assertEquals(null, jp.nextToken());
            jp.close();
        }
    }
}
