package org.codehaus.jackson.map.ser;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.JsonSerializerProvider;

/**
 * Simple general purpose serializer, useful for any
 * type for which {@link Object#toString} returns the desired Json
 * value.
 */
public final class ToStringSerializer
    extends JsonSerializer<Object>
{
    /**
     * Singleton instance to use.
     */
    public final static ToStringSerializer instance = new ToStringSerializer();

    private ToStringSerializer() { } // no instantiation, use singleton
    
    public void serialize(Object value, JsonGenerator jgen, JsonSerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeString(value.toString());
    }
}
