package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.schema.SchemaAware;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * Specialized serializer that can be used as the generic key
 * serializer, when serializing {@link java.util.Map}s to Json
 * Objects.
 */
public final class StdKeySerializer
    extends JsonSerializer<Object> implements SchemaAware
{
    final static StdKeySerializer instace = new StdKeySerializer();
    
    @Override
	public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        String keyStr = (value.getClass() == String.class) ?
            ((String) value) : value.toString();
        jgen.writeFieldName(keyStr);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
    {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("type", "string");
        return objectNode;
    }
}
