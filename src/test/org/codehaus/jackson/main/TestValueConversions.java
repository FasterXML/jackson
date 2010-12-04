package org.codehaus.jackson.main;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

/**
 * @since 1.6
 */
public class TestValueConversions
    extends main.BaseTest
{
    public void testAsInt() throws Exception
    {
        final String input = "[ 1, -3, 4.98, true, false, null, \"-17\", \"foo\" ]";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp;
            if (i == 0) {
                jp = createParserUsingReader(input);                
            } else {
                jp = this.createParserUsingStream(input, "UTF-8");
            }
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertEquals(0, jp.getValueAsLong());
            assertEquals(9, jp.getValueAsLong(9));

            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(1, jp.getValueAsLong());
            assertEquals(1, jp.getValueAsLong(-99));
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(-3, jp.getValueAsLong());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
            assertEquals(4, jp.getValueAsLong());
            assertEquals(4, jp.getValueAsLong(99));
            assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
            assertEquals(1, jp.getValueAsLong());
            assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
            assertEquals(0, jp.getValueAsLong());
            assertToken(JsonToken.VALUE_NULL, jp.nextToken());
            assertEquals(0, jp.getValueAsLong());
            assertEquals(0, jp.getValueAsLong(27));
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(-17, jp.getValueAsLong());
            assertEquals(-17, jp.getValueAsLong(3));
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(0, jp.getValueAsLong());
            assertEquals(9, jp.getValueAsLong(9));
            
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            assertEquals(0, jp.getValueAsLong());
            assertEquals(9, jp.getValueAsLong(9));

            jp.close();
        }     
    }

    /**
     * @since 1.7
     */
    public void testAsBoolean() throws Exception
    {
        final String input = "[ true, false, null, 1, 0, \"true\", \"false\", \"foo\" ]";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp;
            if (i == 0) {
                jp = createParserUsingReader(input);                
            } else {
                jp = this.createParserUsingStream(input, "UTF-8");
            }
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertEquals(false, jp.getValueAsBoolean());
            assertEquals(true, jp.getValueAsBoolean(true));

            assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
            assertEquals(true, jp.getValueAsBoolean());
            assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
            assertEquals(false, jp.getValueAsBoolean());
            assertToken(JsonToken.VALUE_NULL, jp.nextToken());
            assertEquals(false, jp.getValueAsBoolean());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(1, jp.getIntValue());
            assertEquals(true, jp.getValueAsBoolean());
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(0, jp.getIntValue());
            assertEquals(false, jp.getValueAsBoolean());

            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(true, jp.getValueAsBoolean());
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(false, jp.getValueAsBoolean());
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(false, jp.getValueAsBoolean());
            
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            assertEquals(false, jp.getValueAsBoolean());
            assertEquals(true, jp.getValueAsBoolean(true));

            jp.close();
        }     
    }
    
    public void testAsLong() throws Exception
    {
        final String input = "[ 1, -3, 4.98, true, false, null, \"-17\", \"foo\" ]";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp;
            if (i == 0) {
                jp = createParserUsingReader(input);                
            } else {
                jp = this.createParserUsingStream(input, "UTF-8");
            }
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertEquals(0L, jp.getValueAsLong());
            assertEquals(9L, jp.getValueAsLong(9L));

            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(1L, jp.getValueAsLong());
            assertEquals(1L, jp.getValueAsLong(-99L));
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(-3L, jp.getValueAsLong());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
            assertEquals(4L, jp.getValueAsLong());
            assertEquals(4L, jp.getValueAsLong(99L));
            assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
            assertEquals(1L, jp.getValueAsLong());
            assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
            assertEquals(0L, jp.getValueAsLong());
            assertToken(JsonToken.VALUE_NULL, jp.nextToken());
            assertEquals(0L, jp.getValueAsLong());
            assertEquals(0L, jp.getValueAsLong(27L));
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(-17L, jp.getValueAsLong());
            assertEquals(-17L, jp.getValueAsLong(3L));
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(0L, jp.getValueAsLong());
            assertEquals(9L, jp.getValueAsLong(9L));
            
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            assertEquals(0L, jp.getValueAsLong());
            assertEquals(9L, jp.getValueAsLong(9L));

            jp.close();
        }     
    }

    public void testAsDouble() throws Exception
    {
        final String input = "[ 1, -3, 4.98, true, false, null, \"-17.25\", \"foo\" ]";
        for (int i = 0; i < 2; ++i) {
            JsonParser jp;
            if (i == 0) {
                jp = createParserUsingReader(input);                
            } else {
                jp = this.createParserUsingStream(input, "UTF-8");
            }
            assertToken(JsonToken.START_ARRAY, jp.nextToken());
            assertEquals(0.0, jp.getValueAsDouble());
            assertEquals(9.0, jp.getValueAsDouble(9.0));

            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(1., jp.getValueAsDouble());
            assertEquals(1., jp.getValueAsDouble(-99.0));
            assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
            assertEquals(-3., jp.getValueAsDouble());
            assertToken(JsonToken.VALUE_NUMBER_FLOAT, jp.nextToken());
            assertEquals(4.98, jp.getValueAsDouble());
            assertEquals(4.98, jp.getValueAsDouble(12.5));
            assertToken(JsonToken.VALUE_TRUE, jp.nextToken());
            assertEquals(1.0, jp.getValueAsDouble());
            assertToken(JsonToken.VALUE_FALSE, jp.nextToken());
            assertEquals(0.0, jp.getValueAsDouble());
            assertToken(JsonToken.VALUE_NULL, jp.nextToken());
            assertEquals(0.0, jp.getValueAsDouble());
            assertEquals(0.0, jp.getValueAsDouble(27.8));
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(-17.25, jp.getValueAsDouble());
            assertEquals(-17.25, jp.getValueAsDouble(1.9));
            assertToken(JsonToken.VALUE_STRING, jp.nextToken());
            assertEquals(0.0, jp.getValueAsDouble());
            assertEquals(1.25, jp.getValueAsDouble(1.25));
            
            assertToken(JsonToken.END_ARRAY, jp.nextToken());
            assertEquals(0.0, jp.getValueAsDouble());
            assertEquals(7.5, jp.getValueAsDouble(7.5));

            jp.close();
        }     
    }
    
}
