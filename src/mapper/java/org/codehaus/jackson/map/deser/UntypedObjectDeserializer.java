package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationConfig;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.TypeDeserializer;

/**
 * This deserializer is only used if it is necessary to bind content of
 * unknown type (or without regular structure) into generic Java container
 * types; Lists, Maps, wrappers, nulls and so on.
 */
public class UntypedObjectDeserializer
    extends StdDeserializer<Object>
{
    public UntypedObjectDeserializer() { super(Object.class); }

    /*
    /*************************************************************
    /* Deserializer API
    /*************************************************************
     */
    
    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        switch (jp.getCurrentToken()) {
            
            // first, simple types:
        case VALUE_STRING:
            return jp.getText();

        case VALUE_NUMBER_INT:
            /* [JACKSON-100]: caller may want to get all integral values
             * returned as BigInteger, for consistency
             */
            if (ctxt.isEnabled(DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS)) {
                return jp.getBigIntegerValue(); // should be optimal, whatever it is
            }
            return jp.getNumberValue(); // should be optimal, whatever it is

        case VALUE_NUMBER_FLOAT:
            /* [JACKSON-72]: need to allow overriding the behavior regarding
             *   which type to use
             */
            if (ctxt.isEnabled(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return jp.getDecimalValue();
            }
            return Double.valueOf(jp.getDoubleValue());

        case VALUE_TRUE:
            return Boolean.TRUE;
        case VALUE_FALSE:
            return Boolean.FALSE;

        case VALUE_NULL:
            return null;
            
            // Then structured types:
            
        case START_ARRAY:
            return mapArray(jp, ctxt);

        case START_OBJECT:
            return mapObject(jp, ctxt);

            // and finally, invalid types
        case END_ARRAY:
        case END_OBJECT:
        case FIELD_NAME:
            break;
        }

        throw ctxt.mappingException(Object.class);
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        /* Output can be as JSON Object, Array or scalar: no way to know
         * a priori. So:
         */
        return typeDeserializer.deserializeTypedFromAny(jp, ctxt);
    }

    /*
    /*************************************************************
    /* Internal methods
    /*************************************************************
     */
    
    protected List<Object> mapArray(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        ArrayList<Object> result = new ArrayList<Object>();
        while (jp.nextToken() != JsonToken.END_ARRAY) {
            result.add(deserialize(jp, ctxt));
        }
        return result;
    }

    protected Map<String,Object> mapObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
        JsonToken currToken;
        
        while ((currToken = jp.nextToken()) != JsonToken.END_OBJECT) {
            if (currToken != JsonToken.FIELD_NAME) {
                throw JsonMappingException.from(jp, "Unexpected token ("+currToken+"), expected FIELD_NAME");
            }
            String fieldName = jp.getText();
            jp.nextToken();
            result.put(fieldName, deserialize(jp, ctxt));
        }
        return result;
    }
}
