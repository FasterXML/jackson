package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;

/**
 * Class used for namespacing and to contain serializers for misc
 * JDK types that can not use regular {@link ToStringSerializer} or
 * such
 */
public abstract class JdkSerializers
{
    /**
     * Method called by {@link BasicSerializerFactory} to register all
     * serializers contained here.
     */
    static void addAll(HashMap<String, JsonSerializer<?>> sers)
    {
        sers.put(File.class.getName(), new FileSerializer());

        // And then things that 'toString()' can handle
        final ToStringSerializer sls = ToStringSerializer.instance;

        // Other reference types (URLs, URIs)
        sers.put(java.net.URL.class.getName(), sls);
        sers.put(java.net.URI.class.getName(), sls);

        sers.put(Currency.class.getName(), sls);
        sers.put(UUID.class.getName(), sls);
        sers.put(java.util.regex.Pattern.class.getName(), sls);
    }

    /**
     * For now, File objects get serialized by just outputting
     * absolute (but not canonical) name as String value
     */
    public final static class FileSerializer
        extends SerializerBase<File>
    {
        @Override
        public void serialize(File value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.getAbsolutePath());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, java.lang.reflect.Type typeHint)
        {
            return createSchemaNode("string", true);
        }
    }
}


 
