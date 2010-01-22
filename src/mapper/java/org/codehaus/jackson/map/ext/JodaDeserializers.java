package org.codehaus.jackson.map.ext;

import java.io.IOException;
import java.util.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.StdDeserializer;
import org.codehaus.jackson.map.deser.StdScalarDeserializer;
import org.codehaus.jackson.map.util.Provider;

/**
 * Provider for deserializers that handle some basic data types
 * for <a href="http://joda-time.sourceforge.net/">Joda</a> date/time library.
 *
 * @since 1.4
 */
public class JodaDeserializers
    implements Provider<StdDeserializer<?>>
{
    public Collection<StdDeserializer<?>> provide() {
        return Arrays.asList(new StdDeserializer<?>[] {
                new DateTimeDeserializer()
            });
    }

    /*
    ////////////////////////////////////////////////////////////////////////
    // Concrete deserializers
    ////////////////////////////////////////////////////////////////////////
    */

    /**
     * Basic deserializer for {@link DateTime}. Accepts JSON String and Number
     * values and passes those to single-argument constructor.
     * Does not (yet?) support JSON object; support can be added if desired.
     */
    public static class DateTimeDeserializer
        extends StdScalarDeserializer<DateTime>
    {
        public DateTimeDeserializer() { super(DateTime.class); }

        @Override
        public DateTime deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken t = jp.getCurrentToken();
            if (t == JsonToken.VALUE_NUMBER_INT) {
                return new DateTime(jp.getLongValue(), DateTimeZone.UTC);
            }
            if (t == JsonToken.VALUE_STRING) {
                return new DateTime(jp.getText().trim(), DateTimeZone.UTC);
            }
            throw ctxt.mappingException(getValueClass());
        }
    }
}
