package org.codehaus.jackson.map.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.io.NumberOutput;

/**
 * Numeric node that contains simple 32-bit integer values.
 */
public final class IntNode
    extends NumericNode
{
    final int mValue;

    public IntNode(int v) { mValue = v; }

    public static IntNode valueOf(int i) { return new IntNode(i); }

    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isInt() { return true; }

    @Override
    public Number getNumberValue() {
        return Integer.valueOf(mValue);
    }

    @Override
        public int getIntValue() { return mValue; }

    @Override
        public long getLongValue() { return (long) mValue; }

    @Override
        public double getDoubleValue() { return (double) mValue; }

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
        return ((IntNode) o).mValue == mValue;
    }
}
