package org.codehaus.jackson.schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.JsonMappingException;

import java.lang.reflect.Type;

/**
 * Marker interface for schema-aware serializers.
 *
 * @author Ryan Heaton
 */
public interface SchemaAware
{
    /**
     * Get the representation of the schema to which this serializer will conform.
     *
     * @param provider The serializer provider.
     * @param typeHint A hint about the type.
     * @return The {@link http://json-schema.org/ json-schema} for this serializer.
     */
    JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException;
}
