package org.codehaus.jackson.node;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

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

    @Override
    public abstract JsonToken asToken();

    @Override
    public void serializeWithType(JsonGenerator jg, SerializerProvider provider,
            TypeSerializer typeSer)
        throws IOException, JsonProcessingException
    {
        typeSer.writeTypePrefixForScalar(this, jg);
        serialize(jg, provider);
        typeSer.writeTypeSuffixForScalar(this, jg);
    }
    
    /*
    /**********************************************************************
    /* Public API, path handling
    /**********************************************************************
     */

    @Override
    public JsonNode path(String fieldName) { return MissingNode.getInstance(); }

    @Override
    public JsonNode path(int index) { return MissingNode.getInstance(); }

    /*
    /**********************************************************************
    /* Base impls for standard methods
    /**********************************************************************
     */

    @Override
    public String toString() { return asText(); }
}
