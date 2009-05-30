package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * Special bogus "serializer" that will throw
 * {@link JsonGenerationException} if its {@link #serialize}
 * gets invoeked. Most commonly registered as handler for unknown types,
 * as well as for catching unintended usage (like trying to use null
 * as Map/Object key).
 */
public final class FailingSerializer
    extends JsonSerializer<Object>
{
    final String _msg;
    
    public FailingSerializer(String msg) { _msg = msg; }
    
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        throw new JsonGenerationException(_msg);
    }
}
