package org.codehaus.jackson.map;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;

// And then temporary (until 1.0?) support for legacy mapper:
import org.codehaus.jackson.map.legacy.LegacyJavaTypeMapper;

/**
 * This mapper (or, codec) provides for conversions between Java class
 * (JDK provided core classes, beans), and matching JSON constructs.
 * It will use instances of {@link JsonParser} and {@link JsonGenerator}
 * for implementing actual reading/writing of JSON.
 */
public class JavaTypeMapper
// TODO: remove legacy mapper soon
    extends LegacyJavaTypeMapper
{
    /*
    ////////////////////////////////////////////////////
    // Configuration settings
    ////////////////////////////////////////////////////
     */

    /**
     * Factory used to create {@link JsonParser} and {@link JsonGenerator}
     * instances as necessary.
     */
    protected final JsonFactory _jsonFactory;

    /**
     * Object that manages access to serializers used for serialization,
     * including caching.
     * It is configured with {@link #_serializerFactory} for mapping.
     */
    protected JsonSerializerProvider _serializerProvider;

    /**
     * Serializer factory used for constructing serializers serializers.
     */
    protected JsonSerializerFactory _serializerFactory;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    /**
     * Default constructor, which will construct the default
     * {@link JsonFactory} as necessary, use
     * {@link StdSerializerProvider} as its
     * {@link JsonSerializerProvider}, and
     * {@link BeanSerializerFactory} as its
     * {@link JsonSerializerFactory}.
     * This means that it
     * can serialize all standard JDK types, as well as regular
     * Java Beans (based on method names and Jackson-specific annotations),
     * but does not support JAXB annotations.
     */
    public JavaTypeMapper()
    {
        this(null);
    }

    public JavaTypeMapper(JsonFactory jf)
    {
        this(jf, null);
    }

    public JavaTypeMapper(JsonFactory jf, JsonSerializerProvider p)
    {
        _jsonFactory = (jf == null) ? new JsonFactory() : jf;
        _serializerProvider = (p == null) ? new StdSerializerProvider() : p;

        /* Let's just initialize the factory: it's stateless, no need
         * to create anything, no cost to re-set later on
         */
        _serializerFactory = BeanSerializerFactory.instance;
    }

    public void setSerializerFactory(JsonSerializerFactory f) {
        _serializerFactory = f;
    }

    public void setSerializerProvider(JsonSerializerProvider p) {
        _serializerProvider = p;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API: deserialization
    // (mapping from Json to Java types)
    ////////////////////////////////////////////////////
     */

    /**
     * Method to deserialize Json content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     */
    public <T> T readValue(JsonParser jp, Class<T> valueType)
    {
        return null;
    } 

    /*
    ////////////////////////////////////////////////////
    // Public API: serialization
    // (mapping from Java types to Json)
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using provided {@link JsonGenerator}.
     */
    public void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException
    {
        _serializerProvider.serializeValue(jgen, value, _serializerFactory);
        jgen.flush();
    }

    /**
     * Method that can be used to serialize any Java value as
     * Json output, written to File provided.
     */
    public void writeValue(File resultFile, Object value)
        throws IOException, JsonGenerationException
    {
        JsonGenerator jgen = _jsonFactory.createJsonGenerator(resultFile, JsonEncoding.UTF8);
        boolean closed = false;
        try {
            writeValue(jgen, value);
            closed = true;
            jgen.close();
        } finally {
            /* won't try to close twice; also, must catch exception (to it 
             * will not mask exception that is pending)
            */
            if (!closed) {
                try {
                    jgen.close();
                } catch (IOException ioe) { }
            }
        }
    }

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using output stream provided (using encoding
     * {link JsonEncoding#UTF8}).
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(OutputStream out, Object value)
        throws IOException, JsonGenerationException
    {
        JsonGenerator jgen = _jsonFactory.createJsonGenerator(out, JsonEncoding.UTF8);
        boolean closed = false;
        try {
            writeValue(jgen, value);
            closed = true;
            jgen.close();
        } finally {
            if (!closed) {
                jgen.close();
            }
        }
    }

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using Writer provided.
     *<p>
     * Note: method does not close the underlying stream explicitly
     * here; however, {@link JsonFactory} this mapper uses may choose
     * to close the stream depending on its settings (by default,
     * it will try to close it when {@link JsonGenerator} we construct
     * is closed).
     */
    public void writeValue(Writer w, Object value)
        throws IOException, JsonGenerationException
    {
        JsonGenerator jgen = _jsonFactory.createJsonGenerator(w);
        boolean closed = false;
        try {
            writeValue(jgen, value);
            closed = true;
            jgen.close();
        } finally {
            if (!closed) {
                jgen.close();
            }
        }
    }

    /*
    ////////////////////////////////////////////////////
    // !!! TODO: remove
    //
    // Old serialization/de-serialization methods
    ////////////////////////////////////////////////////
     */

    /**
     * Method that will use the current event of the underlying parser
     * (and if there's no event yet, tries to advance to an event)
     * to construct a Java value, and advance the parser to point to the
     * next event, if any.
     * For structured tokens (objects, arrays),
     * will recursively handle and construct contained values.
     *
     * @return Value read and mapped from stream of input events.
     *   Value can be a single value object type (String, Number,
     *   Boolean), null, or structured type (List or Map).
     */
    public Object read(JsonParser jp)
        throws IOException, JsonParseException
    {
        return this.legacyReadAndMap(jp);
    }

    /**
     *<p>
     * Note: method will explicitly call flush on underlying
     * generator.
     *<p>
     * @deprecated Use {@link #writeValue}
     */
    public void writeAny(JsonGenerator jg, Object value)
        throws IOException, JsonGenerationException
    {
        this.legacyWriteAny(jg, value);
        jg.flush();
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, exposing Java constructs as JSON
    // event source via JSONParser
    ////////////////////////////////////////////////////
     */

    /**
     * Method that will take in a Java object that could have
     * been created by mappers write methods, and construct
     * a {@link JsonParser} that exposes contents as JSON
     * tokens
     */
    /*
    public JsonParser createParserFor(Object data)
        throws JsonParseException
    {
        // !!! TBI: parser for reading from Object (array/map, primitives)
        return null;
    }
    */

    /**
     * Method that will create a JSON generator that will build
     * Java objects as members of the current list, appending
     * them at the end of the list.
     */
    /*
    public JsonGenerator createGeneratorFor(List<?> context)
        throws JsonGenerationException
    {
        // !!! TBI: generator for writing (appending) to Json Arrays (Java lists)
        return null;
    }
    */

    /*
    public JsonGenerator createGeneratorFor(Map<?,?> context)
        throws JsonParseException
    {
        // !!! TBI: generator for writing (appending) to Json Objects (Java maps)
        return null;
    }
    */
}

