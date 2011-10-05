package org.codehaus.jackson.map.deser.std;

import java.io.IOException;

import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.DeserializationContext;
import org.codehaus.jackson.type.JavaType;

/**
 * @since 1.9
 */
public class JavaTypeDeserializer
    extends StdScalarDeserializer<JavaType>
{
    public JavaTypeDeserializer() { super(JavaType.class); }
    
    @Override
    public JavaType deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        JsonToken curr = jp.getCurrentToken();
        // Usually should just get string value:
        if (curr == JsonToken.VALUE_STRING) {
            String str = jp.getText().trim();
            if (str.length() == 0) {
                return getEmptyValue();
            }
            return ctxt.getTypeFactory().constructFromCanonical(str);
        }
        // or occasionally just embedded object maybe
        if (curr == JsonToken.VALUE_EMBEDDED_OBJECT) {
            return (JavaType) jp.getEmbeddedObject();
        }
        throw ctxt.mappingException(_valueClass);
    }
}
