package main;

import org.codehaus.jackson.*;

/**
 * Set of basic unit tests for verifying that Array/Object scopes
 * are properly matched.
 */
public class TestScopeMatching
    extends BaseTest
{
    public void testUnclosedArray()
        throws Exception
    {
        JsonParser jp = createParserUsingReader("[ 1, 2");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected an exception for unclosed ARRAY");
        } catch (JsonParseException jpe) {
            verifyException(jpe, "expected close marker for ARRAY");
        }
    }

    public void testUnclosedObject()
        throws Exception
    {
        JsonParser jp = createParserUsingReader("{ \"key\" : 3  ");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected an exception for unclosed OBJECT");
        } catch (JsonParseException jpe) {
            verifyException(jpe, "expected close marker for OBJECT");
        }
    }

    public void testMismatchArrayToObject()
        throws Exception
    {
        JsonParser jp = createParserUsingReader("[ 1, 2 }");
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());
        assertToken(JsonToken.VALUE_NUMBER_INT, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected an exception for incorrectly closed ARRAY");
        } catch (JsonParseException jpe) {
            verifyException(jpe, "Unexpected close marker");
        }
    }

    public void testMismatchObjectToArray()
        throws Exception
    {
        JsonParser jp = createParserUsingReader("{ ]");
        assertToken(JsonToken.START_OBJECT, jp.nextToken());

        try {
            jp.nextToken();
            fail("Expected an exception for incorrectly closed OBJECT");
        } catch (JsonParseException jpe) {
            verifyException(jpe, "Unexpected close marker");
        }
    }
}
