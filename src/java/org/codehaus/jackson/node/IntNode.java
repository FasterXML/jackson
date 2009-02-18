package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.io.NumberOutput;
import org.codehaus.jackson.map.SerializerProvider;

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
        return ((IntNode) o).mValue == mValue;
    }

    @Override
        public int hashCode() { return mValue; }
}
