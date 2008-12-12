package org.codehaus.jackson.map;

import java.io.IOException;

import org.codehaus.jackson.*;

/**
 * Interface that can be implemented by objects that know how to
 * deserialize themselves from Json content, using {@link JsonParser}
 * (and {@link JsonDeserializerProvider} if necessary).
 * Note that in addition to implement this interface, class also
 * has to provide the default constructor, as an instance has to
 * be constructed before this method can be called.
 *<p>
 * Note that implementing this interface binds implementing object
 * closely to Jackson API, and that it is often not necessary to do
 * so -- if class is a bean, it can be serialized without
 * implementing this interface.
 */
public interface JsonDeserializable
{
    public void deserialize(JsonParser jp, JsonDeserializerProvider provider)
        throws IOException, JsonParseException;
}
