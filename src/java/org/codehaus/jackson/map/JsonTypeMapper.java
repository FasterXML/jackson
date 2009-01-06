package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;

/**
 * This class provides backwards compatibility to reduce impact
 * of fixing [JACKSON-30].
 *
 * @deprecated Use {@link TreeMapper} directly
 */
public class JsonTypeMapper extends TreeMapper
{
    public JsonTypeMapper() { super(); }

    /**
     * @deprecated Use {@link #readTree(JsonParser)} instead
     */
    public JsonNode read(JsonParser jp)
       throws IOException, JsonParseException, JsonMappingException
    {
    	return readTree(jp);
    }
}
