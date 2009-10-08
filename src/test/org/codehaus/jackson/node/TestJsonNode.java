package org.codehaus.jackson.node;

import java.io.*;
import java.math.BigInteger;
import java.math.BigDecimal;

import static org.junit.Assert.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * Basic tests for {@link JsonNode} base class and some features
 * of implementation classes
 */
public class TestJsonNode
    extends BaseMapTest
{
    public void testSimple() throws Exception
    {
        // Let's use something that doesn't add much beyond JsonNode base
        NullNode n = NullNode.instance;

        // basic properties
        assertFalse(n.isContainerNode());
        assertFalse(n.isBigDecimal());
        assertFalse(n.isBigInteger());
        assertFalse(n.isBinary());
        assertFalse(n.isBoolean());
        assertFalse(n.isPojo());
        assertFalse(n.isMissingNode());

        // fallback accessors
        assertFalse(n.getBooleanValue());
        assertNull(n.getNumberValue());
        assertEquals(0, n.getIntValue());
        assertEquals(0L, n.getLongValue());
        assertEquals(BigDecimal.ZERO, n.getDecimalValue());
        assertEquals(BigInteger.ZERO, n.getBigIntegerValue());

        assertEquals(0, n.size());
        assertFalse(n.getElements().hasNext());
        assertFalse(n.getFieldNames().hasNext());
        // path is never null; but does point to missing node
        assertNotNull(n.getPath("xyz"));
        assertTrue(n.getPath("xyz").isMissingNode());

        try {
            n.append();
        } catch (IllegalStateException e) {
            verifyException(e, "non-container type");
        }
    }
}
