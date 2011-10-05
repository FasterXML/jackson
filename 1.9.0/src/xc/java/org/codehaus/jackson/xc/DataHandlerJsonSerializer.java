package org.codehaus.jackson.xc;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;

import javax.activation.DataHandler;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.std.SerializerBase;
import org.codehaus.jackson.node.ObjectNode;

/**
 * @author Ryan Heaton
 */
public class DataHandlerJsonSerializer extends SerializerBase<DataHandler>
{
    public DataHandlerJsonSerializer() { super(DataHandler.class); }
    
    @Override
    public void serialize(DataHandler value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonProcessingException
    {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        /* for copy-through, a small buffer should suffice: ideally
         * we might want to reuse a generic byte buffer, but for now
         * there's no serializer context to hold them.
         * 
         * Also: it'd be nice not to have buffer all data, but use a
         * streaming output. But currently JsonGenerator won't allow
         * that.
         */
        byte[] buffer = new byte[1024 * 4]; //10k?
        InputStream in = value.getInputStream();
        int len = in.read(buffer);
        while (len > 0) {
            out.write(buffer, 0, len);
            len = in.read(buffer);
        }
        jgen.writeBinary(out.toByteArray());
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        ObjectNode o = createSchemaNode("array", true);
        ObjectNode itemSchema = createSchemaNode("string"); //binary values written as strings?
        o.put("items", itemSchema);
        return o;
    }
}
