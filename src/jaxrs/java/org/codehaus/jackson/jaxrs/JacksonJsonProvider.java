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
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.ObjectWriter;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonView;
import org.codehaus.jackson.map.type.ClassKey;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.map.util.JSONPObject;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.VersionUtil;

/**
 * Basic implementation of JAX-RS abstractions ({@link MessageBodyReader},
 * {@link MessageBodyWriter}) needed for binding
 * JSON ("application/json") content to and from Java Objects ("POJO"s).
 *<p>
 * Actual data binding functionality is implemented by {@link ObjectMapper}:
 * mapper to use can be configured in multiple ways:
 * <ul>
 *  <li>By explicitly passing mapper to use in constructor
 *  <li>By explictly setting mapper to use by {@link #setMapper}
 *  <li>By defining JAX-RS <code>Provider</code> that returns {@link ObjectMapper}s.
 *  <li>By doing none of above, in which case a default mapper instance is
 *     constructed (and configured if configuration methods are called)
 * </ul>
 * The last method ("do nothing specific") is often good enough; explicit passing
 * of Mapper is simple and explicit; and Provider-based method may make sense
 * with Depedency Injection frameworks, or if Mapper has to be configured differently
 * for different media types.
 *<p>
 * Note that the default mapper instance will be automatically created if
 * one of explicit configuration methods (like {@link #configure})
 * is called: if so, Provider-based introspection is <b>NOT</b> used, but the
 * resulting Mapper is used as configured.
 *<p>
 * Note: version 1.3 added a sub-class ({@link JacksonJaxbJsonProvider}) which
 * is configured by default to use both Jackson and JAXB annotations for configuration
 * (base class when used as-is defaults to using just Jackson annotations)
 *
 * @author Tatu Saloranta
 */
