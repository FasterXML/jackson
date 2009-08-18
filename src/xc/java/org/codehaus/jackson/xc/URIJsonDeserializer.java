package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import java.io.IOException;
import java.net.URI;

/**
 * @author Ryan Heaton
 */
public class URIJsonDeserializer
        extends JsonDeserializer<URI>
{

    @Override
    public URI deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        return URI.create(jp.getText());
    }
}