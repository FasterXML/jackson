package org.codehaus.jackson.map.node;

import java.math.BigDecimal;

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

    public abstract Number getNumberValue();
    public abstract int getIntValue();
    public abstract long getLongValue();
    public abstract double getDoubleValue();
    public abstract BigDecimal getDecimalValue();

    public abstract String getValueAsText();
}
