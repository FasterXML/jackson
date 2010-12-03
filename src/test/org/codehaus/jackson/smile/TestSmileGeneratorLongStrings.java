package org.codehaus.jackson.smile;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;

public class TestSmileGeneratorLongStrings extends SmileTestBase
{
    final static int DOC_LEN = 2000000; // 2 meg test doc
    
    public void testLongWithMultiBytes() throws Exception
    {
        SmileFactory f = new SmileFactory();
        ArrayList<String> strings = new ArrayList<String>();
        Random rnd = new Random(123);

        ByteArrayOutputStream out = new ByteArrayOutputStream(DOC_LEN);
        SmileGenerator gen = f.createJsonGenerator(out);
        gen.writeStartArray();
        
        // Let's create 1M doc, first using Strings
        while (out.size() < (DOC_LEN - 10000)) {
            String str = generateString(5000, rnd);
            strings.add(str);
            gen.writeString(str);
        }
        gen.writeEndArray();
        gen.close();
        // Written ok; let's try parsing then
        _verifyStrings(f, out.toByteArray(), strings);

        // Then same with char[] 
        out = new ByteArrayOutputStream(DOC_LEN);
        gen = f.createJsonGenerator(out);
        gen.writeStartArray();
        
        // Let's create 1M doc, first using Strings
        for (int i = 0, len = strings.size(); i < len; ++i) {
            char[] ch = strings.get(i).toCharArray();
            gen.writeString(ch, 0, ch.length);
        }
        gen.writeEndArray();
        gen.close();
        _verifyStrings(f, out.toByteArray(), strings);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected String generateString(int length, Random rnd) throws Exception
    {
        StringBuilder sw = new StringBuilder(length+10);
        do {
            // First, add 7 ascii characters
            int num = 4 + (rnd.nextInt() & 7);
            while (--num >= 0) {
                sw.append((char) ('A' + num));
            }
            // Then a unicode char of 2, 3 or 4 bytes long
            switch (rnd.nextInt() % 3) {
            case 0:
                sw.append((char) (256 + rnd.nextInt() & 511));
                break;
            case 1:
                sw.append((char) (2048 + rnd.nextInt() & 4095));
                break;
            default:
                sw.append((char) (65536 + rnd.nextInt() & 0x3FFF));
                break;
            }
        } while (sw.length() < length);
        return sw.toString();
    }

    private void _verifyStrings(JsonFactory f, byte[] input, List<String> strings)
        throws IOException
    {
        JsonParser jp = f.createJsonParser(input);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        for (int i = 0, len = strings.size(); i < len; ++i) {
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(strings.get(i), jp.getText());
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        jp.close();
    }
}
