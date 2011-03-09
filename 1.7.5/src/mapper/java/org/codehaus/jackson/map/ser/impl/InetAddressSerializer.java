package org.codehaus.jackson.map.ser.impl;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.InetAddress;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ser.ScalarSerializerBase;

/**
 * Simple serializer for {@link java.net.InetAddress}. Main complexity is
 * with registration, since same serializer is to be used for sub-classes.
 *
 * @since 1.8
 */
public class InetAddressSerializer
    extends ScalarSerializerBase<InetAddress>
{
    public final static InetAddressSerializer instance = new InetAddressSerializer();
    
    public InetAddressSerializer() { super(InetAddress.class); }

    @Override
    public void serialize(InetAddress value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        // Ok: get textual description; choose "more specific" part
        String str = value.toString().trim();
        int ix = str.indexOf('/');
        if (ix >= 0) {
            if (ix == 0) { // missing host name; use address
                str = str.substring(1);
            } else { // otherwise use name
                str = str.substring(0, ix);
            }
        }
        jgen.writeString(str);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint) {
        return createSchemaNode("string", true);
    }
}
