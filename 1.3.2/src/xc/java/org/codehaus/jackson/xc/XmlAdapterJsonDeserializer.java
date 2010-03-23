package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonMappingException;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Ryan Heaton
 */
public class XmlAdapterJsonDeserializer
        extends JsonDeserializer<Object>
{
    private final XmlAdapter<Object,Object> xmlAdapter;
    private final Class<?> valueClass;

    public XmlAdapterJsonDeserializer(XmlAdapter<Object,Object> xmlAdapter)
    {
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
            throw new JsonMappingException("Unable to unmarshal.", e);
        }
    }



}