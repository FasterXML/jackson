package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.NumberOutput;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Numeric node that contains simple 64-bit integer values.
 */
public final class LongNode
    extends NumericNode
{
    final long _value;

    public LongNode(long v) { _value = v; }

    public static LongNode valueOf(long l) { return new LongNode(l); }

    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isLong() { return true; }

    @Override
    public Number getNumberValue() {
        return Long.valueOf(_value);
    }

    @Override
        public int getIntValue() { return (int) _value; }

    @Override
        public long getLongValue() { return _value; }

    @Override
        public double getDoubleValue() { return (double) _value; }

    @Override
        public BigDecimal getDecimalValue() { return BigDecimal.valueOf(_value); }

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
        return ((LongNode) o)._value == _value;
    }

    @Override
    public int hashCode() {
        return ((int) _value) ^ (int) (_value >> 32);
    }
}
