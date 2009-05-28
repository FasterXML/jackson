package org.codehaus.jackson.xc;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * @author Ryan Heaton
 */
public class XmlAdapterJsonSerializer extends JsonSerializer
{
    private final XmlAdapter<Object,Object> xmlAdapter;

    public XmlAdapterJsonSerializer(XmlAdapter<Object,Object> xmlAdapter)
    {
        this.xmlAdapter = xmlAdapter;
    }

    @Override
    public void serialize(Object value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException
    {
        Object adapted;
        try {
            adapted = this.xmlAdapter.marshal(value);
        } catch (Exception e) {
            throw new JsonMappingException("Unable to use an XmlAdapter.", e);
        }
        JsonSerializer<Object> jsonSerializer = provider.findValueSerializer(adapted.getClass());
        jsonSerializer.serialize(adapted, jgen, provider);
    }

//    @Override
//    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
//            throws JsonMappingException
//    {
//        return provider.findValueSerializer(findValueClass()).getSchema(provider, typeHint);
//    }

    private Class findValueClass()
    {
        Type superClass = this.xmlAdapter.getClass().getGenericSuperclass();
        while (superClass instanceof ParameterizedType && XmlAdapter.class != ((ParameterizedType)superClass).getRawType()) {
            superClass = ((Class) ((ParameterizedType) superClass).getRawType()).getGenericSuperclass();
        }
        return (Class) ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

}
