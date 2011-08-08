package org.codehaus.jackson.map.ser.std;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Calendar;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JacksonStdImpl;

/**
 * Standard serializer for {@link java.util.Calendar}.
 * As with other time/date types, is configurable to produce timestamps
 * (standard Java 64-bit timestamp) or textual formats (usually ISO-8601).
 * 
 * @since 1.9 (moved from 'org.codehaus.jackson.map.ser.StdSerializers#CalendarSerializer}
 */
@JacksonStdImpl
public class CalendarSerializer
    extends ScalarSerializerBase<Calendar>
{
    public static CalendarSerializer instance = new CalendarSerializer();

    public CalendarSerializer() { super(Calendar.class); }
    
    @Override
    public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        provider.defaultSerializeDateValue(value.getTimeInMillis(), jgen);
    }

    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
    {
        //TODO: (ryan) add a format for the date in the schema?
        return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                ? "number" : "string", true);
    }
}
