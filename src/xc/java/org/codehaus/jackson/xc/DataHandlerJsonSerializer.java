package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.SerializerBase;
import org.codehaus.jackson.node.ObjectNode;

import javax.activation.DataHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

/**
 * @author Ryan Heaton
 */
public class DataHandlerJsonSerializer extends SerializerBase<DataHandler>
{

    @Override
    public void serialize(DataHandler value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024 * 10]; //10k?
        InputStream in = value.getInputStream();
        int len = in.read(buffer);
        while (len < 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        jgen.writeBinary(out.toByteArray());
    }

    //@Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("array", true);
        ObjectNode itemSchema = createSchemaNode("string"); //binary values written as strings?
        o.put("items", itemSchema);
        return o;
    }
}
