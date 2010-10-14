package org.codehaus.jackson.map.ser;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.atomic.*;

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
    implements Provider<Map.Entry<Class<?>,Object>>
{
    /**
     * Method called by {@link BasicSerializerFactory} to access
     * all serializers this class provides.
     */
    public Collection<Map.Entry<Class<?>, Object>> provide()
    {
        HashMap<Class<?>,Object> sers = new HashMap<Class<?>,Object>();

        // First things that 'toString()' can handle
        final ToStringSerializer sls = ToStringSerializer.instance;

        sers.put(java.net.URL.class, sls);
        sers.put(java.net.URI.class, sls);

        sers.put(Currency.class, sls);
        sers.put(UUID.class, sls);
        sers.put(java.util.regex.Pattern.class, sls);

        // then atomic types
        sers.put(AtomicReference.class, AtomicReferenceSerializer.class);
        sers.put(AtomicBoolean.class, AtomicBooleanSerializer.class);
        sers.put(AtomicInteger.class, AtomicIntegerSerializer.class);
        sers.put(AtomicLong.class, AtomicLongSerializer.class);
        
        // then types that need specialized serializers
        sers.put(File.class, FileSerializer.class);
        sers.put(Class.class, ClassSerializer.class);

        // And then some stranger types... not 100% they are needed but:
        sers.put(Void.TYPE, NullSerializer.class);
        
        return sers.entrySet();
    }

    /*
     ********************************************************
     * Serializers for atomic types
     ********************************************************
     */

    public final static class AtomicBooleanSerializer
        extends SerializerBase<AtomicBoolean>
    {
        public AtomicBooleanSerializer() { super(AtomicBoolean.class, false); }
    
        @Override
        public void serialize(AtomicBoolean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBoolean(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("boolean", true);
        }
    }
    
    public final static class AtomicIntegerSerializer
        extends SerializerBase<AtomicInteger>
    {
        public AtomicIntegerSerializer() { super(AtomicInteger.class, false); }
    
        @Override
        public void serialize(AtomicInteger value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
    }

    public final static class AtomicLongSerializer
        extends SerializerBase<AtomicLong>
    {
        public AtomicLongSerializer() { super(AtomicLong.class, false); }
    
        @Override
        public void serialize(AtomicLong value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.get());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
    }
    
    public final static class AtomicReferenceSerializer
        extends SerializerBase<AtomicReference<?>>
    {
        public AtomicReferenceSerializer() { super(AtomicReference.class, false); }

        @Override
        public void serialize(AtomicReference<?> value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeValue(value.get(), jgen);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("any", true);
        }
    }
    
    /*
    ********************************************************
    * Specialized serializers, referential types
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