@Provider
@Consumes({MediaType.APPLICATION_JSON, "text/json"})
@Produces({MediaType.APPLICATION_JSON, "text/json"})
public class JacksonJsonProvider
    implements
        MessageBodyReader<Object>,
        MessageBodyWriter<Object>,
        Versioned // since 1.6
{
    /**
     * Default annotation sets to use, if not explicitly defined during
     * construction: only Jackson annotations are used for the base
     * class. Sub-classes can use other settings.
     */
    public final static Annotations[] BASIC_ANNOTATIONS = {
        Annotations.JACKSON
    };

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

    /**
     * These are classes that we never use for reading
     * (never try to deserialize instances of these types).
     */
    public final static Class<?>[] _unreadableClasses = new Class<?>[] {
        InputStream.class, Reader.class
    };

    /**
     * These are classes that we never use for writing
     * (never try to serialize instances of these types).
     */
    public final static Class<?>[] _unwritableClasses = new Class<?>[] {
        OutputStream.class, Writer.class,
        StreamingOutput.class, Response.class
    };

    /**
     * Helper object used for encapsulating configuration aspects
     * of {@link ObjectMapper}
     */
    protected final MapperConfigurator _mapperConfig;

    /**
     * Set of types (classes) that provider should ignore for data binding
     * 
     * @since 1.5
     */
    protected HashSet<ClassKey> _cfgCustomUntouchables;

    /**
     * JSONP function name to use for automatic JSONP wrapping, if any;
     * if null, no JSONP wrapping is done.
     */
    protected String _jsonpFunctionName;
    
    /*
    /**********************************************************
    /* Context configuration
    /**********************************************************
     */

    /**
     * Injectable context object used to locate configured
     * instance of {@link ObjectMapper} to use for actual
     * serialization.
     */
    @Context
    protected Providers _providers;

    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Whether we want to actually check that Jackson has
     * a serializer for given type. Since this should generally
     * be the case (due to auto-discovery) and since the call
     * to check availability can be bit expensive, defaults to false.
     */
    protected boolean _cfgCheckCanSerialize = false;

    /**
     * Whether we want to actually check that Jackson has
     * a deserializer for given type. Since this should generally
     * be the case (due to auto-discovery) and since the call
     * to check availability can be bit expensive, defaults to false.
     */
    protected boolean _cfgCheckCanDeserialize = false;

    /*
    /**********************************************************
    /* Construction
    /**********************************************************
     */

    /**
     * Default constructor, usually used when provider is automatically
     * configured to be used with JAX-RS implementation.
     */
    public JacksonJsonProvider()
    {
        this(null, BASIC_ANNOTATIONS);
    }

    /**
     * @param annotationsToUse Annotation set(s) to use for configuring
     *    data binding
     */
    public JacksonJsonProvider(Annotations... annotationsToUse)
    {
        this(null, annotationsToUse);
    }

    public JacksonJsonProvider(ObjectMapper mapper)
    {
        this(mapper, BASIC_ANNOTATIONS);
    }
    
    /**
     * Constructor to use when a custom mapper (usually components
     * like serializer/deserializer factories that have been configured)
     * is to be used.
     * 
     * @annotationsToUse Sets of annotations (Jackson, JAXB) that provider should
     *   support
     */
    public JacksonJsonProvider(ObjectMapper mapper, Annotations[] annotationsToUse)
    {
        _mapperConfig = new MapperConfigurator(mapper, annotationsToUse);
    }

    /**
     * Method that will return version information stored in and read from jar
     * that contains this class.
     * 
     * @since 1.6
     */
    @Override
    public Version version() {
        return VersionUtil.versionFor(getClass());
    }
    
    /*
    /**********************************************************
    /* Configuring
    /**********************************************************
     */

    /**
     * Method for defining whether actual detection for existence of
     * a deserializer for type should be done when {@link #isReadable}
     * is called.
     */
    public void checkCanDeserialize(boolean state) { _cfgCheckCanDeserialize = state; }

    /**
     * Method for defining whether actual detection for existence of
     * a serializer for type should be done when {@link #isWriteable}
     * is called.
     */
    public void checkCanSerialize(boolean state) { _cfgCheckCanSerialize = state; }

    /**
     * Method for configuring which annotation sets to use (including none).
     * Annotation sets are defined in order decreasing precedence; that is,
     * first one has the priority over following ones.
     * 
     * @param annotationsToUse Ordered list of annotation sets to use; if null,
     *    default
     */
    public void setAnnotationsToUse(Annotations[] annotationsToUse) {
        _mapperConfig.setAnnotationsToUse(annotationsToUse);
    }
    
    /**
     * Method that can be used to directly define {@link ObjectMapper} to use
     * for serialization and deserialization; if null, will use the standard
     * provider discovery from context instead. Default setting is null.
     */
    public void setMapper(ObjectMapper m) {
        _mapperConfig.setMapper(m);
    }

    public JacksonJsonProvider configure(DeserializationConfig.Feature f, boolean state) {
        _mapperConfig.configure(f, state);
        return this;
    }

    public JacksonJsonProvider configure(SerializationConfig.Feature f, boolean state) {
        _mapperConfig.configure(f, state);
        return this;
    }

    public JacksonJsonProvider configure(JsonParser.Feature f, boolean state) {
        _mapperConfig.configure(f, state);
        return this;
    }

    public JacksonJsonProvider configure(JsonGenerator.Feature f, boolean state) {
        _mapperConfig.configure(f, state);
        return this;
    }

    public JacksonJsonProvider enable(DeserializationConfig.Feature f, boolean state) {
        _mapperConfig.configure(f, true);
        return this;
    }

    public JacksonJsonProvider enable(SerializationConfig.Feature f, boolean state) {
        _mapperConfig.configure(f, true);
        return this;
    }

    public JacksonJsonProvider enable(JsonParser.Feature f, boolean state) {
        _mapperConfig.configure(f, true);
        return this;
    }

    public JacksonJsonProvider enable(JsonGenerator.Feature f, boolean state) {
        _mapperConfig.configure(f, true);
        return this;
    }

    public JacksonJsonProvider disable(DeserializationConfig.Feature f, boolean state) {
        _mapperConfig.configure(f, false);
        return this;
    }

    public JacksonJsonProvider disable(SerializationConfig.Feature f, boolean state) {
        _mapperConfig.configure(f, false);
        return this;
    }

    public JacksonJsonProvider disable(JsonParser.Feature f, boolean state) {
        _mapperConfig.configure(f, false);
        return this;
    }

    public JacksonJsonProvider disable(JsonGenerator.Feature f, boolean state) {
        _mapperConfig.configure(f, false);
        return this;
    }

    /**
     * Method for marking specified type as "untouchable", meaning that provider
     * will not try to read or write values of this type (or its subtypes).
     * 
     * @param type Type to consider untouchable; can be any kind of class,
     *   including abstract class or interface. No instance of this type
     *   (including subtypes, i.e. types assignable to this type) will
     *   be read or written by provider
     * 
     * @since 1.5
     */
    public void addUntouchable(Class<?> type)
    {
        if (_cfgCustomUntouchables == null) {
            _cfgCustomUntouchables = new HashSet<ClassKey>();
        }
        _cfgCustomUntouchables.add(new ClassKey(type));
    }

    public void setJSONPFunctionName(String fname) {
    	this._jsonpFunctionName = fname;
    }
    
    /*
    /**********************************************************
    /* MessageBodyReader impl
    /**********************************************************
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
    @Override
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
        // and there are some other abstract/interface types to exclude too:
        for (Class<?> cls : _unreadableClasses) {
            if (cls.isAssignableFrom(type)) {
                return false;
            }
        }
        // as well as possible custom exclusions
        if (_containedIn(type, _cfgCustomUntouchables)) {
            return false;
        }

        // Finally: if we really want to verify that we can serialize, we'll check:
        if (_cfgCheckCanSerialize) {
            ObjectMapper mapper = locateMapper(type, mediaType);
            if (!mapper.canDeserialize(mapper.constructType(type))) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Method that JAX-RS container calls to deserialize given
     * value.
     */
    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String,String> httpHeaders, InputStream entityStream) 
        throws IOException
    {
        ObjectMapper mapper = locateMapper(type, mediaType);
        JsonParser jp = mapper.getJsonFactory().createJsonParser(entityStream);
        /* Important: we are NOT to close the underlying stream after
         * mapping, so we need to instruct parser:
         */
        jp.disable(JsonParser.Feature.AUTO_CLOSE_SOURCE);
        return mapper.readValue(jp, mapper.constructType(genericType));
    }

    /*
    /**********************************************************
    /* MessageBodyWriter impl
    /**********************************************************
     */

    /**
     * Method that JAX-RS container calls to try to figure out
     * serialized length of given value. Since computation of
     * this length is about as expensive as serialization itself,
     * implementation will return -1 to denote "not known", so
     * that container will determine length from actual serialized
     * output (if needed).
     */
    @Override
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
    @Override
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
        // and finally, may have additional custom types to exclude
        if (_containedIn(type, _cfgCustomUntouchables)) {
            return false;
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
     * Method that JAX-RS container calls to serialize given value.
     */
    @Override
    public void writeTo(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String,Object> httpHeaders, OutputStream entityStream) 
        throws IOException
    {
        /* 27-Feb-2009, tatu: Where can we find desired encoding? Within
         *   HTTP headers?
         */
        ObjectMapper mapper = locateMapper(type, mediaType);
        JsonEncoding enc = findEncoding(mediaType, httpHeaders);
        JsonGenerator jg = mapper.getJsonFactory().createJsonGenerator(entityStream, enc);
        jg.disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

        // Want indentation?
        if (mapper.getSerializationConfig().isEnabled(SerializationConfig.Feature.INDENT_OUTPUT)) {
            jg.useDefaultPrettyPrinter();
        }
        // 04-Mar-2010, tatu: How about type we were given? (if any)
        JavaType rootType = null;
        
        if (genericType != null && value != null) {
            /* 10-Jan-2011, tatu: as per [JACKSON-456], it's not safe to just force root
             *    type since it prevents polymorphic type serialization. Since we really
             *    just need this for generics, let's only use generic type if it's truly
             *    generic.
             */
            if (genericType.getClass() != Class.class) { // generic types are other impls of 'java.lang.reflect.Type'
                /* This is still not exactly right; should root type be further
                 * specialized with 'value.getClass()'? Let's see how well this works before
                 * trying to come up with more complete solution.
                 */
                rootType = mapper.getTypeFactory().constructType(genericType);
                /* 26-Feb-2011, tatu: To help with [JACKSON-518], we better recognize cases where
                 *    type degenerates back into "Object.class" (as is the case with plain TypeVariable,
                 *    for example), and not use that.
                 */
                if (rootType.getRawClass() == Object.class) {
                    rootType = null;
                }
            }
        }
        // [JACKSON-578]: Allow use of @JsonView in resource methods.
        Class<?> viewToUse = null;
        if (annotations != null && annotations.length > 0) {
            viewToUse = _findView(mapper, annotations);
        }
        if (viewToUse != null) {
            // TODO: change to use 'writerWithType' for 2.0 (1.9 could use, but let's defer)
            ObjectWriter viewWriter = mapper.viewWriter(viewToUse);
            // [JACKSON-245] Allow automatic JSONP wrapping
            if (_jsonpFunctionName != null) {
                viewWriter.writeValue(jg, new JSONPObject(this._jsonpFunctionName, value, rootType));
            } else if (rootType != null) {
                // TODO: change to use 'writerWithType' for 2.0 (1.9 could use, but let's defer)
                mapper.typedWriter(rootType).withView(viewToUse).writeValue(jg, value);
            } else {
                viewWriter.writeValue(jg, value);
            }
        } else {
            // [JACKSON-245] Allow automatic JSONP wrapping
            if (_jsonpFunctionName != null) {
                mapper.writeValue(jg, new JSONPObject(this._jsonpFunctionName, value, rootType));
            } else if (rootType != null) {
                // TODO: change to use 'writerWithType' for 2.0 (1.9 could use, but let's defer)
                mapper.typedWriter(rootType).writeValue(jg, value);
            } else {
                mapper.writeValue(jg, value);
            }
        }
    }

    /**
     * Helper method to use for determining desired output encoding.
     * For now, will always just use UTF-8...
     * 
     * @since 1.7.0
     */
    protected JsonEncoding findEncoding(MediaType mediaType, MultivaluedMap<String,Object> httpHeaders)
    {
        return JsonEncoding.UTF8;
    }
    
    /*
    /**********************************************************
    /* Public helper methods
    /**********************************************************
     */

    /**
     * Helper method used to check whether given media type
     * is JSON type or sub type.
     * Current implementation essentially checks to see whether
     * {@link MediaType#getSubtype} returns "json" or something
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
    public ObjectMapper locateMapper(Class<?> type, MediaType mediaType)
    {
        // First: were we configured with a specific instance?
        ObjectMapper m = _mapperConfig.getConfiguredMapper();
        if (m == null) {
            // If not, maybe we can get one configured via context?
            if (_providers != null) {
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
                    m = resolver.getContext(type);
                }
            }
            if (m == null) {
                // If not, let's get the fallback default instance
                m = _mapperConfig.getDefaultMapper();
            }
        }
        return m;
    }

    /*
    /**********************************************************
    /* Private/sub-class helper methods
    /**********************************************************
     */

    protected static boolean _containedIn(Class<?> mainType, HashSet<ClassKey> set)
    {
        if (set != null) {
            ClassKey key = new ClassKey(mainType);
            // First: type itself?
            if (set.contains(key)) return true;
            // Then supertypes (note: will not contain Object.class)
            for (Class<?> cls : ClassUtil.findSuperTypes(mainType, null)) {
                key.reset(cls);
                if (set.contains(key)) return true;
            }
        }
        return false;
    }

    protected Class<?> _findView(ObjectMapper mapper, Annotation[] annotations)
        throws JsonMappingException
    {
        /* Ideally we would try to use AnnotationIntrospector here, but current
         * API expects to get a method/field, not bunch of annotations,
         * so we would need to change it a bit.
         * Let's not bother, then, since this works well for now.
         */
        for (Annotation annotation : annotations) {
            if (annotation.annotationType().isAssignableFrom(JsonView.class)) {
                JsonView jsonView = (JsonView) annotation;
                Class<?>[] views = jsonView.value();
                if (views.length > 1) {
                    StringBuilder s = new StringBuilder("Multiple @JsonView's can not be used on a JAX-RS method. Got ");
                    s.append(views.length).append(" views: ");
                    for (int i = 0; i < views.length; i++) {
                        if (i > 0) {
                            s.append(", ");
                        }
                        s.append(views[i].getName());
                    }
                    throw new JsonMappingException(s.toString());
                }
                return views[0];
            }
        }
        return null;
    }
}
