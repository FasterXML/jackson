package map;

import main.BaseTest;

import java.io.*;
import java.net.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite tries to verify that the "Native" java type
 * mapper can properly serialize Java core objects to JSON.
 *
 * @author Scott Dixon
 */
public class TestComplexJavaType
    extends BaseTest
{
    /**
     * Sanity test to ensure the pieces all work when put together.
     */
    public void testComplexObject()
        throws Exception
    {
        FixtureObject  aTestObj = new FixtureObject();
        ObjectMapper aMapper  = new ObjectMapper();
        StringWriter aWriter = new StringWriter();
        JsonGenerator aGen = new JsonFactory().createJsonGenerator(aWriter);
        aMapper.writeValue(aGen, aTestObj);
        aGen.close();

        JsonParser jp = new JsonFactory().createJsonParser(new
StringReader(aWriter.toString()));

        assertEquals(JsonToken.START_OBJECT, jp.nextToken());

        //getURL
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("url", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(FixtureObject.VALUE_URSTR, getAndVerifyText(jp));

        //getURI
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("uri", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(FixtureObject.VALUE_URSTR, getAndVerifyText(jp));

        //getTestNull
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testNull", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NULL, jp.nextToken());

        //getTestString
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testString", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(FixtureObject.VALUE_STRING, getAndVerifyText(jp));

        //getTestBoolean
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testBoolean", getAndVerifyText(jp));
        JsonToken aBoolTok = jp.nextToken();
        assertTrue(aBoolTok == JsonToken.VALUE_FALSE || aBoolTok ==
JsonToken.VALUE_TRUE);

        //getTestEnum
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testEnum", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(FixtureObject.VALUE_ENUM.toString(),getAndVerifyText(jp));

        //getTestInteger
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testInteger", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(jp.getIntValue(),FixtureObject.VALUE_INT);

        //getTestLong
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testLong", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(jp.getLongValue(),FixtureObject.VALUE_LONG);

        //getTestBigInteger
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testBigInteger", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(jp.getLongValue(),FixtureObject.VALUE_BIGINT.longValue());

        //getTestBigDecimal
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testBigDecimal", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(jp.getDoubleValue(),FixtureObject.VALUE_BIGDEC.doubleValue());

        //getTestCharacter
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testCharacter", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(String.format("%c",FixtureObject.VALUE_CHAR),
getAndVerifyText(jp));

        //getTestShort
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testShort", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(jp.getIntValue(), FixtureObject.VALUE_SHORT);

        //getTestByte
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testByte", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(jp.getIntValue(), FixtureObject.VALUE_BYTE);

        //getTestFloat
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testFloat", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(jp.getDecimalValue().floatValue(),
FixtureObject.VALUE_FLOAT);

        //getTestDouble
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testDouble", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(jp.getDoubleValue(), FixtureObject.VALUE_DBL);

        //getTestStringBuffer
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testStringBuffer", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(FixtureObject.VALUE_STRING, getAndVerifyText(jp));

        //getTestError
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("testError", getAndVerifyText(jp));
        assertEquals(JsonToken.START_OBJECT, jp.nextToken());
        //getTestError->Exception::getCause
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("cause", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_NULL, jp.nextToken());
        //getTestError->Exception::getMessage
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("message", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        assertEquals(FixtureObject.VALUE_ERRTXT, getAndVerifyText(jp));
        //getTestError->Exception::getLocalizedMessage
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("localizedMessage", getAndVerifyText(jp));
        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
        //getTestError->Exception::getStackTrace
        assertEquals(JsonToken.FIELD_NAME, jp.nextToken());
        assertEquals("stackTrace", getAndVerifyText(jp));
        assertEquals(JsonToken.START_ARRAY,jp.nextToken());
        int i = 0;
        while(jp.nextToken() != JsonToken.END_ARRAY) {
            if(i >= 100000) {
                assertTrue("Probably run away loop in test. StackTrack Array was not properly closed.",false);
            }
        }
        //CLOSE OF THE EXCEPTION
        assertEquals(JsonToken.END_OBJECT,jp.nextToken());
        //CLOSE OF THE TEST OBJECT
        assertEquals(JsonToken.END_OBJECT,jp.nextToken());
        //END OF TOKEN PARSING
        assertNull(jp.nextToken());
    }

    private static enum EFixtureEnum
    {
        THIS_IS_AN_ENUM_VALUE_0,
        THIS_IS_AN_ENUM_VALUE_1,
        THIS_IS_AN_ENUM_VALUE_2,
        THIS_IS_AN_ENUM_VALUE_3,
    };
    private static class FixtureObjectBase
    {
        public static final String       VALUE_STRING = "foobar";
        public static final EFixtureEnum VALUE_ENUM   =
EFixtureEnum.THIS_IS_AN_ENUM_VALUE_2;
        public static final int          VALUE_INT    = Integer.MIN_VALUE;
        public static final long         VALUE_LONG   = Long.MIN_VALUE;
        public static final BigInteger   VALUE_BIGINT = new
BigInteger((new Long(Long.MAX_VALUE)).toString());
        public static final BigDecimal   VALUE_BIGDEC = new
BigDecimal((new Double(Double.MAX_VALUE)).toString());
        public static final char         VALUE_CHAR   = Character.MAX_VALUE;
        public static final short        VALUE_SHORT  = Short.MAX_VALUE;
        public static final byte         VALUE_BYTE   = Byte.MAX_VALUE;
        public static final float        VALUE_FLOAT  = Float.MAX_VALUE;
        public static final double       VALUE_DBL    = Double.MAX_VALUE;
        public static final String       VALUE_ERRTXT = "This is the message text for the test error.";
        public static final String       VALUE_URSTR  = "http://jackson.codehaus.org/hi?var1=foo%20bar";
        public static final URL          VALUE_URL;
        public static final URI          VALUE_URI;
        static {
            URL aUrl = null;
            URI aUri = null;
            try
            {
                aUrl = new URL(VALUE_URSTR);
            }
            catch(MalformedURLException e)
            {
                assertTrue(e.toString(),false);
            }
            finally
            {
                VALUE_URL = aUrl;
            }
            try
            {
                aUri = new URI(VALUE_URSTR);
            }
            catch(URISyntaxException e)
            {
                assertTrue(e.toString(),false);
            }
            finally
            {
                VALUE_URI = aUri;
            }
        }
        public URL getURL()
        {
            return VALUE_URL;
        }
        public URI getURI()
        {
            return VALUE_URI;
        }
        public String getTestNull()
        {
            return null;
        }
        public String getTestString()
        {
            return VALUE_STRING;
        }
        public boolean getTestBoolean()
        {
            return true;
        }
        public EFixtureEnum getTestEnum()
        {
            return VALUE_ENUM;
        }
        public int getTestInteger()
        {
            return VALUE_INT;
        }
        public long getTestLong()
        {
            return VALUE_LONG;
        }
        public BigInteger getTestBigInteger()
        {
            return VALUE_BIGINT;
        }
        public BigDecimal getTestBigDecimal()
        {
            return VALUE_BIGDEC;
        }
        public char getTestCharacter()
        {
            return VALUE_CHAR;
        }
        public short getTestShort()
        {
            return VALUE_SHORT;
        }
        public byte getTestByte()
        {
            return VALUE_BYTE;
        }
        public float getTestFloat()
        {
            return VALUE_FLOAT;
        }
        public double getTestDouble()
        {
            return VALUE_DBL;
        }
        public StringBuffer getTestStringBuffer()
        {
            return new StringBuffer(VALUE_STRING);
        }
    };
    private static class FixtureObject extends FixtureObjectBase
    {
        public Exception getTestError()
        {
            return new Exception(VALUE_ERRTXT);
        }
    };
}
