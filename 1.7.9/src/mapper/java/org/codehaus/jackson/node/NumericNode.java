package org.codehaus.jackson.node;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.JsonParser;

/**
 * Intermediate value node used for numeric nodes.
 */
public abstract class NumericNode
    extends ValueNode
{
    protected NumericNode() { }

    @Override
    public final boolean isNumber() { return true; }

    // // // Let's re-abstract so sub-classes handle them

    @Override
    public abstract JsonParser.NumberType getNumberType();

    @Override
    public abstract Number getNumberValue();
    @Override
    public abstract int getIntValue();
    @Override
    public abstract long getLongValue();
    @Override
    public abstract double getDoubleValue();
    @Override
    public abstract BigDecimal getDecimalValue();
    @Override
    public abstract BigInteger getBigIntegerValue();

    /* 
    /**********************************************************
    /* General type coercions
    /**********************************************************
     */
    
    @Override
    public abstract String getValueAsText();

    @Override
    public int getValueAsInt() {
        return getIntValue();
    }
    @Override
    public int getValueAsInt(int defaultValue) {
        return getIntValue();
    }

    @Override
    public long getValueAsLong() {
        return getLongValue();
    }
    @Override
    public long getValueAsLong(long defaultValue) {
        return getLongValue();
    }
    
    @Override
    public double getValueAsDouble() {
        return getDoubleValue();
    }
    @Override
    public double getValueAsDouble(double defaultValue) {
        return getDoubleValue();
    }
}
