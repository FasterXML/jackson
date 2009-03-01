package org.codeahus.jackson.jaxrs;

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
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.type.TypeFactory;

/**
 * This is a basic implementation of JAX-RS abstractions
 * that are needed for straight-forward and efficient binding of
 * JSON ("application/json") content to and from POJOs.
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public class JsonProvider
    implements
        MessageBodyReader<Object>, MessageBodyWriter<Object>
{
    /*
    ///////////////////////////////////////////////////////
    // Provider objects we use
    ///////////////////////////////////////////////////////
     */

    JsonFactory _jsonFactory;

    ObjectMapper _objectMapper;

    /*
    ///////////////////////////////////////////////////////
    // Provider objects we use
    ///////////////////////////////////////////////////////
     */

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

    /*
    ////////////////////////////////////////////////////
    // MessageBodyReader impl
    ////////////////////////////////////////////////////
     */

    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        /* To know for sure we'd need to find a deserializer; for now,
         * let's claim we can handle anything.
         */
        return (MediaType.APPLICATION_JSON_TYPE.equals(mediaType));
    }
    
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,String> httpHeaders, InputStream entityStream) 
        throws IOException
    {
        JavaType jtype = TypeFactory.fromType(genericType);
        JsonParser jp = _jsonFactory.createJsonParser(entityStream);
        return _objectMapper.readValue(jp, jtype);
    }

    /*
    ////////////////////////////////////////////////////
    // MessageBodyWriter impl
    ////////////////////////////////////////////////////
     */

    public long getSize(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
        return -1;
    }

    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType)
    {
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
}
