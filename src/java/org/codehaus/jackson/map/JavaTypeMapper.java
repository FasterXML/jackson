package org.codehaus.jackson.map;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.*;

/**
 * This mapper (or, codec) provides for conversions between core
 * JDK-defined Java types, and matching JSON constructs.
 * It will use instances of {@link JsonParser} and {@link JsonGenerator}
 * for implementing actual reading/writing of JSON.
 *<p>
 * In addition to mapping to/from textual JSON serialization using
 * json parser and generator, mapper can also expose resulting
 * Java containers via a parser or generator: either as the source
 * of JSON events, or as target so that java objects can be
 * constructed from calls to json generator.
 */
public class JavaTypeMapper
    extends BaseMapper
    implements JavaTypeSerializer<Object>
{
    /*
    ////////////////////////////////////////////////////
    // Public enums for configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Enumeration that defines strategies available for dealing with
     * unknown Java object types (when mapping java objects to JSON)
     */
    public enum UnknownType {
        /**
         * This option defines that if a type is not recognized a
         * {@link JsonGenerationException} is to be thrown
         */
        ERROR
            /**
             * This option means that if a type is not recognized,
             * objects {@link Object#toString} method will be called
             * and output will be done as JSON string.
             */
            ,OUTPUT_USING_TO_STRING /* default */
    }

    /*
    ////////////////////////////////////////////////////
    // Configuration settings
    ////////////////////////////////////////////////////
     */

    /**
     * Optional custom serializer, which can be called to handle
     * Java types that the default handler can not handle.
     * If set, it will be called for the types that the default
     * serialization mechanism does not know how to explicitly
     * deal with (i.e  not including possible eventual conversion
     * to String, as per {@link #_cfgUnknownTypes} )
     */
    protected JavaTypeSerializer<Object> mCustomSerializer = null;

    /**
     * This defines how instances of unrecognized types (for JSON output)
     * are to be handled. Default is to call <b>toString()</b> on such
     * objects, and output result as String.
     */
    protected UnknownType _cfgUnknownTypes = UnknownType.OUTPUT_USING_TO_STRING;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle (construction, configuration)
    ////////////////////////////////////////////////////
     */

    public JavaTypeMapper() { }

    /**
     * Method for specifying a custom type serializer to use when mapping
     * JSON content to Java objects.
     */
    public void setCustomSerializer(JavaTypeSerializer<Object> ser) { mCustomSerializer = ser; }
    public JavaTypeSerializer<Object> getCustomSerializer() { return mCustomSerializer; }

    /**
     * Method for configuring mapper regarding how to handle serialization
     * of types it does not recognize.
     */
    public void setUnkownTypeHandling(UnknownType mode) { _cfgUnknownTypes = mode; }
    public UnknownType getUnkownTypeHandling() { return _cfgUnknownTypes; }

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
     *<p>
     * Note: method will explicitly call flush on underlying
     * generator.
     */
    public final void writeAny(JsonGenerator jg, Object value)
        throws IOException, JsonParseException
    {
        writeAny(this, jg, value);
        jg.flush();
    }

    /**
     *<p>
     * Note: the reason for using untyped map (instead of one with
     *  key type of String) is to
     * allow things like Enums as keys -- anything convertible
     * to String will be ok for us.
     *<p>
     * Note: method will explicitly call flush on underlying
     * generator.
     */
    public final void write(JsonGenerator jg, Map<?,Object> value)
        throws IOException, JsonParseException
    {
        writeValue(this, jg, value);
        jg.flush();
    }

    /**
     *<p>
     * Note: method will explicitly call flush on underlying
     * generator.
     */
    public final void write(JsonGenerator jg, Collection<Object> value)
        throws IOException, JsonParseException
    {
        writeValue(this, jg, value);
        jg.flush();
    }

    /**
     *<p>
     * Note: method will explicitly call flush on underlying
     * generator.
     */
    public final void write(JsonGenerator jg, Object[] value)
        throws IOException, JsonParseException
    {
        writeValue(this, jg, value);
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
        throws JsonParseException
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
    // JavaTypeSerializer implementation
    ////////////////////////////////////////////////////
     */

    /**
     * Implementation of the generic write method required by
     * {@link JavaTypeSerializer}.
     *<p>
     * Note: since this is not the 'root' method of mapper, it will NOT
     * explicitly flush the underlying generator after serializing
     * passed object.
     */
    //@SuppressWarnings("unchecked")
    public final boolean writeAny(JavaTypeSerializer<Object> defaultSerializer,
                                  JsonGenerator jgen, Object value)
        throws IOException, JsonParseException
    {
        if (value == null) {
            jgen.writeNull();
            return true;
        }
        // Perhaps it's one of common core JDK types?
        KnownClasses.JdkClasses jdkType = KnownClasses.findTypeFast(value);
        if (jdkType == null) {
            // If not, maybe we have an auxiliary converter?
            if (mCustomSerializer != null) {
                if (mCustomSerializer.writeAny(defaultSerializer, jgen, value)) {
                    return true;
                }
            }
            // And if not, maybe we can further introspect the type
            jdkType = KnownClasses.findTypeSlow(value);

            if (jdkType == null) {
                // Nope, can't figure it out. Error or toString();
                if (_cfgUnknownTypes == UnknownType.ERROR) {
                    throw new JsonGenerationException("Unknown type ("+value.getClass().getName()+"): don't know how to handle");
                }
                jgen.writeString(value.toString());
                return true;
            }
        }

        // Yes, there is a generic conversion available:
        switch (jdkType) {
        case BOOLEAN:
            jgen.writeBoolean(((Boolean) value).booleanValue());
            break;
        case STRING:
        case STRING_LIKE:
            jgen.writeString(value.toString());
            break;
        case NUMBER_INTEGER:
            jgen.writeNumber(((Number) value).intValue());
            break;
        case NUMBER_LONG:
            jgen.writeNumber(((Number) value).longValue());
            break;
        case NUMBER_DOUBLE:
            jgen.writeNumber(((Number) value).doubleValue());
            break;
        case NUMBER_OTHER:
            /* Could try figuring out exact type etc. etc., but we
             * are probably best off by just asking object to serialize
             * itself and assume that's good:
             */
            jgen.writeNumber(value.toString());
            break;

            // // // Then array types:
            
        case ARRAY_LONG:
            jgen.writeStartArray();
            {
                long[] values = (long[]) value;
                for (int i = 0, len = values.length; i < len; ++i) {
                    jgen.writeNumber(values[i]);
                }
            }
            jgen.writeEndArray();
            break;
        case ARRAY_INT:
            jgen.writeStartArray();
            {
                int[] values = (int[]) value;
                for (int i = 0, len = values.length; i < len; ++i) {
                    jgen.writeNumber(values[i]);
                }
            }
            jgen.writeEndArray();
            break;
        case ARRAY_SHORT:
            jgen.writeStartArray();
            {
                short[] values = (short[]) value;
                for (int i = 0, len = values.length; i < len; ++i) {
                    jgen.writeNumber((int) values[i]);
                }
            }
            jgen.writeEndArray();
            break;
        case ARRAY_CHAR:
            /* This is a pecualiar type: let's assume they really want
             * to output the String contained, instead of individual
             * chars
             */
            {
                char[] text = (char[]) value;
                jgen.writeString(text, 0, text.length);
            }
            jgen.writeEndArray();
            break;

        case ARRAY_BYTE:
            /* Hmmh. As with char arrays, it's not a JSON array,
             * but binary data
             */
            {
                byte[] data = (byte[]) value;
                jgen.writeBinary(data, 0, data.length);
            }
            break;

        case ARRAY_DOUBLE:
            jgen.writeStartArray();
            {
                double[] values = (double[]) value;
                for (int i = 0, len = values.length; i < len; ++i) {
                    jgen.writeNumber(values[i]);
                }
            }
            jgen.writeEndArray();
            break;
        case ARRAY_FLOAT:
            jgen.writeStartArray();
            {
                float[] values = (float[]) value;
                for (int i = 0, len = values.length; i < len; ++i) {
                    jgen.writeNumber(values[i]);
                }
            }
            jgen.writeEndArray();
            break;

        case ARRAY_BOOLEAN:
            jgen.writeStartArray();
            {
                boolean[] values = (boolean[]) value;
                for (int i = 0, len = values.length; i < len; ++i) {
                    jgen.writeBoolean(values[i]);
                }
            }
            jgen.writeEndArray();
            break;

        case ARRAY_OBJECT:
            return writeValue(defaultSerializer, jgen, (Object[]) value);

            // // // And finally java.util Collection types:

        case MAP:
            return writeValue(defaultSerializer, jgen, (Map<?,?>) value);

        case LIST_INDEXED:
            jgen.writeStartArray();
            {
                List<?> l = (List<?>) value;
                for (int i = 0, len = l.size(); i < len; ++i) {
                    writeAny(defaultSerializer, jgen, l.get(i));
                }
            }
            jgen.writeEndArray();
            break;
            
        case LIST_OTHER:
        case COLLECTION:
            return writeValue(defaultSerializer, jgen, (Collection<?>) value);

        case ITERABLE:
            jgen.writeStartArray();
            for (Object elem : (Iterable<?>) value) {
                writeAny(defaultSerializer, jgen, elem);
            }
            jgen.writeEndArray();
            break;

        case ITERATOR:
            jgen.writeStartArray();
            {
                Iterator<?> it = (Iterator<?>) value;
                while (it.hasNext()) {
                    writeAny(defaultSerializer, jgen, it.next());
                }
            }
            jgen.writeEndArray();
            break;
            
        default: // should never get here
            throwInternal("unhandled internal type: "+jdkType);
        }

        return true;
    }

    /**
     * Implementation of the typed map/object write method required by
     * {@link JavaTypeSerializer}.
     *<p>
     * Note: since this is not the 'root' method of mapper, it will NOT
     * explicitly flush the underlying generator after serializing
     * passed object.
     */
    public boolean writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Map<?,?> value)
        throws IOException, JsonParseException
    {
        jgen.writeStartObject();
        for (Map.Entry<?,?> me: value.entrySet()) {
            jgen.writeFieldName(me.getKey().toString());
            writeAny(defaultSerializer, jgen, me.getValue());
        }
        jgen.writeEndObject();
        return true;
    }

    /**
     * Implementation of the typed list/array write method required by
     * {@link JavaTypeSerializer}.
     *<p>
     * Note: since this is not the 'root' method of mapper, it will NOT
     * explicitly flush the underlying generator after serializing
     * passed object.
     */
    public boolean writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Collection<?> values)
        throws IOException, JsonParseException
    {
        jgen.writeStartArray();
        if (!values.isEmpty()) {
            for (Object ob : values) {
                writeAny(defaultSerializer, jgen, ob);
            }
        }
        jgen.writeEndArray();
        return true;
    }

    /**
     * Implementation of the typed list/array write method required by
     * {@link JavaTypeSerializer}.
     *<p>
     * Note: since this is not the 'root' method of mapper, it will NOT
     * explicitly flush the underlying generator after serializing
     * passed object.
     */
    public boolean writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Object[] values)
        throws IOException, JsonParseException
    {
        jgen.writeStartArray();
        for (int i = 0, len = values.length; i < len; ++i) {
            writeAny(defaultSerializer, jgen, values[i]);
        }
        jgen.writeEndArray();
        return true;
    }

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
                        reportProblem(jp, "Unexpected token ("+currToken+"), expected FIELD_NAME");
                    }
                    String fieldName = jp.getText();
                    Object  value = _readAndMap(jp, jp.nextToken());

                    if (_cfgDupFields == DupFields.ERROR) {
                        Object old = result.put(fieldName, value);
                        if (old != null) {
                            reportProblem(jp, "Duplicate value for field '"+fieldName+"', when dup fields mode is "+_cfgDupFields);
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
            reportProblem(jp, "Can not map token "+currToken+": stream off by a token or two?");

        default: // sanity check, should never happen
            throwInternal("Unrecognized event type: "+currToken);
            return null; // never gets this far
        }
    }
}

