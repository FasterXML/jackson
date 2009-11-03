package org.codehaus.jackson.map.ser;

import org.codehaus.jackson.map.BaseMapTest;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.*;

/**
 * Unit tests for verifying that constraints on ordering of serialized
 * properties are held.
 */
public class TestSerializationOrder
    extends BaseMapTest
{
    /*
    //////////////////////////////////////////////
    // Annotated helper classes
    //////////////////////////////////////////////
     */

    static class BeanWithCreator
    {
        public int a;
        public int b;
        public int c;

        @JsonCreator public BeanWithCreator(@JsonProperty("c") int c, @JsonProperty("a") int a) {
            this.a = a;
            this.c = c;
        }
    }

    /*
    //////////////////////////////////////////////
    // Unit tests
    //////////////////////////////////////////////
     */

    // Test for [JACKSON-170]
    public void testImplicitOrderByCreator() throws Exception
    {
        assertEquals("{\"c\":1,\"a\":2,\"b\":0}", serializeAsString(new BeanWithCreator(1, 2)));
    }
}
