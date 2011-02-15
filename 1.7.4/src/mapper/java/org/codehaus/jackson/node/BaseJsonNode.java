package org.codehaus.jackson.node;

import java.io.IOException;
import java.util.List;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

/**
 * Abstract base class common to all standard {@link JsonNode}
 * implementations.
 * The main addition here is that we declare that sub-classes must
 * implement {@link JsonSerializableWithType}.
 * This simplifies object mapping
 * aspects a bit, as no external serializers are needed.
 */
public abstract class BaseJsonNode
    extends JsonNode
    implements JsonSerializableWithType
{
    protected BaseJsonNode() { }

    /*
    /**********************************************************
    /* Basic definitions for non-container types
    /**********************************************************
     */

    @Override
    public JsonNode findValue(String fieldName) {
        return null;
    }

    @Override
    public final JsonNode findPath(String fieldName)
    {
        JsonNode value = findValue(fieldName);
        if (value == null) {
            return MissingNode.getInstance();
        }
        return value;
    }
    
    // note: co-variant return type
    @Override
    public ObjectNode findParent(String fieldName) {
        return null;
    }

    @Override
    public List<JsonNode> findValues(String fieldName, List<JsonNode> foundSoFar) {
        return foundSoFar;
    }

    @Override
    public List<String> findValuesAsText(String fieldName, List<String> foundSoFar) {
        return foundSoFar;
    }
    
    @Override
    public List<JsonNode> findParents(String fieldName, List<JsonNode> foundSoFar) {
        return foundSoFar;
    }
    
    /*
    /**********************************************************
    /* Support for traversal-as-stream
    /**********************************************************
     */

    @Override
    public JsonParser traverse() {
        return new TreeTraversingParser(this);
    }

    /**
     * Method that can be used for efficient type detection
     * when using stream abstraction for traversing nodes.
     * Will return the first {@link JsonToken} that equivalent
     * stream event would produce (for most nodes there is just
     * one token but for structured/container types multiple)
     *
     * @since 1.3
     */
    @Override
    public abstract JsonToken asToken();

    /**
     * @since 1.3
     */
    @Override
    public JsonParser.NumberType getNumberType() {
        // most types non-numeric, so:
        return null; 
    }

    /*
    /**********************************************************
    /* JsonSerializable
    /**********************************************************
     */

    /**
     * Method called to serialize node instances using given generator.
     */
    public abstract void serialize(JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException;

    /**
     * Since JSON node typing is only based on JSON values,
     * there is no need to include type information. So, serialize
     * the same way as when no typing is enabled.
     */
    public void serializeWithType(JsonGenerator jgen, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        serialize(jgen, provider);
    }

    /*
    /**********************************************************
    /* Other
    /**********************************************************
     */

    /**
     *<p>
     * Note: this method is deprecated, given that we
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
}

