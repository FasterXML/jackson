package org.codehaus.jackson.jaxrs;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.*;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.ext.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * Basic implementation of JAX-RS abstractions ({@link MessageBodyReader},
 * {@link MessageBodyWriter}) needed for binding
 * JSON ("application/json") content to and from POJOs.
 *<p>
 * Currently most configurability is via caller configuring
 * {@link ObjectMapper} it uses to construct this provider.
 * Additionally it is possible to enable detection of which types
 * can be serialized/deserialized, which is not enabled by default
 * (since it is usually not needed).
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JacksonJsonProvider
    implements
        MessageBodyReader<Object>,
        MessageBodyWriter<Object>
{
    /**
     * 06-Apr-2009, tatu: Looks like we need to worry about accidental
     *   data binding for types we shouldn't be handling. This is
     *   probably not a very good way to do it, but let's start by
     *   blacklisting things we are not to handle.
     *
     *  (why ClassKey? since plain old Class has no hashCode() defined,
     *  lookups are painfully slow)
     */
    public final static HashSet<ClassKey> _untouchables = new HashSet<ClassKey>();
    static {
        // First, I/O things (direct matches)
        _untouchables.add(new ClassKey(java.io.InputStream.class));
        _untouchables.add(new ClassKey(java.io.Reader.class));
        _untouchables.add(new ClassKey(java.io.OutputStream.class));
        _untouchables.add(new ClassKey(java.io.Writer.class));

        // then some primitive types
        _untouchables.add(new ClassKey(byte[].class));
        _untouchables.add(new ClassKey(char[].class));

        // Then core JAX-RS things
        _untouchables.add(new ClassKey(StreamingOutput.class));
        _untouchables.add(new ClassKey(Response.class));
    }

    public final static Class<?>[] _unreadableClasses = new Class<?>[] {
        InputStream.class, Reader.class
            };

    public final static Class<?>[] _unwritableClasses = new Class<?>[] {
        OutputStream.class, Writer.class,
            StreamingOutput.class, Response.class
            };

    /*
    ///////////////////////////////////////////////////////
    // Context configuration
    ///////////////////////////////////////////////////////
     */

    protected final ContextResolver<ObjectMapper> _resolver;

    /*
    ///////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////
     */

    /**
     * Whether return type of type String is to be output
     * as JSON strings (double-quoted) or not. Default to "false",
     * as most often this is not wanted
     */
    protected boolean _cfgSerializeStringAsJSON = false;

    /**
     * Whether we want to actually check that Jackson has
     * a serializer for given type. Since this should generally
     * be the case (due to auto-discovery) and since the call
     * to check this is not free, defaults to false.
     */
    protected boolean _cfgCheckCanSerialize = false;

    /**
     * Whether we want to actually check that Jackson has
     * a deserializer for given type. Since this should generally
     * be the case (due to auto-discovery) and since the call
     * to check this is not free, defaults to false.
     */
    protected boolean _cfgCheckCanDeserialize = false;

    /*
    ///////////////////////////////////////////////////////
    // Construction
    ///////////////////////////////////////////////////////
     */

    /**
     * Default constructor that shouldn't be needed when used it
     * with actul JAX-RS implementation. But it may be needed from
     * tests or such; if so, it will construct appropriate
     * set up to still work as expected.
     */
    public JacksonJsonProvider()
    {
        this(new ObjectMapper());
    }

    /**
     * Constructor to use when a custom mapper (usually components
     * like serializer/deserializer factories that have been configured)
     * is to be used.
     */
    public JacksonJsonProvider(ObjectMapper mapper)
    {
        _resolver = new SingleContextResolver<ObjectMapper>(mapper);
    }

    /**
     * Constructor usually used when configured and wired by
     * IoC system or auto-detection by the JAX-RS implementation.
     */
    public JacksonJsonProvider(@Context Providers providers)
    {
        ContextResolver<ObjectMapper> resolver;

        if (providers == null) {
            resolver = null;
        } else {
            // null -> no filtering by MediaType
            resolver = providers.getContextResolver(ObjectMapper.class, null);
        }
        ObjectMapper mapper;

        if (resolver == null) {
            mapper = new ObjectMapper();
            // If not accessible via injection, let's still create one
            resolver = new SingleContextResolver<ObjectMapper>(mapper);
        }
        _resolver = resolver;
    }

    /*
    ///////////////////////////////////////////////////////
    // Configuring
    ///////////////////////////////////////////////////////
     */

    public void checkCanDeserialize(boolean state) { _cfgCheckCanDeserialize = state; }
    public void checkCanSerialize(boolean state) { _cfgCheckCanSerialize = state; }

    /**
     * Method for enabling/disabling providers conversion of plain old Strings
     * to JSON Strings; affects both input and output data binding.
     */
    public void serializeStringsAsJSON(boolean state) {
        _cfgSerializeStringAsJSON = state;
    }

    /*
    ////////////////////////////////////////////////////
    // MessageBodyReader impl
    ////////////////////////////////////////////////////
     */

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        if (!isJsonType(mediaType)) {
            return false;
        }

        /* Ok: looks like we must weed out some core types here; ones that
         * make no sense to try to bind from JSON:
         */
        if (_untouchables.contains(new ClassKey(type))) {
            return false;
        }
        // but some are interface/abstract classes, so
        for (Class<?> cls : _unreadableClasses) {
            if (cls.isAssignableFrom(type)) {
                return false;
            }
        }

        // Finally: if we really want to verify that we can serialize, we'll check:
        if (_cfgCheckCanSerialize) {
            ObjectMapper mapper = _resolver.getContext(type);
            if (!mapper.canDeserialize(_convertType(type))) {
                return false;
            }
        }
        return true;
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,String> httpHeaders, InputStream entityStream) 
        throws IOException
    {
        ObjectMapper mapper = _resolver.getContext(type);
        JsonParser jp = mapper.getJsonFactory().createJsonParser(entityStream);
        /* Important: we are NOT to close the underlying stream after
         * mapping, so we need to instruct parser:
         */
        jp.disableFeature(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        return mapper.readValue(jp, _convertType(genericType));
    }

    /*
    ////////////////////////////////////////////////////
    // MessageBodyWriter impl
    ////////////////////////////////////////////////////
     */

    public long getSize(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        /* In general figuring output size requires actual writing; usually not
         * worth it to write everything twice.
         */
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        if (!isJsonType(mediaType)) {
            return false;
        }

        /* Ok: looks like we must weed out some core types here; ones that
         * make no sense to try to bind from JSON:
         */
        if (_untouchables.contains(new ClassKey(type))) {
            return false;
        }
        // but some are interface/abstract classes, so
        for (Class<?> cls : _unwritableClasses) {
            if (cls.isAssignableFrom(type)) {
                return false;
            }
        }

        // Also: if we really want to verify that we can deserialize, we'll check:
        if (_cfgCheckCanSerialize) {
            ObjectMapper mapper = _resolver.getContext(type);
            if (!mapper.canSerialize(type)) {
                return false;
            }
        }
        return true;
    }

    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,Object> httpHeaders, OutputStream entityStream) 
        throws IOException
    {
        /* 27-Feb-2009, tatu: Where can we find desired encoding? Within
         *   http headers?
         */
        ObjectMapper mapper = _resolver.getContext(type);
        JsonGenerator jg = mapper.getJsonFactory().createJsonGenerator(entityStream, JsonEncoding.UTF8);
        jg.disableFeature(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        mapper.writeValue(jg, value);
    }

    /*
    ////////////////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////////////////
     */

    protected boolean isJsonType(MediaType mediaType)
    {
        /* As suggested by Stephen D, there are 2 ways to check: either
         * being as inclusive as possible (if subtype is "json"), or
         * exclusive (major type "application", minor type "json").
         * Let's start with inclusive one, hard to know which major
         * types we should cover aside from "application".
         */
        return (mediaType != null) && "json".equals(mediaType.getSubtype());
    }

    /**
     * Method used to construct a JDK generic type into type definition
     * Jackson understands.
     */
    protected JavaType _convertType(Type jdkType)
    {
        return TypeFactory.fromType(jdkType);
    }

    /*
    ////////////////////////////////////////////////////
    // Helper classes
    ////////////////////////////////////////////////////
     */

    /**
     * We need a simple container to use for feeding our ObjectMapper,
     * in case it's not injected from outside.
     */
  final static class SingleContextResolver<T>
      implements ContextResolver<T>
  {
      private final T _singleton;

      public SingleContextResolver(T s) { _singleton = s; }

      public T getContext(Class<?> cls)
      {
          // should we use 'cls' somehow? Shouldn't need to?
          return _singleton;
      }
  }
}
