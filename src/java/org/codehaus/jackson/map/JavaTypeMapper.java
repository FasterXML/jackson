package org.codehaus.jackson.map;

import java.io.*;
import java.util.*;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.type.TypeFactory;

/**
 * This class is provides legacy support for earlier data binding
 * support: it has been deprecated by {@link ObjectMapper} and only
 * exists to allow for migrating old code to new functionality
 *
 * @deprecated Use basic {@link ObjectMapper} (which this class extends) instead
 */
public class JavaTypeMapper
    extends ObjectMapper
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
     * {@link SerializerProvider}, and
     * {@link BeanSerializerFactory} as its
     * {@link SerializerFactory}.
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

    public JavaTypeMapper(JsonFactory jf, SerializerProvider sp,
                          DeserializerProvider dp)
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
        // Regular "untyped" deserializer should work just fine here:
        return _readValue(jp, TypeFactory.instance.fromClass(Object.class));
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
        /* Regular (new) serializer should work ok here too; won't use
         * older customizations, but will still compile for the default
         * case...
         */
        writeValue(jg, value);
    }
}

