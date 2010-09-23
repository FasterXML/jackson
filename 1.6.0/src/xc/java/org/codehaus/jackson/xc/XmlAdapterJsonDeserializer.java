package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.deser.StdDeserializer;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Ryan Heaton
 */
public class XmlAdapterJsonDeserializer
    extends StdDeserializer<Object>
{
    private final XmlAdapter<Object,Object> xmlAdapter;
    private final Class<?> valueClass;

    public XmlAdapterJsonDeserializer(XmlAdapter<Object,Object> xmlAdapter)
    {
        super(Object.class); // type not really known, not passed
        this.xmlAdapter = xmlAdapter;
        this.valueClass = findValueClass();
    }

    private Class<?> findValueClass()
    {
        Type superClass = this.xmlAdapter.getClass().getGenericSuperclass();
        while (superClass instanceof ParameterizedType && XmlAdapter.class != ((ParameterizedType)superClass).getRawType()) {
            superClass = ((Class<?>) ((ParameterizedType) superClass).getRawType()).getGenericSuperclass();
        }
        return (Class<?>) ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    @Override
    public Object deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        Object boundObject = jp.readValueAs(valueClass);
        try {
            return this.xmlAdapter.unmarshal(boundObject);
        } catch (Exception e) {
            throw new JsonMappingException("Unable to unmarshal: "+e.getMessage(), e);
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
