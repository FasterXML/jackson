package org.codehaus.jackson.map.ser;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

/**
 * Specialized serializer that can be used as the generic key
 * serializer, when serializing {@link java.util.Map}s to Json
 * Objects.
 */
public final class StdKeySerializer
    extends JsonSerializer<Object>
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
}
