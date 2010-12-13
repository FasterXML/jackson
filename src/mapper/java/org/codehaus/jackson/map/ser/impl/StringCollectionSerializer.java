package org.codehaus.jackson.map.ser.impl;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.BeanProperty;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ResolvableSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.annotate.JacksonStdImpl;

/**
 * Efficient implement for serializing {@link Collection}s that contain Strings.
 * The only complexity is due to possibility that serializer for {@link String}
 * may be overridde; because of this, logic is needed to ensure that the default
 * serializer is in use to use fastest mode, or if not, to defer to custom
 * String serializer.
 * 
 * @since 1.7
 */
@JacksonStdImpl

public class StringCollectionSerializer
    extends StaticListSerializerBase<Collection<String>>
    implements ResolvableSerializer
{
    protected JsonSerializer<String> _serializer;
    
    public StringCollectionSerializer(BeanProperty property) {
        super(Collection.class, property);
    }
        
    @Override protected JsonNode contentSchema() {
        return createSchemaNode("string", true);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void resolve(SerializerProvider provider) throws JsonMappingException
    {
        JsonSerializer<?> ser = provider.findValueSerializer(String.class, _property);
        if (!isDefaultSerializer(ser)) {
            _serializer = (JsonSerializer<String>) ser;
        }
    }

    @Override
    public final void serializeContents(Collection<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_serializer != null) {
            serializeUsingCustom(value, jgen, provider);
            return;
        }
        int i = 0;
        for (String str : value) {
            try {
                if (str == null) {
                    provider.getNullValueSerializer().serialize(null, jgen, provider);
                } else {
                    jgen.writeString(str);
                }
                ++i;
            } catch (Exception e) {
                wrapAndThrow(e, value, i);
            }
        }
    }

    protected void serializeUsingCustom(Collection<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final JsonSerializer<String> ser = _serializer;
        int i = 0;
        for (String str : value) {
            try {
                if (str == null) {
                    provider.getNullValueSerializer().serialize(null, jgen, provider);
                } else {
                    ser.serialize(str, jgen, provider);
                }
            } catch (Exception e) {
                wrapAndThrow(e, value, i);
            }
       }
    }

}
