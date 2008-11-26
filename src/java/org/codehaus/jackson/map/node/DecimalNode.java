package org.codehaus.jackson.map.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

/**
 * Numeric node that contains values that do not fit in simple
 * integer (int, long) or floating point (double) values.
 */
public final class DecimalNode
    extends NumericNode
{
    final BigDecimal mValue;

    public DecimalNode(BigDecimal v) { mValue = v; }

    public static DecimalNode valueOf(BigDecimal d) { return new DecimalNode(d); }

    @Override
        public boolean isFloatingPointNumber() { return true; }
    
    @Override
        public boolean isBigDecimal() { return true; }
    
    @Override
        public Number getNumberValue() { return mValue; }

    @Override
        public int getIntValue() { return mValue.intValue(); }

    @Override
        public long getLongValue() { return mValue.longValue(); }

    @Override
        public double getDoubleValue() { return mValue.doubleValue(); }

    @Override
        public BigDecimal getDecimalValue() { return mValue; }

    public String getValueAsText() {
        return mValue.toString();
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
        return ((DecimalNode) o).mValue.equals(mValue);
    }
}
