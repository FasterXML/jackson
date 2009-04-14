package main;

import java.math.BigDecimal;

import org.codehaus.jackson.*;

/**
 * Set of basic unit tests for verifying that the basic parser
 * functionality works as expected.
 */
public class TestNumericValues
    extends BaseTest
{
    public void testSimpleInt()
        throws Exception
    {
        int EXP_I = 1234;

        JsonParser jp = createParserUsingReader("[ "+EXP_I+" ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(""+EXP_I, jp.getText());

        assertEquals(EXP_I, jp.getIntValue());
        assertEquals((long) EXP_I, jp.getLongValue());
        assertEquals((double) EXP_I, jp.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_I), jp.getDecimalValue());
    }

    public void testSimpleLong()
        throws Exception
    {
        long EXP_L = 12345678907L;

        JsonParser jp = createParserUsingReader("[ "+EXP_L+" ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(""+EXP_L, jp.getText());

        assertEquals(EXP_L, jp.getLongValue());
        // Should get an exception if trying to convert to int 
        try {
            jp.getIntValue();
        } catch (JsonParseException jpe) {
            verifyException(jpe, "out of range");
        }
        assertEquals((double) EXP_L, jp.getDoubleValue());
        assertEquals(BigDecimal.valueOf((long) EXP_L), jp.getDecimalValue());
    }

    public void testSimpleDouble()
        throws Exception
    {
        /* Testing double is more difficult, given the rounding
         * errors and such. But let's try anyways.
         */
        String EXP_D_STR = "1234.00";
        double EXP_D = Double.parseDouble(EXP_D_STR);

        JsonParser jp = createParserUsingReader("[ "+EXP_D_STR+" ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(EXP_D_STR, jp.getText());
        assertEquals(EXP_D, jp.getDoubleValue());
        jp.close();

        EXP_D_STR = "2.1101567E-16";
        EXP_D = Double.parseDouble(EXP_D_STR);

        jp = createParserUsingReader("[ "+EXP_D_STR+" ]");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(EXP_D_STR, jp.getText());
        assertEquals(EXP_D, jp.getDoubleValue());
        jp.close();
    }

    public void testNumbers()
        throws Exception
    {
        final String DOC = "[ -13, 8100200300, 13.5, 0.00010, -2.033 ]";
        JsonParser jp = createParserUsingStream(DOC, "UTF-8");

        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(-13, jp.getIntValue());
        assertEquals(-13L, jp.getLongValue());
        assertEquals(-13., jp.getDoubleValue());
        assertEquals("-13", jp.getText());

        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertEquals(8100200300L, jp.getLongValue());
        // Should get exception for overflow:
        try {
            /*int x =*/ jp.getIntValue();
            fail("Expected an exception for overflow");
        } catch (Exception e) {
            verifyException(e, "out of range");
        }
        assertEquals(8100200300., jp.getDoubleValue());
        assertEquals("8100200300", jp.getText());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(13, jp.getIntValue());
        assertEquals(13L, jp.getLongValue());
        assertEquals(13.5, jp.getDoubleValue());
        assertEquals("13.5", jp.getText());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(0, jp.getIntValue());
        assertEquals(0L, jp.getLongValue());
        assertEquals(0.00010, jp.getDoubleValue());
        assertEquals("0.00010", jp.getText());

        assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
        assertEquals(-2, jp.getIntValue());
        assertEquals(-2L, jp.getLongValue());
        assertEquals(-2.033, jp.getDoubleValue());
        assertEquals("-2.033", jp.getText());

        assertToken(JsonToken.END_ARRAY, jp.nextToken());
    }
}
