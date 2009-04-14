package org.codehaus.jackson.node;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * This singleton node class is generated to denote "missing nodes"
 * along paths that do not exist. For example, if a path via
 * element of an array is requested for an element outside range
 * of elements in the array; or for a non-array value, result
 * will be reference to this node.
 */
public final class MissingNode
    extends BaseJsonNode
{
    private final static MissingNode instance = new MissingNode();

    private MissingNode() { }

    public static MissingNode getInstance() { return instance; }

    @Override
    public boolean isMissingNode() { return true; }

    @Override
    public String getValueAsText() { return null; }

    @Override
    public JsonNode path(String fieldName) { return this; }

    @Override
    public JsonNode path(int index) { return this; }

    @Override
    public final void serialize(JsonGenerator jg, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        /* Nothing to output... should we signal an error tho?
         * Chances are, this is an erroneous call. For now, let's
         * not do that.
         */
    }

    public boolean equals(Object o)
    {
        /* Hmmh. Since there's just a singleton instance, this
         * fails in all cases but with identity comparison.
         * However: if this placeholder value was to be considered
         * similar to Sql NULL, it shouldn't even equal itself?
         * That might cause problems when dealing with collections
         * like Sets... so for now, let's let identity comparison
         * return true.
         */
        return (o == this);
    }

    @Override
    public String toString()
    {
        // toString() should never return null
        return "";
    }
}
