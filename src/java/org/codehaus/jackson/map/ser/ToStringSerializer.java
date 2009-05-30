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
 * Simple general purpose serializer, useful for any
 * type for which {@link Object#toString} returns the desired Json
 * value.
 */
public final class ToStringSerializer
    extends JsonSerializer<Object> implements SchemaAware
{
    /**
     * Singleton instance to use.
     */
    public final static ToStringSerializer instance = new ToStringSerializer();

    /**
     *<p>
     * Note: usually you should NOT create new instances, but instead use
     * {@link #instance} which is stateless and fully thread-safe. However,
     * there are cases where constructor is needed; for example,
     * when using explicit serializer annotations like
     * {@link org.codehaus.jackson.annotate.JsonSerialize#using}.
     */
    public ToStringSerializer() { }
    
    @Override
	public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeString(value.toString());
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
    {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        objectNode.put("type", "string");
        objectNode.put("optional", true);
        return objectNode;
    }
    
}
