package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.SerializerBase;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.URI;

/**
 * @author Ryan Heaton
 */
public class URIJsonSerializer
        extends SerializerBase<URI>
{

    @Override
    public void serialize(URI value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
    {
        jgen.writeString(value.toString());
    }

    //@Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        return createSchemaNode("string", true);
    }
}