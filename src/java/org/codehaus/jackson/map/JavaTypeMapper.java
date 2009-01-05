package org.codehaus.jackson.map;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.ser.StdSerializerProvider;
import org.codehaus.jackson.map.ser.BeanSerializerFactory;

// Classes needed to support legacy mapping
import org.codehaus.jackson.map.legacy.JavaTypeSerializer;
import org.codehaus.jackson.map.legacy.KnownClasses;

/**
 * This class is provides legacy support for earlier data binding
 * support: it has been deprecated by {@link ObjectMapper} and only
 * exists to allow for migrating old code to new functionality
 *
 * @deprecated Use basic {@link ObjectMapper} (which this class extends) instead
 */
public class JavaTypeMapper
    extends ObjectMapper
    implements JavaTypeSerializer<Object>
{
    /*
    ////////////////////////////////////////////////////
    // Life-cycle
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
        this(jf, null, null);
    }

    public JavaTypeMapper(JsonFactory jf, JsonSerializerProvider sp,
                          JsonDeserializerProvider dp)
    {
        super(jf, sp, dp);
    }

    /*
    ////////////////////////////////////////////////////
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
     *<p>
     * @deprecated Use {@link #readValue}
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
        writeAny(this, jg, value);
        jg.flush();
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods, deserialization
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

    /*
    ////////////////////////////////////////////////////
    // Internal methods, serialization;
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
     *<p>
     * @deprecated Use {@link org.codehaus.jackson.map.JavaTypeMapper#writeValue}
     */
    //@SuppressWarnings("unchecked")
    public final boolean writeAny(JavaTypeSerializer<Object> defaultSerializer,
                                  JsonGenerator jgen, Object value)
        throws IOException, JsonGenerationException
    {
        if (value == null) {
            jgen.writeNull();
            return true;
        }
        // Perhaps it's one of common core JDK types?
        KnownClasses.JdkClasses jdkType = KnownClasses.findTypeFast(value);
        if (jdkType == null) {
            // And if not, maybe we can further introspect the type
            jdkType = KnownClasses.findTypeSlow(value);

            if (jdkType == null) {
                // Nope, can't figure it out. Error out:
                throw new JsonGenerationException("Unknown type ("+value.getClass().getName()+"): don't know how to handle");
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
            return _writeValue(defaultSerializer, jgen, (Object[]) value);

            // // // And finally java.util Collection types:

        case MAP:
            return _writeValue(defaultSerializer, jgen, (Map<?,?>) value);

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
            return _writeValue(defaultSerializer, jgen, (Collection<?>) value);

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
            throw new RuntimeException("Internal error: unhandled internal type: "+jdkType);
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
    public boolean _writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Map<?,?> value)
        throws IOException, JsonGenerationException
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
    public boolean _writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Collection<?> values)
        throws IOException, JsonGenerationException
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
    public boolean _writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Object[] values)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartArray();
        for (int i = 0, len = values.length; i < len; ++i) {
            writeAny(defaultSerializer, jgen, values[i]);
        }
        jgen.writeEndArray();
        return true;
    }
}

