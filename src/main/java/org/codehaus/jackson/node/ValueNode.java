package org.codehaus.jackson.node;

import org.codehaus.jackson.JsonNode;

/**
 * This intermediate base class is used for all leaf nodes, that is,
 * all non-container (array or object) nodes, except for the
 * "missing node".
 */
public abstract class ValueNode
    extends BaseJsonNode
{
    protected ValueNode() { }

    @Override
    public boolean isValueNode() { return true; }

    /*
    ////////////////////////////////////////////////////
    // Public API, path handling
    ////////////////////////////////////////////////////
     */

    @Override
    public JsonNode path(String fieldName) { return MissingNode.getInstance(); }

    @Override
    public JsonNode path(int index) { return MissingNode.getInstance(); }

    /*
    ////////////////////////////////////////////////////
    // Base impls for standard methods
    ////////////////////////////////////////////////////
     */

    @Override
    public String toString() { return getValueAsText(); }
}
