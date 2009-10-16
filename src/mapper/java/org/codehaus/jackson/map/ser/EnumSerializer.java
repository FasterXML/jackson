package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;

import org.codehaus.jackson.*;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.map.util.EnumValues;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.type.JavaType;

public class EnumSerializer
    extends SerializerBase<Enum<?>>
{
    /**
     * This map contains pre-resolved values (since there are ways
     * to customize actual String constants to use) to use as
     * serializations.
     */
    protected final EnumValues _values;

    public EnumSerializer(EnumValues v) {
        _values = v;
    }

    public static EnumSerializer construct(Class<Enum<?>> enumClass, AnnotationIntrospector intr)
    {
        return new EnumSerializer(EnumValues.construct(enumClass, intr));
    }

    @Override
    public void serialize(Enum<?> en, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeString(_values.valueFor(en));
    }
    
    @Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        ObjectNode objectNode = createSchemaNode("string", true);
        if (typeHint != null) {
            JavaType type = TypeFactory.fromType(typeHint);
            if (type.isEnumType()) {
                ArrayNode enumNode = objectNode.putArray("enum");
                for (String value : _values.values()) {
                    enumNode.add(value);
                }
            }
        }
        return objectNode;
    }

    public EnumValues getEnumValues() { return _values; }
}

