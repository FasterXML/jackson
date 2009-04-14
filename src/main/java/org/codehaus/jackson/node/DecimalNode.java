package org.codehaus.jackson.node;

import java.io.IOException;
import java.math.BigDecimal;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Numeric node that contains values that do not fit in simple
 * integer (int, long) or floating point (double) values.
 */
public final class DecimalNode
    extends NumericNode
{
    final BigDecimal _value;

    public DecimalNode(BigDecimal v) { _value = v; }

    public static DecimalNode valueOf(BigDecimal d) { return new DecimalNode(d); }

    @Override
        public boolean isFloatingPointNumber() { return true; }
    
    @Override
        public boolean isBigDecimal() { return true; }
    
    @Override
        public Number getNumberValue() { return _value; }

    @Override
        public int getIntValue() { return _value.intValue(); }

    @Override
        public long getLongValue() { return _value.longValue(); }

    @Override
        public double getDoubleValue() { return _value.doubleValue(); }

    @Override
        public BigDecimal getDecimalValue() { return _value; }

    public String getValueAsText() {
        return _value.toString();
    }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        jg.writeNumber(_value);
    }

    @Override
    public boolean equals(Object o)
    {
        if (o == this) return true;
        if (o == null) return false;
        if (o.getClass() != getClass()) { // final class, can do this
            return false;
        }
        return ((DecimalNode) o)._value.equals(_value);
    }

    @Override
        public int hashCode() { return _value.hashCode(); }
}
