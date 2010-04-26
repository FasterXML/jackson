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

    public abstract JsonParser.NumberType getNumberType();

    public abstract Number getNumberValue();
    public abstract int getIntValue();
    public abstract long getLongValue();
    public abstract double getDoubleValue();
    public abstract BigDecimal getDecimalValue();
    public abstract BigInteger getBigIntegerValue();

    public abstract String getValueAsText();
}
