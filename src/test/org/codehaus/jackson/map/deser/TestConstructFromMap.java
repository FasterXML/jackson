package org.codehaus.jackson.map.deser;

import java.math.BigDecimal;
import java.util.*;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.*;

/**
 * This unit test suite that tests use of {@link JsonCreator}
 * with "delegate" constructors and factory methods: ones that
 * take a deserializable type that is bound from JSON content.
 * This is usually done to get two-phase data binding, often using
 * {@link java.util.Map} as the intermediate form.
 */
public class TestConstructFromMap
    extends BaseMapTest
{
    static class ConstructorFromMap
    {
        int _x;
        String _y;

        @JsonCreator
        ConstructorFromMap(Map arg)
        {
            _x = ((Number) arg.get("x")).intValue();
            _y = (String) arg.get("y");
        }
    }

    static class FactoryFromBigDecimal
    {
        int _value;

        private FactoryFromBigDecimal(int v) { _value = v; }

        @JsonCreator
            static FactoryFromBigDecimal createIt(BigDecimal d)
        {
            return new FactoryFromBigDecimal(d.intValue());
        }
    }

    /*
    //////////////////////////////////////////////
    // Test methods
    //////////////////////////////////////////////
     */

    public void testViaConstructor() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        ConstructorFromMap result = m.readValue
            ("{ \"x\":1, \"y\" : 2 }", ConstructorFromMap.class);
        assertEquals(1, result._x);
        assertEquals(2, result._y);
    }

    public void testViaFactory() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        FactoryFromBigDecimal result = m.readValue("  28.13 ", FactoryFromBigDecimal.class);
        assertEquals(28, result._value);
    }

}
