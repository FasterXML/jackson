package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Deserializer only used for abstract types used as placeholders during polymorphic
 * type handling deserialization. If so, there is no real deserializer associated
 * with nominal type, just {@link TypeDeserializer}; and any calls that do not
 * pass such resolver will result in an error.
 * 
 * @author tatu
 * 
 * @since 1.6
 */
public class AbstractDeserializer
    extends JsonDeserializer<Object>
{
    protected final JavaType _baseType;
    
    public AbstractDeserializer(JavaType bt)
    {
        _baseType = bt;
    }

    @Override
    public Object deserializeWithType(JsonParser jp, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer)
        throws IOException, JsonProcessingException
    {
        /* As per [JACKSON-417], there is a chance we might be "native" types (String, Boolean,
         * Integer), which do not include any type information...
         */
        switch (jp.getCurrentToken()) {
        /* First, so-called "native" types (ones that map
         * naturally and thus do not need or use type ids)
         */
        case VALUE_STRING:
            return jp.getText();

        case VALUE_NUMBER_INT:
            // For [JACKSON-100], see above:
            if (ctxt.isEnabled(DeserializationConfig.Feature.USE_BIG_INTEGER_FOR_INTS)) {
                return jp.getBigIntegerValue();
            }
            return jp.getIntValue();

        case VALUE_NUMBER_FLOAT:
            // For [JACKSON-72], see above
            if (ctxt.isEnabled(DeserializationConfig.Feature.USE_BIG_DECIMAL_FOR_FLOATS)) {
                return jp.getDecimalValue();
            }
            return Double.valueOf(jp.getDoubleValue());

        case VALUE_TRUE:
            return Boolean.TRUE;
        case VALUE_FALSE:
            return Boolean.FALSE;
        case VALUE_EMBEDDED_OBJECT:
            return jp.getEmbeddedObject();

        case VALUE_NULL: // should not get this far really but...
            return null;

        case START_ARRAY:
            /* 11-Feb-2011, tatu: Uh. Given that we know very little about the type
             *   here, we can't be sure but it sure looks like we must have used
             *   "As.WRAPPER_ARRAY" here. This will NOT work properly if someone
             *   tries to use "As.WRAPPER_OBJECT"; but let's tackle one issue
             *   at a time. See [JACKSON-485] for an example of where this fix
             *   is needed.
             */
            return typeDeserializer.deserializeTypedFromAny(jp, ctxt);

            /*
        case START_OBJECT:
        case FIELD_NAME:
             */
        }

        // should we call 'fromAny' or 'fromObject'? We should get an object, for abstract types, right?
        // 11-Feb-2011: not necessarily; for example, when serialize Enums that implement an interface... *sigh*
        //return typeDeserializer.deserializeTypedFromAny(jp, ctxt);

        return typeDeserializer.deserializeTypedFromObject(jp, ctxt);
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
    {
        // no can do:
        throw ctxt.instantiationException(_baseType.getRawClass(), "abstract types can only be instantiated with additional type information");
    }
}
