package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Calendar;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.annotate.JacksonStdImpl;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.JsonSerializableSchema;
import org.codehaus.jackson.util.TokenBuffer;

/**
 * Container class for serializers used for handling standard JDK-provided
 * types
 * 
 * @since 1.5
 */
public class StdSerializers
{
    protected StdSerializers() { }

    /*
    /**********************************************************
    /* Abstract base classes
    /**********************************************************
     */

    /**
     * Intermediate base class for limited number of scalar types
     * that should never include type information. These are "native"
     * types that are default mappings for corresponding JSON scalar
     * types: String, Integer, Double and Boolean.
     */
    protected abstract static class NonTypedScalarSerializer<T>
        extends ScalarSerializerBase<T>
    {
        protected NonTypedScalarSerializer(Class<T> t) {
            super(t);
        }

        @Override
        public final void serializeWithType(T value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            // no type info, just regular serialization
            serialize(value, jgen, provider);            
        }
    }
    
    /*
    /**********************************************************
    /* Concrete serializers, non-numeric primitives, Strings, Classes
    /**********************************************************
     */
    
    /**
     * Serializer used for primitive boolean, as well as java.util.Boolean
     * wrapper type.
     *<p>
     * Since this is one of "native" types, no type information is ever
     * included on serialization (unlike for most scalar types as of 1.5)
     */
    @JacksonStdImpl
    public final static class BooleanSerializer
        extends NonTypedScalarSerializer<Boolean>
    {
        /**
         * Whether type serialized is primitive (boolean) or wrapper
         * (java.lang.Boolean); if true, former, if false, latter.
         */
        final boolean _forPrimitive;
    
        public BooleanSerializer(boolean forPrimitive)
        {
            super(Boolean.class);
            _forPrimitive = forPrimitive;
        }
    
        @Override
        public void serialize(Boolean value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeBoolean(value.booleanValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            /*(ryan) it may not, in fact, be optional, but there's no way
             * to tell whether we're referencing a boolean or java.lang.Boolean.
             */
            /* 27-Jun-2009, tatu: Now we can tell, after passing
             *   'forPrimitive' flag...
             */
            return createSchemaNode("boolean", !_forPrimitive);
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.String}s.
     *<p>
     * Since this is one of "native" types, no type information is ever
     * included on serialization (unlike for most scalar types as of 1.5)
     */
    @JacksonStdImpl
    public final static class StringSerializer
        extends NonTypedScalarSerializer<String>
    {
        public StringSerializer() { super(String.class); }

        @Override
        public void serialize(String value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("string", true);
        }
    }

    /*
    /**********************************************************
    /* Concrete serializers, numerics
    /**********************************************************
     */

    /**
     * This is the special serializer for regular {@link java.lang.Integer}s
     * (and primitive ints)
     *<p>
     * Since this is one of "native" types, no type information is ever
     * included on serialization (unlike for most scalar types as of 1.5)
     */
    @JacksonStdImpl
    public final static class IntegerSerializer
        extends NonTypedScalarSerializer<Integer>
    {
        public IntegerSerializer() { super(Integer.class); }
    
        @Override
        public void serialize(Integer value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
    }

    /**
     * Similar to {@link IntegerSerializer}, but will not cast to Integer:
     * instead, cast is to {@link java.lang.Number}, and conversion is
     * by calling {@link java.lang.Number#intValue}.
     */
    @JacksonStdImpl
    public final static class IntLikeSerializer
        extends ScalarSerializerBase<Number>
    {
        final static IntLikeSerializer instance = new IntLikeSerializer();
    
        public IntLikeSerializer() { super(Number.class); }
        
        @Override
        public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.intValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("integer", true);
        }
    }

    @JacksonStdImpl
    public final static class LongSerializer
        extends ScalarSerializerBase<Long>
    {
        final static LongSerializer instance = new LongSerializer();
    
        public LongSerializer() { super(Long.class); }
        
        @Override
        public void serialize(Long value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.longValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }
    
    @JacksonStdImpl
    public final static class FloatSerializer
        extends ScalarSerializerBase<Float>
    {
        final static FloatSerializer instance = new FloatSerializer();
    
        public FloatSerializer() { super(Float.class); }
        
        @Override
        public void serialize(Float value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.floatValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }

    /**
     * This is the special serializer for regular {@link java.lang.Double}s
     * (and primitive doubles)
     *<p>
     * Since this is one of "native" types, no type information is ever
     * included on serialization (unlike for most scalar types as of 1.5)
     */
    @JacksonStdImpl
    public final static class DoubleSerializer
        extends NonTypedScalarSerializer<Double>
    {
        final static DoubleSerializer instance = new DoubleSerializer();
    
        public DoubleSerializer() { super(Double.class); }
    
        @Override
        public void serialize(Double value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeNumber(value.doubleValue());
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }
    
    /**
     * As a fallback, we may need to use this serializer for other
     * types of {@link Number}s (custom types).
     */
    @JacksonStdImpl
    public final static class NumberSerializer
        extends ScalarSerializerBase<Number>
    {
        public final static NumberSerializer instance = new NumberSerializer();
    
        public NumberSerializer() { super(Number.class); }
    
        @Override
        public void serialize(Number value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            // As per [JACKSON-423], handling for BigInteger and BigDecimal was missing!
            if (value instanceof BigDecimal) {
                jgen.writeNumber((BigDecimal) value);
            } else if (value instanceof BigInteger) {
                jgen.writeNumber((BigInteger) value);
                
            /* These shouldn't match (as there are more specific ones),
             * but just to be sure:
             */
            } else if (value instanceof Double) {
                jgen.writeNumber(((Double) value).doubleValue());
            } else if (value instanceof Float) {
                jgen.writeNumber(((Float) value).floatValue());
            } else {
                // We'll have to use fallback "untyped" number write method
                jgen.writeNumber(value.toString());
            }
        }
    
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("number", true);
        }
    }

    /*
    /**********************************************************
    /* Serializers for JDK date/time data types
    /**********************************************************
     */

    /**
     * For time values we should use timestamp, since that is about the only
     * thing that can be reliably converted between date-based objects
     * and json.
     */
    @JacksonStdImpl
    public final static class CalendarSerializer
        extends ScalarSerializerBase<Calendar>
    {
        public final static CalendarSerializer instance = new CalendarSerializer();

        public CalendarSerializer() { super(Calendar.class); }
        
        @Override
        public void serialize(Calendar value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeDateValue(value.getTimeInMillis(), jgen);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //TODO: (ryan) add a format for the date in the schema?
            return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                    ? "number" : "string", true);
        }
    }

    /**
     * For efficiency, we will serialize Dates as longs, instead of
     * potentially more readable Strings.
     */
    @JacksonStdImpl
    public final static class UtilDateSerializer
        extends ScalarSerializerBase<java.util.Date>
    {
        public final static UtilDateSerializer instance = new UtilDateSerializer();

        public UtilDateSerializer() { super(java.util.Date.class); }

        @Override
        public void serialize(java.util.Date value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            provider.defaultSerializeDateValue(value, jgen);
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //todo: (ryan) add a format for the date in the schema?
            return createSchemaNode(provider.isEnabled(SerializationConfig.Feature.WRITE_DATES_AS_TIMESTAMPS)
                    ? "number" : "string", true);
        }
    }

    /**
     * Compared to regular {@link UtilDateSerializer}, we do use String
     * representation here. Why? Basically to truncate of time part, since
     * that should not be used by plain SQL date.
     */
    @JacksonStdImpl
    public final static class SqlDateSerializer
        extends ScalarSerializerBase<java.sql.Date>
    {
        public SqlDateSerializer() { super(java.sql.Date.class); }

        @Override
        public void serialize(java.sql.Date value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            //todo: (ryan) add a format for the date in the schema?
            return createSchemaNode("string", true);
        }
    }

    @JacksonStdImpl
    public final static class SqlTimeSerializer
        extends ScalarSerializerBase<java.sql.Time>
    {
        public SqlTimeSerializer() { super(java.sql.Time.class); }

        @Override
        public void serialize(java.sql.Time value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            jgen.writeString(value.toString());
        }

        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            return createSchemaNode("string", true);
        }
    }

    
    /*
    /**********************************************************
    / Other serializers
    /**********************************************************
     */

    /**
     * Generic handler for types that implement {@link JsonSerializable}.
     *<p>
     * Note: given that this is used for anything that implements
     * interface, can not be checked for direct class equivalence.
     */
    @JacksonStdImpl
    @SuppressWarnings("deprecation")
    public final static class SerializableSerializer
        extends SerializerBase<JsonSerializable>
    {
        protected final static SerializableSerializer instance = new SerializableSerializer();

        private SerializableSerializer() { super(JsonSerializable.class); }

        @Override
        public void serialize(JsonSerializable value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            value.serialize(jgen, provider);
        }

        @Override
        public final void serializeWithType(JsonSerializable value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            /* 24-Jan-2009, tatus: This is not quite optimal (perhaps we should
             *   just create separate serializer...), but works until 2.0 will
             *   deprecate non-typed interface
             */
            if (value instanceof JsonSerializableWithType) {
                ((JsonSerializableWithType) value).serializeWithType(jgen, provider, typeSer);
            } else {
                this.serialize(value, jgen, provider);
            }
        }
        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode objectNode = createObjectNode();
            String schemaType = "any";
            String objectProperties = null;
            String itemDefinition = null;
            if (typeHint != null) {
                Class<?> rawClass = TypeFactory.type(typeHint).getRawClass();
                if (rawClass.isAnnotationPresent(JsonSerializableSchema.class)) {
                    JsonSerializableSchema schemaInfo = rawClass.getAnnotation(JsonSerializableSchema.class);
                    schemaType = schemaInfo.schemaType();
                    if (!"##irrelevant".equals(schemaInfo.schemaObjectPropertiesDefinition())) {
                        objectProperties = schemaInfo.schemaObjectPropertiesDefinition();
                    }
                    if (!"##irrelevant".equals(schemaInfo.schemaItemDefinition())) {
                        itemDefinition = schemaInfo.schemaItemDefinition();
                    }
                }
            }
            objectNode.put("type", schemaType);
            if (objectProperties != null) {
                try {
                    objectNode.put("properties", new ObjectMapper().readValue(objectProperties, JsonNode.class));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            if (itemDefinition != null) {
                try {
                    objectNode.put("items", new ObjectMapper().readValue(itemDefinition, JsonNode.class));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            // always optional, no need to specify:
            //objectNode.put("required", false);
            return objectNode;
        }
    }

    /**
     * Generic handler for types that implement {@link JsonSerializableWithType}.
     *<p>
     * Note: given that this is used for anything that implements
     * interface, can not be checked for direct class equivalence.
     */
    @JacksonStdImpl
    public final static class SerializableWithTypeSerializer
        extends SerializerBase<JsonSerializableWithType>
    {
        protected final static SerializableWithTypeSerializer instance = new SerializableWithTypeSerializer();

        private SerializableWithTypeSerializer() { super(JsonSerializableWithType.class); }

        @SuppressWarnings("deprecation") // why is this needed?
        @Override
        public void serialize(JsonSerializableWithType value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            value.serialize(jgen, provider);
        }

        @Override
        public final void serializeWithType(JsonSerializableWithType value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            value.serializeWithType(jgen, provider, typeSer);
        }
        
        // copied verbatim from "JsonSerializableSerializer"
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
        {
            ObjectNode objectNode = createObjectNode();
            String schemaType = "any";
            String objectProperties = null;
            String itemDefinition = null;
            if (typeHint != null) {
                Class<?> rawClass = TypeFactory.rawClass(typeHint);
                if (rawClass.isAnnotationPresent(JsonSerializableSchema.class)) {
                    JsonSerializableSchema schemaInfo = rawClass.getAnnotation(JsonSerializableSchema.class);
                    schemaType = schemaInfo.schemaType();
                    if (!"##irrelevant".equals(schemaInfo.schemaObjectPropertiesDefinition())) {
                        objectProperties = schemaInfo.schemaObjectPropertiesDefinition();
                    }
                    if (!"##irrelevant".equals(schemaInfo.schemaItemDefinition())) {
                        itemDefinition = schemaInfo.schemaItemDefinition();
                    }
                }
            }
            objectNode.put("type", schemaType);
            if (objectProperties != null) {
                try {
                    objectNode.put("properties", new ObjectMapper().readValue(objectProperties, JsonNode.class));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            if (itemDefinition != null) {
                try {
                    objectNode.put("items", new ObjectMapper().readValue(itemDefinition, JsonNode.class));
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            }
            // always optional, no need to specify:
            //objectNode.put("required", false);
            return objectNode;
        }
    }

    /**
     * We also want to directly support serialization of {@link TokenBuffer};
     * and since it is part of core package, it can not implement
     * {@link JsonSerializable} (which is only included in the mapper
     * package)
     *
     * @since 1.5
     */
    @JacksonStdImpl
    public final static class TokenBufferSerializer
        extends SerializerBase<TokenBuffer>
    {
        public TokenBufferSerializer() { super(TokenBuffer.class); }

        @Override
        public void serialize(TokenBuffer value, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonGenerationException
        {
            value.serialize(jgen);
        }

        /**
         * Implementing typed output for contents of a TokenBuffer is very tricky,
         * since we do not know for sure what its contents might look like (or, rather,
         * we do know when serializing, but not necessarily when deserializing!)
         * One possibility would be to check the current token, and use that to
         * determine if we would output JSON Array, Object or scalar value.
         * Jackson 1.5 did NOT include any type information; but this seems wrong,
         * and so 1.6 WILL include type information.
         *<p>
         * Note that we just claim it is scalar; this should work ok and is simpler
         * than doing introspection on both serialization and deserialization.
         */
        @Override
        public final void serializeWithType(TokenBuffer value, JsonGenerator jgen, SerializerProvider provider,
                TypeSerializer typeSer)
            throws IOException, JsonGenerationException
        {
            typeSer.writeTypePrefixForScalar(value, jgen);
            serialize(value, jgen, provider);
            typeSer.writeTypeSuffixForScalar(value, jgen);
        }
        
        @Override
        public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        {
            /* 01-Jan-2010, tatu: Not 100% sure what we should say here:
             *   type is basically not known. This seems closest
             *   approximation
             */
            return createSchemaNode("any", true);
        }
    }    
}
