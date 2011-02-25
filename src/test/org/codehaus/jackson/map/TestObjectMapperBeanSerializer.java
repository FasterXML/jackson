package org.codehaus.jackson.map;

import main.BaseTest;

import java.io.*;
import java.net.*;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;

/**
 * This unit test suite tries to verify that the "Native" java type
 * mapper can properly serialize Java core objects to JSON.
 *
 * @author Scott Dixon
 */
public class TestObjectMapperBeanSerializer
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

        JsonParser jp = new JsonFactory().createJsonParser(new StringReader(aWriter.toString()));

        assertEquals(JsonToken.START_OBJECT, jp.nextToken());

        while (jp.nextToken() != JsonToken.END_OBJECT) {
            assertEquals(JsonToken.FIELD_NAME, jp.getCurrentToken());
            String name = jp.getCurrentName();
            JsonToken t = jp.nextToken();

            if (name.equals("uri")) {
                assertToken(JsonToken.VALUE_STRING, t);
                assertEquals(FixtureObject.VALUE_URSTR, getAndVerifyText(jp));
            } else if (name.equals("url")) {
                assertToken(JsonToken.VALUE_STRING, t);
                assertEquals(FixtureObject.VALUE_URSTR, getAndVerifyText(jp));
            } else if (name.equals("testNull")) {
                assertToken(JsonToken.VALUE_NULL, t);
            } else if (name.equals("testString")) {
                assertToken(JsonToken.VALUE_STRING, t);
                assertEquals(FixtureObject.VALUE_STRING, getAndVerifyText(jp));
            } else if (name.equals("testBoolean")) {
                assertToken(JsonToken.VALUE_TRUE, t);
            } else if (name.equals("testEnum")) {
                assertToken(JsonToken.VALUE_STRING, t);
                assertEquals(FixtureObject.VALUE_ENUM.toString(),getAndVerifyText(jp));
            } else if (name.equals("testInteger")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, t);
                assertEquals(jp.getIntValue(),FixtureObject.VALUE_INT);
            } else if (name.equals("testLong")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, t);
                assertEquals(jp.getLongValue(),FixtureObject.VALUE_LONG);
            } else if (name.equals("testBigInteger")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, t);
                assertEquals(jp.getLongValue(),FixtureObject.VALUE_BIGINT.longValue());
            } else if (name.equals("testBigDecimal")) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
                assertEquals(jp.getText(), FixtureObject.VALUE_BIGDEC.toString());
            } else if (name.equals("testCharacter")) {
                assertToken(JsonToken.VALUE_STRING, t);
                assertEquals(String.valueOf(FixtureObject.VALUE_CHAR), getAndVerifyText(jp));
            } else if (name.equals("testShort")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, t);
                assertEquals(jp.getIntValue(),FixtureObject.VALUE_SHORT);
            } else if (name.equals("testByte")) {
                assertToken(JsonToken.VALUE_NUMBER_INT, t);
                assertEquals(jp.getIntValue(),FixtureObject.VALUE_BYTE);
            } else if (name.equals("testFloat")) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
                assertEquals(jp.getDecimalValue().floatValue(),FixtureObject.VALUE_FLOAT);
            } else if (name.equals("testDouble")) {
                assertToken(JsonToken.VALUE_NUMBER_FLOAT, t);
                assertEquals(jp.getDoubleValue(),FixtureObject.VALUE_DBL);
            } else if (name.equals("testStringBuffer")) {
                assertToken(JsonToken.VALUE_STRING, t);
                assertEquals(FixtureObject.VALUE_STRING, getAndVerifyText(jp));
            } else if (name.equals("testError")) {
                // More complicated...
                assertToken(JsonToken.START_OBJECT, t);

                //getTestError->Exception::getCause
                
                while (jp.nextToken() == JsonToken.FIELD_NAME) {
                    name = jp.getCurrentName();
                    if (name.equals("cause")) {
                        assertEquals(JsonToken.VALUE_NULL, jp.nextToken());
                    } else if (name.equals("message")) {
                        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
                        assertEquals(FixtureObject.VALUE_ERRTXT, getAndVerifyText(jp));
                    } else if (name.equals("localizedMessage")) {
                        assertEquals(JsonToken.VALUE_STRING, jp.nextToken());
                    } else if (name.equals("stackTrace")) {
                        assertEquals(JsonToken.START_ARRAY,jp.nextToken());
                        int i = 0;
                        while(jp.nextToken() != JsonToken.END_ARRAY) {
                            if(i >= 100000) {
                                assertTrue("Probably run away loop in test. StackTrack Array was not properly closed.",false);
                            }
                        }
                    } else {
                        fail("Unexpected field name '"+name+"'");
                    }
                }
                //CLOSE OF THE EXCEPTION
                assertEquals(JsonToken.END_OBJECT, jp.getCurrentToken());
            } else {
                fail("Unexpected field, name '"+name+"'");
            }
        }

        //END OF TOKEN PARSING
        assertNull(jp.nextToken());
    }

    private static enum EFixtureEnum
    {
        THIS_IS_AN_ENUM_VALUE_0,
        THIS_IS_AN_ENUM_VALUE_1,
        THIS_IS_AN_ENUM_VALUE_2,
        THIS_IS_AN_ENUM_VALUE_3,
    }

    @SuppressWarnings("unused")
    private static class FixtureObjectBase
    {
        public static final String       VALUE_STRING = "foobar";
        public static final EFixtureEnum VALUE_ENUM   = EFixtureEnum.THIS_IS_AN_ENUM_VALUE_2;
        public static final int          VALUE_INT    = Integer.MIN_VALUE;
        public static final long         VALUE_LONG   = Long.MIN_VALUE;
        public static final BigInteger   VALUE_BIGINT = new BigInteger((new Long(Long.MAX_VALUE)).toString());
        public static final BigDecimal   VALUE_BIGDEC = new BigDecimal((new Double(Double.MAX_VALUE)).toString());
        // this is not necessarily a good char to check
        public static final char         VALUE_CHAR   = Character.MAX_VALUE;
        public static final short        VALUE_SHORT  = Short.MAX_VALUE;
        public static final byte         VALUE_BYTE   = Byte.MAX_VALUE;
        public static final float        VALUE_FLOAT  = Float.MAX_VALUE;
        public static final double       VALUE_DBL    = Double.MAX_VALUE;
        public static final String       VALUE_ERRTXT = "This is the message text for the test error.";

        public static final String       VALUE_URSTR  = "http://jackson.codehaus.org/hi?var1=foo%20bar";

        public URL getURL() throws IOException
        {
            return new URL(VALUE_URSTR);
        }

        public URI getURI() throws IOException
        {
            try {
                return new URI(VALUE_URSTR);
            } catch (Exception e) {
                throw new IllegalArgumentException(e);
            }
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
    }

    @SuppressWarnings("unused")
    private static class FixtureObject extends FixtureObjectBase
    {
        public Exception getTestError()
        {
            return new Exception(VALUE_ERRTXT);
        }
    }
}
