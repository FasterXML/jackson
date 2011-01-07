package org.codehaus.jackson.smile;

import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.codehaus.jackson.*;

/**
 * Test similar to {@link org.codehaus.jackson.main.TestRawStringWriting},
 * to verify handling of "raw String value" write methods that by-pass
 * most encoding steps, for potential higher output speed (in cases where
 * input naturally comes as UTF-8 encoded byte arrays).
 *
 * @since 1.7
 */
public class TestGeneratorWithRawUtf8 extends SmileTestBase
{
    public void testUtf8RawStrings() throws Exception
    {
        // Let's create set of Strings to output; no ctrl chars as we do raw
        List<byte[]> strings = generateStrings(new Random(28), 750000, false);
        ByteArrayOutputStream out = new ByteArrayOutputStream(16000);
        SmileFactory jf = new SmileFactory();
        JsonGenerator jgen = jf.createJsonGenerator(out, JsonEncoding.UTF8);
        jgen.writeStartArray();
        for (byte[] str : strings) {
            jgen.writeRawUTF8String(str, 0, str.length);
        }
        jgen.writeEndArray();
        jgen.close();
        byte[] json = out.toByteArray();
        
        // Ok: let's verify that stuff was written out ok
        JsonParser jp = jf.createJsonParser(json);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        for (byte[] inputBytes : strings) {
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            String string = jp.getText();
            byte[] outputBytes = string.getBytes("UTF-8");
            assertEquals(inputBytes.length, outputBytes.length);
            assertArrayEquals(inputBytes, outputBytes);
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }

    /**
     * Unit test for "JsonGenerator.writeUTF8String()", which needs
     * to handle escaping properly
     */
    public void testUtf8StringsWithEscaping() throws Exception
    {
        // Let's create set of Strings to output; do include control chars too:
        List<byte[]> strings = generateStrings(new Random(28), 720000, true);
        ByteArrayOutputStream out = new ByteArrayOutputStream(16000);
        SmileFactory jf = new SmileFactory();
        JsonGenerator jgen = jf.createJsonGenerator(out, JsonEncoding.UTF8);
        jgen.writeStartArray();
        for (byte[] str : strings) {
            jgen.writeUTF8String(str, 0, str.length);
        }
        jgen.writeEndArray();
        jgen.close();
        byte[] json = out.toByteArray();
        
        // Ok: let's verify that stuff was written out ok
        JsonParser jp = jf.createJsonParser(json);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        for (byte[] inputBytes : strings) {
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            String string = jp.getText();
            byte[] outputBytes = string.getBytes("UTF-8");
            assertEquals(inputBytes.length, outputBytes.length);
            assertArrayEquals(inputBytes, outputBytes);
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }
    
    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */
    
    private List<byte[]> generateStrings(Random rnd, int totalLength, boolean includeCtrlChars)
        throws IOException
    {
        ArrayList<byte[]> strings = new ArrayList<byte[]>();
        do {
            int len = 2;
            int bits = rnd.nextInt(14);
            while (--bits >= 0) {
                len += len;
            }
            len = 1 + ((len + len) / 3);
            String str = generateString(rnd, len, includeCtrlChars);
            byte[] bytes = str.getBytes("UTF-8");
            strings.add(bytes);
            totalLength -= bytes.length;
        } while (totalLength > 0);
        return strings;
    }
        
    private String generateString(Random rnd, int length, boolean includeCtrlChars)
    {
        StringBuilder sb = new StringBuilder(length);
        do {
            int i;
            switch (rnd.nextInt(3)) {
            case 0: // 3 byte one
                i = 2048 + rnd.nextInt(16383);
                break;
            case 1: // 2 byte
                i = 128 + rnd.nextInt(1024);
                break;
            default: // ASCII
                i = rnd.nextInt(192);
                if (!includeCtrlChars) {
                    i += 32;
                    // but also need to avoid backslash, double-quote
                    if (i == '\\' || i == '"') {
                        i = '@'; // just arbitrary choice
                    }
                }
            }
            sb.append((char) i);
        } while (sb.length() < length);
        return sb.toString();
    }
}
