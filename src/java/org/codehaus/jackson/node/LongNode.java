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
        return ((LongNode) o).mValue == mValue;
    }

    @Override
    public int hashCode() {
        return ((int) mValue) ^ (int) (mValue >> 32);
    }
}
