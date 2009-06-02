package org.codehaus.jackson.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.JsonNodeFactory;

/**
 * A {@link org.codehaus.jackson.JsonNode} that represents a JSON-Schema instance.
 *
 * @author Ryan Heaton
 * @see http://json-schema.org/
 */
public class JsonSchema
{

    private final ObjectNode schema;

    public JsonSchema(ObjectNode schema)
    {
        this.schema = schema;
    }

    public ObjectNode getSchemaNode()
    {
        return schema;
    }

    @Override
    public String toString()
    {
        return this.schema.toString();
    }

    @Override
    public boolean equals(Object o)
    {
        return this.schema.equals(o);
    }

    /**
     * Get the default schema node.
     *
     * @return The default schema node.
     */
    public static JsonNode getDefaultSchemaNode()
    {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("type", "any");
        objectNode.put("optional", true);
        return objectNode;
    }

}
