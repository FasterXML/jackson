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
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
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
 *
 * @author Tatu Saloranta
 */
@Provider
    @Consumes({MediaType.APPLICATION_JSON, "text/json"})
    @Produces({MediaType.APPLICATION_JSON, "text/json"})
public class JacksonJsonProvider
    implements
        MessageBodyReader<Object>,
        MessageBodyWriter<Object>
{
    /**
     * Looks like we need to worry about accidental
     *   data binding for types we shouldn't be handling. This is
     *   probably not a very good way to do it, but let's start by
     *   blacklisting things we are not to handle.
     *<p>
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
        // 24-Apr-2009, tatu: String is an edge case... let's leave it out
        _untouchables.add(new ClassKey(String.class));

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

    /**
     * Default {@link ObjectMapper} to use in absence of any
     * configured (direct or indirect) mapper. Created lazily
     * if (and only if) it is needed.
     */
    protected ObjectMapper _defaultMapper;

    /*
    ///////////////////////////////////////////////////////
    // Context configuration
    ///////////////////////////////////////////////////////
     */

    /**
     * Injectable context object used to locate configured
     * instance of {@link ObjectMapper} to use for actual
     * serialization.
     */
    @Context
    protected Providers _providers;

    /**
     * Mapper provider was constructed with if any; if null, provider
     * is dynamically located; and if that fails, default instance
     * will be used.
     */
    protected ObjectMapper _configuredMapper;

    /*
    ///////////////////////////////////////////////////////
    // Configuration
    ///////////////////////////////////////////////////////
     */

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
     * Default constructor, usually used when provider is automatically
     * configured to be used with JAX-RS implementation.
     */
    public JacksonJsonProvider()
    {
        this(null);
    }

    /**
     * Constructor to use when a custom mapper (usually components
     * like serializer/deserializer factories that have been configured)
     * is to be used.
     */
    public JacksonJsonProvider(ObjectMapper mapper)
    {
        _configuredMapper = mapper;
    }

    /*
    ///////////////////////////////////////////////////////
    // Configuring
    ///////////////////////////////////////////////////////
     */

    /**
     * Method for defining whether {@link 
     */
    public void checkCanDeserialize(boolean state) { _cfgCheckCanDeserialize = state; }
    public void checkCanSerialize(boolean state) { _cfgCheckCanSerialize = state; }

    /**
     * Method that can be used to directly define {@link ObjectMapper} to use
     * for serialization and deserialization; if null, will use the standard
     * provider discovery from context instead. Default setting is null.
     */
    public void setMapper(ObjectMapper m) {
        _configuredMapper = m;
    }

    /**
     * Method that can be used to access configured mapper; and if
     * one has not yet been specified (by call to {@link #setMapper}),
     * construct an instance and set it by calling {@link #setMapper}.
     *<p>
     * Note that construction of such mapper means that
     * no auto-detection will occur: that is, context is
     * not checked for auto-wired {@link ObjectMapper}.
     */
    protected ObjectMapper configuredMapper() {
        if (_configuredMapper == null) {
            _configuredMapper = new ObjectMapper();
        }
        return _configuredMapper;
    }

    public JacksonJsonProvider configure(DeserializationConfig.Feature f, boolean state) {
        configuredMapper().configure(f, state);
        return this;
    }

    public JacksonJsonProvider configure(SerializationConfig.Feature f, boolean state) {
        configuredMapper().configure(f, state);
        return this;
    }

    public JacksonJsonProvider configure(JsonParser.Feature f, boolean state) {
        configuredMapper().configure(f, state);
        return this;
    }

    public JacksonJsonProvider configure(JsonGenerator.Feature f, boolean state) {
        configuredMapper().configure(f, state);
        return this;
    }

    public JacksonJsonProvider enable(DeserializationConfig.Feature f, boolean state) {
        configuredMapper().configure(f, true);
        return this;
    }

    public JacksonJsonProvider enable(SerializationConfig.Feature f, boolean state) {
        configuredMapper().configure(f, true);
        return this;
    }

    public JacksonJsonProvider enable(JsonParser.Feature f, boolean state) {
        configuredMapper().configure(f, true);
        return this;
    }

    public JacksonJsonProvider enable(JsonGenerator.Feature f, boolean state) {
        configuredMapper().configure(f, true);
        return this;
    }

    public JacksonJsonProvider disable(DeserializationConfig.Feature f, boolean state) {
        configuredMapper().configure(f, false);
        return this;
    }

    public JacksonJsonProvider disable(SerializationConfig.Feature f, boolean state) {
        configuredMapper().configure(f, false);
        return this;
    }

    public JacksonJsonProvider disable(JsonParser.Feature f, boolean state) {
        configuredMapper().configure(f, false);
        return this;
    }

    public JacksonJsonProvider disable(JsonGenerator.Feature f, boolean state) {
        configuredMapper().configure(f, false);
        return this;
    }

    /*
    ////////////////////////////////////////////////////
    // MessageBodyReader impl
    ////////////////////////////////////////////////////
     */

    /**
     * Method that JAX-RS container calls to try to check whether
     * values of given type (and media type) can be deserialized by
     * this provider.
     * Implementation will first check that expected media type is
     * a JSON type (via call to {@link #isJsonType}; then verify
     * that type is not one of "untouchable" types (types we will never
     * automatically handle), and finally that there is a deserializer
     * for type (iff {@link #checkCanDeserialize} has been called with
     * true argument -- otherwise assumption is there will be a handler)
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
            if (!locateMapper(type, mediaType).canDeserialize(_convertType(type))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Method that JAX-RS container calls to deserialize given
     * value.
     */
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,String> httpHeaders, InputStream entityStream) 
        throws IOException
    {
        ObjectMapper mapper = locateMapper(type, mediaType);
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

    /**
     * Method that JAX-RS container calls to try to figure out
     * serialized length of given value. Since computation of
     * this length is about as expensive as serialization itself,
     * implementation will return -1 to denote "not known", so
     * that container will determine length from actual serialized
     * output (if needed).
     */
    public long getSize(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        /* In general figuring output size requires actual writing; usually not
         * worth it to write everything twice.
         */
        return -1;
    }

    /**
     * Method that JAX-RS container calls to try to check whether
     * given value (of specified type) can be serialized by
     * this provider.
     * Implementation will first check that expected media type is
     * a JSON type (via call to {@link #isJsonType}; then verify
     * that type is not one of "untouchable" types (types we will never
     * automatically handle), and finally that there is a serializer
     * for type (iff {@link #checkCanSerialize} has been called with
     * true argument -- otherwise assumption is there will be a handler)
     */
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
            if (!locateMapper(type, mediaType).canSerialize(type)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Method that JAX-RS container calls to serialize given
     * value.
     */
    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,Object> httpHeaders, OutputStream entityStream) 
        throws IOException
    {
        /* 27-Feb-2009, tatu: Where can we find desired encoding? Within
         *   http headers?
         */
        ObjectMapper mapper = locateMapper(type, mediaType);
        JsonGenerator jg = mapper.getJsonFactory().createJsonGenerator(entityStream, JsonEncoding.UTF8);
        jg.disableFeature(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

        // Want indentation?
        if (mapper.getSerializationConfig().isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
            jg.useDefaultPrettyPrinter();
        }
        mapper.writeValue(jg, value);
    }

    /*
    ////////////////////////////////////////////////////
    // Public helper methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method called to locate {@link ObjectMapper} to use for serialization
     * and deserialization. If an instance has been explicitly defined by
     * {@link #setMapper} (or non-null instance passed in constructor), that
     * will be used. 
     * If not, will try to locate it using standard JAX-RS
     * {@link ContextResolver} mechanism, if it has been properly configured
     * to access it (by JAX-RS runtime).
     * Finally, if no mapper is found, will return a default unconfigured
     * {@link ObjectMapper} instance (one constructed with default constructor
     * and not modified in any way)
     *
     * @param type Class of object being serialized or deserialized;
     *   not checked at this point, since it is assumed that unprocessable
     *   classes have been already weeded out,
     *   but will be passed to {@link ContextResolver} as is.
     * @param mediaType Declared media type for the instance to process:
     *   not used by this method,
     *   but will be passed to {@link ContextResolver} as is.
     */
    protected ObjectMapper locateMapper(Class<?> type, MediaType mediaType)
    {
        // First: were we configured with an instance?
        if (_configuredMapper != null) {
            return _configuredMapper;
        }

        // If not, maybe we can get one configured via context?
        ContextResolver<ObjectMapper> resolver = _providers.getContextResolver(ObjectMapper.class, mediaType);
        /* Above should work as is, but due to this bug
         *   [https://jersey.dev.java.net/issues/show_bug.cgi?id=288]
         * in Jersey, it doesn't. But this works until resolution of
         * the issue:
         */
        if (resolver == null) {
            resolver = _providers.getContextResolver(ObjectMapper.class, null);
        }
        if (resolver != null) {
            ObjectMapper mapper = resolver.getContext(type);
            if (mapper != null) {
                return mapper;
            }
        }
        /* nope: must use an unconfigured default instance
         * (ideally we'd log a warning if this happens...)
         */
        synchronized (this) {
            if (_defaultMapper == null) {
                _defaultMapper = new ObjectMapper();
            }
            return _defaultMapper;
        }
    }

    /**
     * Helper method used to check whether given media type
     * is JSON type or sub type.
     * Current implementation essentially checks to see whether
     * {@link MediaType#getSubType} returns "json" or something
     * ending with "+json".
     */
    protected boolean isJsonType(MediaType mediaType)
    {
        /* As suggested by Stephen D, there are 2 ways to check: either
         * being as inclusive as possible (if subtype is "json"), or
         * exclusive (major type "application", minor type "json").
         * Let's start with inclusive one, hard to know which major
         * types we should cover aside from "application".
         */
        if (mediaType != null) {
            // Ok: there are also "xxx+json" subtypes, which count as well
            String subtype = mediaType.getSubtype();
            return "json".equalsIgnoreCase(subtype) || subtype.endsWith("+json");
        }
        /* Not sure if this can happen; but it seems reasonable
         * that we can at least produce json without media type?
         */
        return true;
    }

    /*
    ////////////////////////////////////////////////////
    // Private/sub-class helper methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method used to construct a JDK generic type into type definition
     * Jackson understands.
     */
    protected JavaType _convertType(Type jdkType)
    {
        return TypeFactory.fromType(jdkType);
    }
}
