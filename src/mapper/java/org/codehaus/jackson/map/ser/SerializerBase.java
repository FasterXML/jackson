package org.codehaus.jackson.map.ser;

import java.lang.reflect.Type;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.SchemaAware;

/**
 * Base class used by all standard serializers. Provides some convenience
 * methods for implementing {@link SchemaAware}
 */
public abstract class SerializerBase<T>
    extends JsonSerializer<T>
    implements SchemaAware
{
    public abstract JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException;
    
    protected ObjectNode createObjectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
    
    protected ObjectNode createSchemaNode(String type)
    {
        ObjectNode schema = createObjectNode();
        schema.put("type", type);
        return schema;
    }
    
    protected ObjectNode createSchemaNode(String type, boolean isOptional)
    {
        ObjectNode schema = createSchemaNode(type);
        schema.put("optional", isOptional);
        return schema;
    }
}
