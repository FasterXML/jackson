package org.codehaus.jackson.map.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.io.NumberOutput;

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

    public void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeNumber(mValue);
    }

    public boolean equals(Object o)
    {
        if (o == this) {
            return true;
        }
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return ((DoubleNode) o).mValue == mValue;
    }
}
