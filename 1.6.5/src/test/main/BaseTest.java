package main;

import java.io.*;
import java.util.Arrays;

import junit.framework.TestCase;

import org.codehaus.jackson.*;

//import static org.junit.Assert.*;

public abstract class BaseTest
    extends TestCase
{
    /*
    /**********************************************************
    /* Some sample documents:
    /**********************************************************
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
    /**********************************************************
    /* High-level helpers
    /**********************************************************
     */

    protected void verifyJsonSpecSampleDoc(JsonParser jp, boolean verifyContents)
        throws IOException
    {
        if (!jp.hasCurrentToken()) {
            jp.nextToken();
        }
        assertToken(JsonToken.START_OBJECT, jp.getCurrentToken()); // main object

        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Image'
        if (verifyContents) {
            verifyFieldName(jp, "Image");
        }

        assertToken(JsonToken.START_OBJECT, jp.nextToken()); // 'image' object

        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(jp, "Width");
        }

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_WIDTH);
        }

        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(jp, "Height");
        }

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_HEIGHT);
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Title'
        if (verifyContents) {
            verifyFieldName(jp, "Title");
        }
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(SAMPLE_SPEC_VALUE_TITLE, getAndVerifyText(jp));
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Thumbnail'
        if (verifyContents) {
            verifyFieldName(jp, "Thumbnail");
        }

        assertToken(JsonToken.START_OBJECT, jp.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Url'
        if (verifyContents) {
            verifyFieldName(jp, "Url");
        }
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_URL, getAndVerifyText(jp));
        }
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Height'
        if (verifyContents) {
            verifyFieldName(jp, "Height");
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_HEIGHT);
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'Width'
        if (verifyContents) {
            verifyFieldName(jp, "Width");
        }
        // Width value is actually a String in the example
        assertToken(JsonToken.VALUE_STRING, jp.nextToken());
        if (verifyContents) {
            assertEquals(SAMPLE_SPEC_VALUE_TN_WIDTH, getAndVerifyText(jp));
        }

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // 'thumbnail' object
        assertToken(JsonToken.FIELD_NAME, jp.nextToken()); // 'IDs'
        assertToken(JsonToken.START_ARRAY, jp.nextToken()); // 'ids' array
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[0]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID1);
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[1]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID2);
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[2]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID3);
        }
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken()); // ids[3]
        if (verifyContents) {
            verifyIntValue(jp, SAMPLE_SPEC_VALUE_TN_ID4);
        }
        assertToken(JsonToken.END_ARRAY, jp.nextToken()); // 'ids' array

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // 'image' object

        assertToken(JsonToken.END_OBJECT, jp.nextToken()); // main object
    }

    protected void verifyFieldName(JsonParser jp, String expName)
        throws IOException
    {
        assertEquals(expName, jp.getText());
        assertEquals(expName, jp.getCurrentName());
    }

    protected void verifyIntValue(JsonParser jp, long expValue)
        throws IOException
    {
        // First, via textual
        assertEquals(String.valueOf(expValue), jp.getText());
    }

    /**
     * Method that checks whether Unit tests appear to run from Ant build
     * scripts.
     * 
     * @since 1.6
     */
    protected static boolean runsFromAnt() {
        return "true".equals(System.getProperty("FROM_ANT"));
    }
    
    /*
    /**********************************************************
    /* Parser/generator construction
    /**********************************************************
     */

    protected JsonParser createParserUsingReader(String input)
        throws IOException, JsonParseException
    {
        return createParserUsingReader(new JsonFactory(), input);
    }

    protected JsonParser createParserUsingReader(JsonFactory f, String input)
        throws IOException, JsonParseException
    {
        return f.createJsonParser(new StringReader(input));
    }

    protected JsonParser createParserUsingStream(String input, String encoding)
        throws IOException, JsonParseException
    {
        return createParserUsingStream(new JsonFactory(), input, encoding);
    }

    protected JsonParser createParserUsingStream(JsonFactory f,
                                                 String input, String encoding)
        throws IOException, JsonParseException
    {

        /* 23-Apr-2008, tatus: UTF-32 is not supported by JDK, have to
         *   use our own codec too (which is not optimal since there's
         *   a chance both encoder and decoder might have bugs, but ones
         *   that cancel each other out or such)
         */
        byte[] data;
        if (encoding.equalsIgnoreCase("UTF-32")) {
            data = encodeInUTF32BE(input);
        } else {
            data = input.getBytes(encoding);
        }
        InputStream is = new ByteArrayInputStream(data);
        return f.createJsonParser(is);
    }

    /*
    /**********************************************************
    /* Additional assertion methods
    /**********************************************************
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

    protected void assertType(Object ob, Class<?> expType)
    {
        if (ob == null) {
            fail("Expected an object of type "+expType.getName()+", got null");
        }
        Class<?> cls = ob.getClass();
        if (!expType.isAssignableFrom(cls)) {
            fail("Expected type "+expType.getName()+", got "+cls.getName());
        }
    }

    protected void verifyException(Throwable e, String... matches)
    {
        String msg = e.getMessage();
        String lmsg = (msg == null) ? "" : msg.toLowerCase();
        for (String match : matches) {
            String lmatch = match.toLowerCase();
            if (lmsg.indexOf(lmatch) >= 0) {
                return;
            }
        }
        fail("Expected an exception with one of substrings ("+Arrays.asList(matches)+"): got one with message \""+msg+"\"");
    }

    /**
     * Method that gets textual contents of the current token using
     * available methods, and ensures results are consistent, before
     * returning them
     */
    protected String getAndVerifyText(JsonParser jp)
        throws IOException, JsonParseException
    {
        // Ok, let's verify other accessors
        int actLen = jp.getTextLength();
        char[] ch = jp.getTextCharacters();
        String str2 = new String(ch, jp.getTextOffset(), actLen);
        String str = jp.getText();

        if (str.length() !=  actLen) {
            fail("Internal problem (jp.token == "+jp.getCurrentToken()+"): jp.getText().length() ['"+str+"'] == "+str.length()+"; jp.getTextLength() == "+actLen);
        }
        assertEquals("String access via getText(), getTextXxx() must be the same", str, str2);

        return str;
    }

    /*
    /**********************************************************
    /* And other helpers
    /**********************************************************
     */

    protected byte[] encodeInUTF32BE(String input)
    {
        int len = input.length();
        byte[] result = new byte[len * 4];
        int ptr = 0;
        for (int i = 0; i < len; ++i, ptr += 4) {
            char c = input.charAt(i);
            result[ptr] = result[ptr+1] = (byte) 0;
            result[ptr+2] = (byte) (c >> 8);
            result[ptr+3] = (byte) c;
        }
        return result;
    }

    public String quote(String str) {
        return '"'+str+'"';
    }
}
