package org.codehaus.jackson.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.annotate.JsonValue;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.JsonNodeFactory;

/**
 * A {@link org.codehaus.jackson.JsonNode} that represents a JSON-Schema instance.
 *
 * @author Ryan Heaton
 * @see <a href="http://json-schema.org/>JSON Schema</a>
 */
public class JsonSchema
{
    private final ObjectNode schema;

    public JsonSchema(ObjectNode schema)
    {
        this.schema = schema;
    }

    /**
     *<p>
     * Note: this method is specified with {@link JsonValue} annotation
     * to represent serialization to use; same as if explicitly
     * serializing returned object.
     *
     * @return Root node of the schema tree
     */
    @JsonValue
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
