package org.codehaus.jackson.node;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;

public class TestNodeTraversingParser
    extends BaseMapTest
{
    public void testSimple() throws Exception
    {
        // For convenience, parse tree from JSON first
        final String JSON =
            "{ \"a\" : 123, \"list\" : [ 12, null, true, { }, [ ] ] }";
        ObjectMapper m = new ObjectMapper();
        JsonNode tree = m.readTree(JSON);
        JsonParser jp = tree.traverse();

        assertNull(jp.getCurrentToken());

        assertToken(JsonToken.START_OBJECT, jp.nextToken());

        jp.close();
        assertTrue(jp.isClosed());
    }
}

