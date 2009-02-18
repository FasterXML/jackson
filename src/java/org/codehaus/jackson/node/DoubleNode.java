package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.NumberOutput;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Numeric node that contains 64-bit ("double precision")
 * floating point values simple 32-bit integer values.
 */
public final class DoubleNode
    extends NumericNode
{
    final double mValue;

    public DoubleNode(double v) { mValue = v; }

    public static DoubleNode valueOf(double v) { return new DoubleNode(v); }

    @Override
        public boolean isFloatingPointNumber() { return true; }

    @Override
        public boolean isDouble() { return true; }

    @Override
    public Number getNumberValue() {
        return Double.valueOf(mValue);
    }

    @Override
        public int getIntValue() { return (int) mValue; }

    @Override
        public long getLongValue() { return (long) mValue; }

    @Override
        public double getDoubleValue() { return mValue; }

    @Override
        public BigDecimal getDecimalValue() { return BigDecimal.valueOf(mValue); }

    public String getValueAsText() {
        return NumberOutput.toString(mValue);
    }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeNumber(mValue);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return ((DoubleNode) o).mValue == mValue;
    }

    @Override
    public int hashCode()
    {
        // same as hashCode Double.class uses
        long l = Double.doubleToLongBits(mValue);
        return ((int) l) ^ (int) (l >> 32);

    }
}
