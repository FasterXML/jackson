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
    extends BaseMapper
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
     * {@link BeanSerializerFacotyr} as its
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

    public JavaTypeMapper(JsonFactory jf, JsonSerializerProvider sp)
    {
        this(jf, sp, null);
    }

    public JavaTypeMapper(JsonFactory jf, JsonSerializerProvider p, JsonSerializerFactory jsf)
    {
        _jsonFactory = (jf == null) ? new JsonFactory() : jf;
        _serializerProvider = (p == null) ? new StdSerializerProvider() : p;
        _serializerFactory = (jsf == null) ? BeanSerializerFactory.instance : jsf;
    }

    public void setSerializerFactory(JsonSerializerFactory f) {
        _serializerFactory = f;
    }

    public void setSerializerProvider(JsonSerializerProvider p) {
        _serializerProvider = p;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, root-level mapping methods,
    // mapping from JSON to Java types
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
        JsonToken curr = jp.getCurrentToken();
        if (curr == null) {
            curr  = jp.nextToken();
            // We hit EOF? Nothing more to do, if so:
            if (curr == null) {
                return null;
            }
        }
        Object result = _readAndMap(jp, curr);
        /* Need to also advance the reader, if we get this far,
         * to allow handling of root level sequence of values
         */
        jp.nextToken();
        return result;
    }

    /*
    ////////////////////////////////////////////////////
    // Public API, root-level mapping methods,
    // mapping from Java types to JSON
    ////////////////////////////////////////////////////
     */

    /**
     * Method that can be used to serialize any Java value as
     * Json output, using provided {@link JsonGenerator}.
     */
    public final void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException
    {
        _serializerProvider.serializeValue(jgen, value, _serializerFactory);
        jgen.flush();
    }

    /**
     * Method that can be used to serialize any Java value as
     * Json output, written to File provided.
     */
    public final void writeValue(File resultFile, Object value)
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
    public final void writeValue(OutputStream out, Object value)
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
    public final void writeValue(Writer w, Object value)
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
    // Old serialization methods
    ////////////////////////////////////////////////////
     */

    final static LegacyJavaTypeMapper _legacyMapper = new LegacyJavaTypeMapper();

    /**
     *<p>
     * Note: method will explicitly call flush on underlying
     * generator.
     *<p>
     * @deprecated Use {@link #writeValue}
     */
    public final void writeAny(JsonGenerator jg, Object value)
        throws IOException, JsonGenerationException
    {
        _legacyMapper.writeAny(jg, value);
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

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */

    protected Object _readAndMap(JsonParser jp, JsonToken currToken)
        throws IOException, JsonParseException
    {
        switch (currToken) {
        case START_OBJECT:
            {
                LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();

                while ((currToken = jp.nextToken()) != JsonToken.END_OBJECT) {
                    if (currToken != JsonToken.FIELD_NAME) {
                        _reportProblem(jp, "Unexpected token ("+currToken+"), expected FIELD_NAME");
                    }
                    String fieldName = jp.getText();
                    Object  value = _readAndMap(jp, jp.nextToken());

                    if (_cfgDupFields == DupFields.ERROR) {
                        Object old = result.put(fieldName, value);
                        if (old != null) {
                            _reportProblem(jp, "Duplicate value for field '"+fieldName+"', when dup fields mode is "+_cfgDupFields);
                        }
                    } else if (_cfgDupFields == DupFields.USE_LAST) {
                        // Easy, just add
                        result.put(fieldName, value);
                    } else { // use first; need to ensure we don't yet have it
                        if (!result.containsKey(fieldName)) {
                            result.put(fieldName, value);
                        }
                    }
                }
                return result;
            }

        case START_ARRAY:
            {
                ArrayList<Object> result = new ArrayList<Object>();
                while ((currToken = jp.nextToken()) != JsonToken.END_ARRAY) {
                    Object value = _readAndMap(jp, currToken);
                    result.add(value);
                }
                return result;
            }

        case VALUE_STRING:
            return jp.getText();

        case VALUE_NUMBER_INT:
        case VALUE_NUMBER_FLOAT:
            return jp.getNumberValue();

        case VALUE_TRUE:
            return Boolean.TRUE;

        case VALUE_FALSE:
            return Boolean.FALSE;

        case VALUE_NULL:
            return null;

            /* These states can not be mapped; input stream is
             * off by an event or two
             */

        case FIELD_NAME:
        case END_OBJECT:
        case END_ARRAY:
            _reportProblem(jp, "Can not map token "+currToken+": stream off by a token or two?");

        default: // sanity check, should never happen
            _throwInternal("Unrecognized event type: "+currToken);
            return null; // never gets this far
        }
    }
}

