package org.codehaus.jackson.jaxrs;

import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ObjectMapper;
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
    /*
    ///////////////////////////////////////////////////////
    // Provider objects we use
    ///////////////////////////////////////////////////////
     */

    /**
     * Factory used to construct underlying JSON parsers and generators
     */
    JsonFactory _jsonFactory;

    /**
     * Mapper that is responsible for data binding.
     */
    ObjectMapper _objectMapper;

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
     * Default constructor that is usually used when instances are to
     * be constructed from given class. If so, an instance of
     * {@link ObjectMapper} is constructed with default settings.
     */
    public JacksonJsonProvider()
    {
        this(new ObjectMapper());
    }

    public JacksonJsonProvider(ObjectMapper m)
    {
        _objectMapper = m;
        _jsonFactory = m.getJsonFactory();
        /* note: we have to prevent underlying parser/generator from
         * closing the stream we deal with, so:
         */
        _jsonFactory.disableParserFeature(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        _jsonFactory.disableGeneratorFeature(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
    }

    /*
    ///////////////////////////////////////////////////////
    // Configuring
    ///////////////////////////////////////////////////////
     */

    public ObjectMapper getObjectMapper() { return _objectMapper; }
    public void setObjectMapper(ObjectMapper m) { _objectMapper = m; }

    public void checkCanDeserialize(boolean state) { _cfgCheckCanDeserialize = state; }
    public void checkCanSerialize(boolean state) { _cfgCheckCanSerialize = state; }

    /*
    ////////////////////////////////////////////////////
    // MessageBodyReader impl
    ////////////////////////////////////////////////////
     */

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        // Do we have to verify this here? Just to be safe:
        if (!MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            return false;
        }
        // Also: if we really want to verify that we can serialize, we'll check:
        if (_cfgCheckCanSerialize) {
            if (!_objectMapper.canDeserialize(_convertType(type))) {
                return false;
            }
        }
        return true;
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,String> httpHeaders, InputStream entityStream) 
        throws IOException
    {
        JsonParser jp = _jsonFactory.createJsonParser(entityStream);
        return _objectMapper.readValue(jp, _convertType(genericType));
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
        // Do we have to verify this here? Just to be safe:
        if (!MediaType.APPLICATION_JSON_TYPE.equals(mediaType)) {
            return false;
        }
        // Also: if we really want to verify that we can deserialize, we'll check:
        if (_cfgCheckCanSerialize) {
            if (!_objectMapper.canSerialize(type)) {
                return false;
            }
        }
        /* To know for sure we'd need to find a serializer; for now,
         * let's claim we can handle anything.
         */
        return (MediaType.APPLICATION_JSON_TYPE.equals(mediaType));
    }

    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,Object> httpHeaders, OutputStream entityStream) 
        throws IOException
    {
        /* 27-Feb-2009, tatu: Where can we find desired encoding? Within
         *   http headers?
         */
        JsonGenerator jg = _jsonFactory.createJsonGenerator(entityStream, JsonEncoding.UTF8);
        _objectMapper.writeValue(jg, value);
    }

    /*
    ////////////////////////////////////////////////////
    // Helper methods
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
