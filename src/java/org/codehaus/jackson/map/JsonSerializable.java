package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Interface that can be implemented by objects that know how to
 * serialize themselves to Json, using {@link JsonGenerator}
 * (and {@link JsonSerializerProvider} if necessary).
 *<p>
 * Note that implementing this interface binds implementing object
 * closely to Jackson API, and that it is often not necessary to do
 * so -- if class is a bean, it can be serialized without
 * implementing this interface.
 */
public interface JsonSerializable
{
    public void serialize(JsonGenerator jgen, JsonSerializerProvider provider)
        throws IOException, JsonGenerationException;
}
