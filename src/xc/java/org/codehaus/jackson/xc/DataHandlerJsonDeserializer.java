package org.codehaus.jackson.xc;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayInputStream;

/**
 * @author Ryan Heaton
 */
public class DataHandlerJsonDeserializer
        extends JsonDeserializer<DataHandler>
{

    @Override
    public DataHandler deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        final byte[] value = jp.getBinaryValue();
        return new DataHandler(new DataSource()
        {
            @Override
            public InputStream getInputStream()
                    throws IOException
            {
                return new ByteArrayInputStream(value);
            }

            @Override
            public OutputStream getOutputStream()
                    throws IOException
            {
                throw new IOException();
            }

            @Override
            public String getContentType()
            {
                return "application/octet-stream";
            }

            @Override
            public String getName()
            {
                return "json-binary-data";
            }
        });
    }
}
