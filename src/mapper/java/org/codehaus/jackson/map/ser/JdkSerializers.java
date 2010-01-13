package org.codehaus.jackson.map.ser;

import java.io.*;
import java.lang.reflect.Type;
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

        // First things that 'toString()' can handle
        final ToStringSerializer sls = ToStringSerializer.instance;

        sers.put(java.net.URL.class, sls);
        sers.put(java.net.URI.class, sls);

        sers.put(Currency.class, sls);
        sers.put(UUID.class, sls);
        sers.put(java.util.regex.Pattern.class, sls);

        // then types that need specialized serializers
        sers.put(File.class, new FileSerializer());
        sers.put(Class.class, new ClassSerializer());

        // And then some stranger types... not 100% they are needed but:
        sers.put(Void.TYPE, NullSerializer.instance);

        return sers.entrySet();
    }

    /*
    ********************************************************
    * Specialized serializers
    ********************************************************
     */

    /**
     * For now, File objects get serialized by just outputting
     * absolute (but not canonical) name as String value
     */
    public final static class FileSerializer
        extends SerializerBase<File>
    {
        public FileSerializer() { super(File.class); }

        @Override
        public void serialize(File value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.getAbsolutePath());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("string", true);
        }
    }

    /**
     * Also: default bean access will not do much good with Class.class. But
     * we can just serialize the class name and that should be enough.
     */
    @SuppressWarnings("unchecked")
    public final static class ClassSerializer
        extends SerializerBase<Class>
    {
        public ClassSerializer() { super(Class.class); }

        @Override
        public void serialize(Class value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.getName());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            return createSchemaNode("string", true);
        }
    }
}
