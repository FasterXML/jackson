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
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;

/**
 * @author Ryan Heaton
 */
public class XMLGregorianCalendarJsonDeserializer
        extends JsonDeserializer<XMLGregorianCalendar>
{

    @Override
    public XMLGregorianCalendar deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(jp.getText());
        } catch (DatatypeConfigurationException e) {
            throw new JsonParseException("Unable to parse an instance of javax.xml.datatype.XMLGregorianCalendar",
                    jp.getTokenLocation(), e);
        }
    }
}