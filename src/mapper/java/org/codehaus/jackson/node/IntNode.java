package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.NumberOutput;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Numeric node that contains simple 32-bit integer values.
 */
public final class IntNode
    extends NumericNode
{
    final int _value;

    public IntNode(int v) { _value = v; }

    public static IntNode valueOf(int i) { return new IntNode(i); }

    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isInt() { return true; }

    @Override
    public Number getNumberValue() {
        return Integer.valueOf(_value);
    }

    @Override
        public int getIntValue() { return _value; }

    @Override
        public long getLongValue() { return (long) _value; }

    @Override
        public double getDoubleValue() { return (double) _value; }

    @Override
        public BigDecimal getDecimalValue() { return BigDecimal.valueOf(_value); }

    @Override
        public BigInteger getBigIntegerValue() { return BigInteger.valueOf(_value); }

    public String getValueAsText() {
        return NumberOutput.toString(_value);
    }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return ((IntNode) o)._value == _value;
    }

    @Override
        public int hashCode() { return _value; }
}
