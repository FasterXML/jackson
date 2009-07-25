package org.codehaus.jackson.main;

import main.BaseTest;

import org.codehaus.jackson.*;

import java.io.*;

/**
 * Some unit tests to try to exercise part of parser code that
 * deals with symbol (table) management.
 */
public class TestWithTonsaSymbols
    extends BaseTest
{
    final static String FIELD_BASENAME = "f";

    /**
     * How many fields to generate? Since maximum symbol table
     * size is defined as 6000 (above which table gets cleared,
     * assuming the name vocabulary is unbounded), let's do something
     * just slightly below it.
     */
    final static int FIELD_COUNT = 5000;

    public void testStreamReaderParser() throws Exception
    {
        _testWith(true);
    }

    public void testReaderParser() throws Exception
    {
        _testWith(false);
    }

    /*
    //////////////////////////////////////////////////////
    // Helper methods
    //////////////////////////////////////////////////////
     */

    private void _testWith(boolean useStream)
        throws Exception
    {
        JsonFactory jf = new JsonFactory();
        String doc = buildDoc(FIELD_COUNT);

        /* And let's do this multiple times: just so that symbol table
         * state is different between runs.
         */
        for (int x = 0; x < 3; ++x) {
            JsonParser jp = useStream ?
                jf.createJsonParser(new ByteArrayInputStream(doc.getBytes("UTF-8")))
                : jf.createJsonParser(new StringReader(doc));
            assertToken(JsonToken.START_OBJECT, jp.nextToken());
            for (int i = 0; i < FIELD_COUNT; ++i) {
                assertToken(JsonToken.FIELD_NAME, jp.nextToken());
                assertEquals(fieldNameFor(i), jp.getCurrentName());
                assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
                assertEquals(i, jp.getIntValue());
            }
            assertToken(JsonToken.END_OBJECT, jp.nextToken());
            jp.close();
        }
    }

    private void fieldNameFor(StringBuilder sb, int index)
    {
        /* let's do something like "f1.1" to exercise different
         * field names (important for byte-based codec)
         * Other name shuffling done mostly just for fun... :)
         */
        sb.append(FIELD_BASENAME);
        sb.append(index);
        if (index > 50) {
            sb.append('.');
            if (index > 200) {
                sb.append(index);
                if (index > 4000) { // and some even longer symbols...
                    sb.append(".").append(index);
                }
            } else {
                sb.append(index >> 3); // divide by 8
            }
        }
    }

    private String fieldNameFor(int index)
    {
        StringBuilder sb = new StringBuilder(16);
        fieldNameFor(sb, index);
        return sb.toString();
    }
        
    private String buildDoc(int len)
    {
        StringBuilder sb = new StringBuilder(len * 12);
        sb.append('{');
        for (int i = 0; i < len; ++i) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('"');
            fieldNameFor(sb, i);
            sb.append('"');
            sb.append(':');
            sb.append(i);
        }
        sb.append('}');
        return sb.toString();
    }
}
