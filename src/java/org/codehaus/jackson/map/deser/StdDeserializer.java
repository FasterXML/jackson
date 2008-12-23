package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializable;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonDeserializationContext;
import org.codehaus.jackson.map.type.*;

/**
 * Base class for simple standard deserializers
 */
public abstract class StdDeserializer<T>
    extends JsonDeserializer<T>
{
    protected StdDeserializer() { }


    /*
    //////////////////////////////////////////////
    // Primitive/wrapper deserializers
    //////////////////////////////////////////////
    */

    public final static class StringDeserializer
        extends JsonDeserializer<String>
    {
        public StringDeserializer() { }

        public String deserialize(JsonParser jp, JsonDeserializationContext ctxt)
            throws IOException, JsonParseException
        {
            JsonToken curr = jp.getCurrentToken();
            // Usually should just get string value:
            if (curr == JsonToken.VALUE_STRING) {
                return jp.getText();
            }
            // Can deserialize any scaler value, but not markers
            if (curr.isScalarValue()) {
                return jp.getText();
            }
            throw new JsonParseException("Can not deserialize String out of "+curr+" token", jp.getTokenLocation());
        }
    }
    
}
