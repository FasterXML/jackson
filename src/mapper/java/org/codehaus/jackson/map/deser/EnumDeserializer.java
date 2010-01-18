package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.annotate.JsonCachable;

/**
 * Deserializer class that can deserialize instances of
 * specified Enum class from Strings and Integers.
 */
@JsonCachable
/**
 * Construction of these deserializers is bit costly, plus it's
 * absolutely safe to cache them as well (no generic variations etc).
 */
public class EnumDeserializer
    extends StdScalarDeserializer<Enum<?>>
{
    final EnumResolver<?> _resolver;
    
    public EnumDeserializer(EnumResolver<?> res)
    {
        super(Enum.class);
        _resolver = res;
    }

    /*
    /////////////////////////////////////////////////////////
    // JsonDeserializer implementation
    /////////////////////////////////////////////////////////
     */

    public Enum<?> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken curr = jp.getCurrentToken();
        
        // Usually should just get string value:
        if (curr == JsonToken.VALUE_STRING) {
            String name = jp.getText();
            Enum<?> result = _resolver.findEnum(name);
            if (result == null) {
                throw ctxt.weirdStringException(_resolver.getEnumClass(), "value not one of declared Enum instance names");
            }
            return result;
        }
        // But let's consider int acceptable as well (if within ordinal range)
        if (curr == JsonToken.VALUE_NUMBER_INT) {
            int index = jp.getIntValue();
            Enum<?> result = _resolver.getEnum(index);
            if (result == null) {
                throw ctxt.weirdNumberException(_resolver.getEnumClass(), "index value outside legal index range [0.."+_resolver.lastValidIndex()+"]");
            }
            return result;
        }
        throw ctxt.mappingException(_resolver.getEnumClass());
    }
}
