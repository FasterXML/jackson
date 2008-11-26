package org.codehaus.jackson.map.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.io.NumberOutput;

/**
 * Numeric node that contains simple 64-bit integer values.
 */
public final class LongNode
    extends NumericNode
{
    final long mValue;

    public LongNode(long v) { mValue = v; }

    public static LongNode valueOf(long l) { return new LongNode(l); }

    @Override
    public boolean isIntegralNumber() { return true; }

    @Override
    public boolean isLong() { return true; }

    @Override
    public Number getNumberValue() {
        return Long.valueOf(mValue);
    }

    @Override
        public int getIntValue() { return (int) mValue; }

    @Override
        public long getLongValue() { return mValue; }

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
        return ((LongNode) o).mValue == mValue;
    }
}
