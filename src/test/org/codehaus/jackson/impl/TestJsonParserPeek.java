package org.codehaus.jackson.impl;

import java.io.IOException;
import java.io.StringReader;

import org.codehaus.jackson.*;
import org.codehaus.jackson.util.TokenBuffer;

public class TestJsonParserPeek
    extends main.BaseTest
{
    public void testSimple() throws Exception
    {
        _testSimple(0);
        _testSimple(1);
        _testSimple(2);
    }
    
    private void _testSimple(int mode) throws IOException
    {
        final String JSON = "[1, true, null, {\"a\" :  \n [ ], \"booya\":{ } } ]";
        JsonParser jp;
        switch (mode) {
        case 0:
            jp = createParserUsingReader(JSON);
            break;
        case 1:
            jp = createParserUsingStream(JSON, "UTF-8");
            break;
        default:
            jp = tokenBuffer(JSON).asParser();
        }

        assertNull(jp.getCurrentToken());
        assertToken(JsonToken.START_ARRAY, jp.peekNextToken());
        assertNull(jp.getText());
        assertNull(jp.getCurrentToken());
        assertPeekAndNext(jp, JsonToken.START_ARRAY);
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.peekNextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.peekNextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());
        assertPeekAndNext(jp, JsonToken.VALUE_TRUE);
        assertPeekAndNext(jp, JsonToken.VALUE_NULL);

        assertPeekAndNext(jp, JsonToken.START_OBJECT);
        assertPeekAndNext(jp, JsonToken.FIELD_NAME);
        assertEquals("a", jp.getCurrentName());
        assertPeekAndNext(jp, JsonToken.START_ARRAY);
        assertPeekAndNext(jp, JsonToken.END_ARRAY);
        assertPeekAndNext(jp, JsonToken.FIELD_NAME);
        assertEquals("booya", jp.getCurrentName());
        assertPeekAndNext(jp, JsonToken.START_OBJECT);
        assertPeekAndNext(jp, JsonToken.END_OBJECT);
        assertPeekAndNext(jp, JsonToken.END_OBJECT);
        assertPeekAndNext(jp, JsonToken.END_ARRAY);
    }

    private void assertPeekAndNext(JsonParser jp, JsonToken exp) throws IOException
    {
        assertToken(exp, jp.peekNextToken());
        assertToken(exp, jp.nextToken());
    }

    private TokenBuffer tokenBuffer(String json) throws IOException
    {
        JsonParser jp = createParserUsingReader(json);
        TokenBuffer tb = new TokenBuffer(null);
        while (jp.nextToken() != null) {
            tb.copyCurrentEvent(jp);
        }
        jp.close();
        return tb;
    }
}
    