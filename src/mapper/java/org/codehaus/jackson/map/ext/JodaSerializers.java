package org.codehaus.jackson.map.ext;

import java.io.IOException;
import java.util.*;

import org.joda.time.DateTime;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.SerializerBase;
import org.codehaus.jackson.map.util.Provider;

/**
 * Provider for serializers that handle some basic data types
 * for <a href="http://joda-time.sourceforge.net/">Joda</a> date/time library.
 *
 * @since 1.4
 */
public class JodaSerializers
    implements Provider<Map.Entry<Class<?>,JsonSerializer<?>>>
{
    final static HashMap<Class<?>,JsonSerializer<?>> _serializers = new HashMap<Class<?>,JsonSerializer<?>>();
    static {
        _serializers.put(DateTime.class, new DateTimeSerializer());
    }

    public Collection<Map.Entry<Class<?>,JsonSerializer<?>>> provide() {
        return _serializers.entrySet();
    }

    /*
    ////////////////////////////////////////////////////////////////////////
    // Concrete deserializers
    ////////////////////////////////////////////////////////////////////////
    */

    public final static class DateTimeSerializer
        extends SerializerBase<DateTime>
    {
        public DateTimeSerializer() { super(DateTime.class); }

        @Override
        public void serialize(DateTime value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            if (provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)) {
                jgen.writeNumber(value.getMillis());
            } else {
                jgen.writeString(value.toString());
            }
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, java.lang.reflect.Type typeHint)
        {
            return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                    ? "number" : "string", true);
        }
    }
    
}
