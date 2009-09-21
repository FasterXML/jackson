package org.codehaus.jackson.node;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.JsonSerializable;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Abstract base class common to all standard {@link JsonNode}
 * implementations.
 * The main addition here is that we declare that sub-classes must
 * implement {@link JsonSerializable}. This simplifies object mapping
 * aspects a bit, as no external serializers are needed.
 */
public abstract class BaseJsonNode
    extends JsonNode
    implements JsonSerializable
{
    protected BaseJsonNode() { }

    public abstract void serialize(JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException;

    /**
     *<p>
     * Note: this method should probably be deprecated, given that we
     * want to use the standard serialization interface.
     */
    @Override
    public final void writeTo(JsonGenerator jgen)
        throws IOException, JsonGenerationException
    {
        /* it's ok to pass null, as long as other nodes handle
         * it properly...
         */
        serialize(jgen, null);
    }

    @Override
    public JsonParser traverse() {
        return new NodeTraversingParser(this);
    }
}

