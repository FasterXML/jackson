package org.codehaus.jackson.map.deser;

import java.io.IOException;

import org.codehaus.jackson.Base64Variants;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.annotate.JacksonStdImpl;
import org.codehaus.jackson.map.deser.std.StdScalarDeserializer;
import org.codehaus.jackson.type.JavaType;

/**
 * @deprecated Since 1.9, use {@link org.codehaus.jackson.map.deser.std.StdDeserializer} instead.
 */
@Deprecated
public abstract class StdDeserializer<T>
    extends org.codehaus.jackson.map.deser.std.StdDeserializer<T>
{
    protected StdDeserializer(Class<?> vc) {
        super(vc);
    }

    protected StdDeserializer(JavaType valueType) {
        super(valueType);
    }

    /*
    /**********************************************************
    /* Deprecated inner classes
    /**********************************************************
     */

    /**
     * @deprecated Since 1.9 use {@link org.codehaus.jackson.map.deser.std.ClassDeserializer} instead.
     */
    @Deprecated
    @JacksonStdImpl
    public class ClassDeserializer extends org.codehaus.jackson.map.deser.std.ClassDeserializer { }

    /**
     * @deprecated Since 1.9 use {@link org.codehaus.jackson.map.deser.std.CalendarDeserializer} instead.
     */
    @Deprecated
    @JacksonStdImpl
    public class CalendarDeserializer extends org.codehaus.jackson.map.deser.std.CalendarDeserializer { }
    
    /**
     * @deprecated Since 1.9 use {@link org.codehaus.jackson.map.deser.std.StringDeserializer} instead.
     */
    @Deprecated
    @JacksonStdImpl
    public final static class StringDeserializer
        extends StdScalarDeserializer<String>
    {
        public StringDeserializer() { super(String.class); }

        @Override
        public String deserialize(JsonParser jp, DeserializationContext ctxt)
            throws IOException, JsonProcessingException
        {
            JsonToken curr = jp.getCurrentToken();
            if (curr == JsonToken.VALUE_STRING) {
                return jp.getText();
            }
            if (curr == JsonToken.VALUE_EMBEDDED_OBJECT) {
                Object ob = jp.getEmbeddedObject();
                if (ob == null) {
                    return null;
                }
                if (ob instanceof byte[]) {
                    return Base64Variants.getDefaultVariant().encode((byte[]) ob, false);
                }
                return ob.toString();
            }
            if (curr.isScalarValue()) {
                return jp.getText();
            }
            throw ctxt.mappingException(_valueClass, curr);
        }

        @SuppressWarnings("deprecation")
        @Override
        public String deserializeWithType(JsonParser jp, DeserializationContext ctxt,
                TypeDeserializer typeDeserializer)
            throws IOException, JsonProcessingException
        {
            return deserialize(jp, ctxt);
        }
    }

}
