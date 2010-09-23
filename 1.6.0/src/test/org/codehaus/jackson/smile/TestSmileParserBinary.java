package org.codehaus.jackson.smile;

import java.io.*;

import static org.junit.Assert.*;

import org.codehaus.jackson.JsonToken;

public class TestSmileParserBinary
    extends SmileTestBase
{
    final static int[] SIZES = new int[] {
        1, 2, 3, 4, 5, 6,
        7, 8, 12,
        100, 350, 1900, 6000, 19000, 65000,
        139000
    };
    
    public void testRaw() throws IOException
    {
        _testBinary(true);
    }

    public void test7Bit() throws IOException
    {
        _testBinary(false);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testBinary(boolean raw) throws IOException
    {
        SmileFactory f = new SmileFactory();
        f.configure(SmileGenerator.Feature.ENCODE_BINARY_AS_7BIT, !raw);
        for (int size : SIZES) {
            byte[] data = _generateData(size);
            ByteArrayOutputStream bo = new ByteArrayOutputStream(size+10);            
            SmileGenerator g = f.createJsonGenerator(bo);
            g.writeStartArray();
            g.writeBinary(data);
            g.writeNumber(1); // just to verify there's no overrun
            g.writeEndArray();
            g.close();
            byte[] smile = bo.toByteArray();            
            
            // and verify
            SmileParser p = f.createJsonParser(smile);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
            byte[] result = p.getBinaryValue();
            assertArrayEquals(data, result);
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertEquals(1, p.getIntValue());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertNull(p.nextToken());
            p.close();

            // and second time around, skipping
            p = f.createJsonParser(smile);
            assertToken(JsonToken.START_ARRAY, p.nextToken());
            assertToken(JsonToken.VALUE_EMBEDDED_OBJECT, p.nextToken());
            assertToken(JsonToken.VALUE_NUMBER_INT, p.nextToken());
            assertToken(JsonToken.END_ARRAY, p.nextToken());
            assertNull(p.nextToken());
            p.close();
        }
    }

    private byte[] _generateData(int size)
    {
        byte[] result = new byte[size];
        for (int i = 0; i < size; ++i) {
            result[i] = (byte) (i % 255);
        }
        return result;
    }
}
