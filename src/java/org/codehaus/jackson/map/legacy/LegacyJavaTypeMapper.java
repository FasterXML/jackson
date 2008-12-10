package org.codehaus.jackson.map.legacy;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;

/**
 * This class contains legacy support code, to implement pre-0.9.5
 * "java type" mapper functionality. Mapping interface and code
 * has since been refactored, but to smooth upgrade path, we'll
 * still retain old functionality for a while.
 */
public final class LegacyJavaTypeMapper
    implements JavaTypeSerializer<Object>
{
    public LegacyJavaTypeMapper() { }

    public final void writeAny(JsonGenerator jg, Object value)
        throws IOException, JsonGenerationException
    {
        writeAny(this, jg, value);
        jg.flush();
    }

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
     *<p>
     * @deprecated Use {@link #writeValue}
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
    public boolean writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Map<?,?> value)
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
    public boolean writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Collection<?> values)
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
    public boolean writeValue(JavaTypeSerializer<Object> defaultSerializer, JsonGenerator jgen, Object[] values)
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
