package org.codehaus.jackson.map.deser;

import org.codehaus.jackson.map.BaseMapTest;

import java.io.IOException;

import org.codehaus.jackson.map.*;

/**
 * Unit tests for verifying that simple exceptions can be deserialized.
 */
public class TestExceptionDeserialization
    extends BaseMapTest
{
    public void testIOException()
        throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        IOException ioe = new IOException("TEST");

        String json = serializeAsString(mapper, ioe);

        IOException result = mapper.readValue(json, IOException.class);

        assertEquals(ioe.getMessage(), result.getMessage());
    }
}
