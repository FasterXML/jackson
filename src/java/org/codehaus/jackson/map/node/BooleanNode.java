package org.codehaus.jackson.map.node;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

/**
 * This concrete value class is used to contain boolean (true / false)
 * values. Only two instances are ever created, to minimize memory
 * usage
 */
public final class BooleanNode
    extends ValueNode
{
    // // Just need two instances...

    private final static BooleanNode sTrue = new BooleanNode();
    private final static BooleanNode sFalse = new BooleanNode();

    private BooleanNode() { }

    public static BooleanNode getTrue() { return sTrue; }
    public static BooleanNode getFalse() { return sFalse; }

    public static BooleanNode valueOf(boolean b) { return b ? sTrue : sFalse; }

    @Override
    public boolean isBoolean() { return true; }

    @Override
    public boolean getBooleanValue() {
        return (this == sTrue);
    }

    public String getValueAsText() {
        return (this == sTrue) ? "true" : "false";
    }

    public void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeBoolean(this == sTrue);
    }

    public boolean equals(Object o)
    {
        /* Since there are only ever two instances in existence
         * can do identity comparison
         */
        return (o == this);
    }
}
