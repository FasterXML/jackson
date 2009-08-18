package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import javax.xml.namespace.QName;
import javax.xml.datatype.Duration;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import java.io.IOException;

/**
 * @author Ryan Heaton
 */
public class DurationJsonDeserializer
        extends JsonDeserializer<Duration>
{

    @Override
    public Duration deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        try {
            return DatatypeFactory.newInstance().newDuration(jp.getText());
        } catch (DatatypeConfigurationException e) {
            throw new JsonParseException("Unable to parse an instance of javax.xml.datatype.Duration",
                    jp.getTokenLocation(), e);
        }
    }
}