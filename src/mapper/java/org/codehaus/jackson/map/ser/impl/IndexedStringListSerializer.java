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
 * Efficient implement for serializing {@link List}s that contains Strings and are random-accessible.
 * The only complexity is due to possibility that serializer for {@link String}
 * may be overridde; because of this, logic is needed to ensure that the default
 * serializer is in use to use fastest mode, or if not, to defer to custom
 * String serializer.
 * 
 * @since 1.7
 */
@JacksonStdImpl
public final class IndexedStringListSerializer
    extends StaticListSerializerBase<List<String>>
    implements ResolvableSerializer
{
    protected JsonSerializer<String> _serializer;
    
    public IndexedStringListSerializer(BeanProperty property) {
        super(List.class, property);
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
    public void serializeContents(List<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        if (_serializer != null) {
            serializeUsingCustom(value, jgen, provider);
            return;
        }
        final int len = value.size();
        for (int i = 0; i < len; ++i) {
            String str = value.get(i);
            try {
                if (str == null) {
                    provider.getNullValueSerializer().serialize(null, jgen, provider);
                } else {
                    jgen.writeString(str);
                }
            } catch (Exception e) {
                wrapAndThrow(e, value, i);
            }
        }
    }

    protected void serializeUsingCustom(List<String> value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        final int len = value.size();
        final JsonSerializer<String> ser = _serializer;
        for (int i = 0; i < len; ++i) {
            String str = value.get(i);
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
