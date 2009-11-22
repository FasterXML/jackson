package org.codehaus.jackson.map.ser;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.util.Provider;

/**
 * Class used for namespacing and to contain serializers for misc
 * JDK types that can not use regular {@link ToStringSerializer} or
 * such
 */
public class JdkSerializers
    implements Provider<Map.Entry<Class<?>,JsonSerializer<?>>>
{
    /**
     * Method called by {@link BasicSerializerFactory} to access
     * all serializers this class provides.
     */
    public Collection<Map.Entry<Class<?>, JsonSerializer<?>>> provide()
    {
        HashMap<Class<?>,JsonSerializer<?>> sers = new HashMap<Class<?>,JsonSerializer<?>>();
        sers.put(File.class, new FileSerializer());

        // And then things that 'toString()' can handle
        final ToStringSerializer sls = ToStringSerializer.instance;

        // Other reference types (URLs, URIs)
        sers.put(java.net.URL.class, sls);
        sers.put(java.net.URI.class, sls);

        sers.put(Currency.class, sls);
        sers.put(UUID.class, sls);
        sers.put(java.util.regex.Pattern.class, sls);

        return sers.entrySet();
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


 
