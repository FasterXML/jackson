package org.codehaus.jackson.map.ser;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

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

    /**
     *<p>
     * Note: usually you should NOT create new instances, but instead use
     * {@link #instance} which is stateless and fully thread-safe. However,
     * there are cases where constructor is needed; for example, when using
     * explicit serializer annotations like {@link org.codehaus.jackson.annotate.JsonUseSerializer}.
     */
    public ToStringSerializer() { }
    
    @Override
	public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeString(value.toString());
    }
}
