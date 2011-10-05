package org.codehaus.jackson.main;

import main.BaseTest;

import org.codehaus.jackson.*;

import java.io.IOException;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
public class TestParserLinefeeds
    extends BaseTest
{
    public void testCR() throws Exception
    {
        _testLinefeeds("\r", true);
        _testLinefeeds("\r", false);
    }

    public void testLF() throws Exception
    {
        _testLinefeeds("\n", true);
        _testLinefeeds("\n", false);
    }

    public void testCRLF() throws Exception
    {
        _testLinefeeds("\r\n", true);
        _testLinefeeds("\r\n", false);
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private void _testLinefeeds(String lf, boolean useStream)
        throws IOException
    {
        String DOC = "[1,@2,@-178@]";
        DOC = DOC.replaceAll("@", lf);

        JsonParser jp = useStream ?
            createParserUsingStream(DOC, "UTF-8")
            : createParserUsingReader(DOC);
            
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertEquals(1, jp.getCurrentLocation().getLineNr());
        
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(1, jp.getIntValue());
        assertEquals(1, jp.getCurrentLocation().getLineNr());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(2, jp.getIntValue());
        assertEquals(2, jp.getCurrentLocation().getLineNr());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-178, jp.getIntValue());
        assertEquals(3, jp.getCurrentLocation().getLineNr());
        
        assertToken(JsonToken.END_ARRAY, jp.nextToken());
        assertEquals(4, jp.getCurrentLocation().getLineNr());

        jp.close();
    }
}
