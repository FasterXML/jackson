package org.codehaus.jackson.map.node;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

/**
 * This singleton value class is used to contain explicit JSON null
 * value.
 */
public final class NullNode
    extends ValueNode
{
    // // Just need two instances...

    private final static NullNode sNull = new NullNode();

    private NullNode() { }

    public static NullNode getInstance() { return sNull; }

    @Override
    public boolean isNull() { return true; }

    public String getValueAsText() {
        return "null";
    }

    public void writeTo(JsonGenerator jg)
        throws IOException, JsonGenerationException
    {
        jg.writeNull();
    }

    public boolean equals(Object o)
    {
        return (o == this);
    }
}
