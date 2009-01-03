package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonDeserializationContext;
import org.codehaus.jackson.map.JsonMappingException;

/**
 * Base class for simple standard deserializers
 */
public abstract class StdDeserializer<T>
    extends JsonDeserializer<T>
{
    protected StdDeserializer() { }

    /*
    ////////////////////////////////////////////////////////////
    // Helper methods for sub-classes to use; exception handling
    ////////////////////////////////////////////////////////////
    */

    protected JsonMappingException mappingException(JsonParser jp, Class<?> targetClass)
    {
        return JsonMappingException.from(jp, "Can not deserialize "+targetClass.getName()+" out of "+jp.getCurrentToken()+" token");
    }

    protected JsonMappingException instantiationException(JsonParser jp, Class<?> instClass, Exception e)
    {
        return JsonMappingException.from(jp, "Can not construct instance of "+instClass.getName()+", problem: "+e.getMessage());
    }

    /*
    //////////////////////////////////////////////
    // Primitive/wrapper deserializers
    //////////////////////////////////////////////
    */

    public final static class StringDeserializer
        extends StdDeserializer<String>
    {
        public StringDeserializer() { }

        public String deserialize(JsonParser jp, JsonDeserializationContext ctxt)
            throws IOException, JsonProcessingException
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
            throw mappingException(jp, String.class);
        }
    }
    
}
