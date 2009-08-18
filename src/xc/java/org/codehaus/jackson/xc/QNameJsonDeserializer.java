package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;

import javax.xml.namespace.QName;
import java.io.IOException;

/**
 * @author Ryan Heaton
 */
public class QNameJsonDeserializer
        extends JsonDeserializer<QName>
{

    @Override
    public QName deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        return QName.valueOf(jp.getText());
    }
}