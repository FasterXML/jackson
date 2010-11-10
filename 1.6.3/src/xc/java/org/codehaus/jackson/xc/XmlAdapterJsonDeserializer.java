package org.codehaus.jackson.xc;

import java.io.IOException;
import javax.xml.bind.annotation.adapters.XmlAdapter;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.deser.StdDeserializer;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

/**
 * @author Ryan Heaton
 * @author Tatu Saloranta
 */
public class XmlAdapterJsonDeserializer
    extends StdDeserializer<Object>
{
    protected final static JavaType ADAPTER_TYPE = TypeFactory.type(XmlAdapter.class);
    
    protected final XmlAdapter<Object,Object> _xmlAdapter;
    protected final JavaType _valueType;

    protected JsonDeserializer<?> _deserializer;
    
    public XmlAdapterJsonDeserializer(XmlAdapter<Object,Object> xmlAdapter)
    {
        super(Object.class); // type not yet known (will be in a second), but that's ok...
        _xmlAdapter = xmlAdapter;
        /* [JACKSON-404] Need to figure out generic type parameters
         *   used...
         */
        JavaType type = TypeFactory.type(xmlAdapter.getClass());
        JavaType[] rawTypes = TypeFactory.findParameterTypes(type, XmlAdapter.class);
        _valueType = (rawTypes == null || rawTypes.length == 0)  ? TypeFactory.type(Object.class) : rawTypes[0];
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        /* Unfortunately we can not use the usual resolution mechanism (ResolvableDeserializer)
         * because it won't get called due to way adapters are created. So, need to do it
         * lazily when we get here:
         */
        JsonDeserializer<?> deser = _deserializer;
        if (deser == null) {
            _deserializer = deser = ctxt.getDeserializerProvider().findValueDeserializer
                (ctxt.getConfig(), _valueType, ADAPTER_TYPE, "");
        }
        Object boundObject = deser.deserialize(jp, ctxt);
        try {
            return _xmlAdapter.unmarshal(boundObject);
        } catch (Exception e) {
            throw new JsonMappingException("Unable to unmarshal (to type "+_valueType+"): "+e.getMessage(), e);
        }
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
}
