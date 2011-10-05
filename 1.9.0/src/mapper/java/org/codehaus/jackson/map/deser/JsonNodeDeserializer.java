package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.node.*;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.JsonNodeDeserializer} instead.
 */
@Deprecated
public class JsonNodeDeserializer
    extends org.codehaus.jackson.map.deser.std.JsonNodeDeserializer
{
    /**
     * Singleton instance of generic deserializer for {@link JsonNode}.
     *
     * @deprecated Use {@link #getDeserializer} accessor instead: will be removed from 2.0
     */
    @Deprecated
    public final static JsonNodeDeserializer instance = new JsonNodeDeserializer();

    /**
     * @deprecated since 1.9.0
     */
    @Deprecated
    protected final ObjectNode deserializeObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return deserializeObject(jp, ctxt, ctxt.getNodeFactory());
    }

    /**
     * @deprecated since 1.9.0
     */
    @Deprecated
    protected final ArrayNode deserializeArray(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return deserializeArray(jp, ctxt, ctxt.getNodeFactory());
    }

    /**
     * @deprecated since 1.9.0
     */
    @Deprecated
    protected final JsonNode deserializeAny(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        return deserializeAny(jp, ctxt, ctxt.getNodeFactory());
    }

}
