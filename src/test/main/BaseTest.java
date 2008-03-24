package main;

import java.io.*;

import junit.framework.TestCase;

import org.codehaus.jackson.*;

public class BaseTest
    extends TestCase
{
    /*
    ////////////////////////////////////////////////////////
    // Some sample documents:
    ////////////////////////////////////////////////////////
     */

    protected final static int SAMPLE_SPEC_VALUE_WIDTH = 800;
    protected final static int SAMPLE_SPEC_VALUE_HEIGHT = 600;
    protected final static String SAMPLE_SPEC_VALUE_TITLE = "View from 15th Floor";
    protected final static String SAMPLE_SPEC_VALUE_TN_URL = "http://www.example.com/image/481989943";
    protected final static int SAMPLE_SPEC_VALUE_TN_HEIGHT = 125;
    protected final static String SAMPLE_SPEC_VALUE_TN_WIDTH = "100";
    protected final static int SAMPLE_SPEC_VALUE_TN_ID1 = 116;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID2 = 943;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID3 = 234;
    protected final static int SAMPLE_SPEC_VALUE_TN_ID4 = 38793;

    protected final static String SAMPLE_DOC_JSON_SPEC = 
        "{\n"
        +"  \"Image\" : {\n"
        +"    \"Width\" : "+SAMPLE_SPEC_VALUE_WIDTH+",\n"
        +"    \"Height\" : "+SAMPLE_SPEC_VALUE_HEIGHT+","
        +"\"Title\" : \""+SAMPLE_SPEC_VALUE_TITLE+"\",\n"
        +"    \"Thumbnail\" : {\n"
        +"      \"Url\" : \""+SAMPLE_SPEC_VALUE_TN_URL+"\",\n"
        +"\"Height\" : "+SAMPLE_SPEC_VALUE_TN_HEIGHT+",\n"
        +"      \"Width\" : \""+SAMPLE_SPEC_VALUE_TN_WIDTH+"\"\n"
        +"    },\n"
        +"    \"IDs\" : ["+SAMPLE_SPEC_VALUE_TN_ID1+","+SAMPLE_SPEC_VALUE_TN_ID2+","+SAMPLE_SPEC_VALUE_TN_ID3+","+SAMPLE_SPEC_VALUE_TN_ID4+"]\n"
        +"  }"
        +"}"
        ;

    /*
    ////////////////////////////////////////////////////////
    // Parser/generator construction
    ////////////////////////////////////////////////////////
     */

    protected JsonParser createParserUsingReader(String input)
        throws IOException, JsonParseException
    {
        return new JsonFactory().createJsonParser(new StringReader(input));
    }

    protected JsonParser createParserUsingStream(String input, String encoding)
        throws IOException, JsonParseException
    {
        byte[] data = input.getBytes(encoding);
        InputStreamReader is = new InputStreamReader(new ByteArrayInputStream(data), encoding);
        return new JsonFactory().createJsonParser(is);
    }

    /*
    ////////////////////////////////////////////////////////
    // Additional assertion methods
    ////////////////////////////////////////////////////////
     */

    protected void assertToken(JsonToken expToken, JsonToken actToken)
    {
        if (actToken != expToken) {
            fail("Expected token "+expToken+", current token "+actToken);
        }
    }

    protected void assertToken(JsonToken expToken, JsonParser jp)
    {
        assertToken(expToken, jp.getCurrentToken());
    }

    protected void verifyException(Exception e, String match)
    {
        String msg = e.getMessage();
        if (msg.indexOf(match) < 0) {
            fail("Expected an exception with sub-string \""+match+"\": got one with message \""+msg+"\"");
        }
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser jp)
        throws IOException, JsonParseException
    {
        String str = jp.getText();

        // Ok, let's verify other accessors
        int actLen = jp.getTextLength();
        assertEquals(str.length(), actLen);
        char[] ch = jp.getTextCharacters();
        /*String str2 =*/ new String(ch, jp.getTextOffset(), actLen);

        return str;
    }
}
