package org.codehaus.jackson.map.jsontype.impl;

import java.io.IOException;

import org.codehaus.jackson.*;
import org.codehaus.jackson.annotate.JsonTypeInfo.As;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.util.JsonParserSequence;
import org.codehaus.jackson.util.TokenBuffer;

/**
 * Type deserializer used with {@link As#PROPERTY}
 * inclusion mechanism.
 * Uses regular form (additional key/value entry before actual data)
 * when typed object is expressed as JSON Object; otherwise behaves similar to how
 * {@link As#WRAPPER_ARRAY} works.
 * Latter is used if JSON representation is polymorphic
 * 
 * @since 1.5
 * @author tatu
 */
public class AsPropertyTypeDeserializer extends AsArrayTypeDeserializer
{
    protected final String _typePropertyName;

    @Deprecated // since 1.9
    public AsPropertyTypeDeserializer(JavaType bt, TypeIdResolver idRes, BeanProperty property,
            String typePropName) {
        this(bt, idRes, property, null, typePropName);
    }
    
    public AsPropertyTypeDeserializer(JavaType bt, TypeIdResolver idRes, BeanProperty property,
            Class<?> defaultImpl,
            String typePropName)
    {
        super(bt, idRes, property, defaultImpl);
        _typePropertyName = typePropName;
    }

    @Override
    public As getTypeInclusion() {
        return As.PROPERTY;
    }

    @Override
    public String getPropertyName() { return _typePropertyName; }

    /**
     * This is the trickiest thing to handle, since property we are looking
     * for may be anywhere...
     */
    @Override
    public Object deserializeTypedFromObject(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // but first, sanity check to ensure we have START_OBJECT or FIELD_NAME
        JsonToken t = jp.getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            t = jp.nextToken();
        } else if (t != JsonToken.FIELD_NAME) {
            throw ctxt.wrongTokenException(jp, JsonToken.START_OBJECT,
                    "need JSON Object to contain As.PROPERTY type information (for class "+baseTypeName()+")");
        }
        // Ok, let's try to find the property. But first, need token buffer...
        TokenBuffer tb = null;

        for (; t == JsonToken.FIELD_NAME; t = jp.nextToken()) {
            String name = jp.getCurrentName();
            jp.nextToken(); // to point to the value
            if (_typePropertyName.equals(name)) { // gotcha!
                String typeId = jp.getText();
                JsonDeserializer<Object> deser = _findDeserializer(ctxt, typeId);
                // deserializer should take care of closing END_OBJECT as well
               if (tb != null) {
                    jp = JsonParserSequence.createFlattened(tb.asParser(jp), jp);
                }
                /* Must point to the next value; tb had no current, jp
                 * pointed to VALUE_STRING:
                 */
                jp.nextToken(); // to skip past String value
                // deserializer should take care of closing END_OBJECT as well
                return deser.deserialize(jp, ctxt);
            }
            if (tb == null) {
                tb = new TokenBuffer(null);
            }
            tb.writeFieldName(name);
            tb.copyCurrentStructure(jp);
        }
        return _deserializeTypedUsingDefaultImpl(jp, ctxt, tb);
    }
    
    // off-lined to keep main method lean and meand...
    protected Object _deserializeTypedUsingDefaultImpl(JsonParser jp,
            DeserializationContext ctxt, TokenBuffer tb)
        throws IOException, JsonProcessingException
    {
        // As per [JACKSON-614], may have default implement to use
        if (_defaultImpl == null) { // if not, an error
            throw ctxt.wrongTokenException(jp, JsonToken.FIELD_NAME,
                    "missing property '"+_typePropertyName+"' that is to contain type id  (for class "+baseTypeName()+")");
        }
        JsonDeserializer<Object> deser = _findDefaultImplDeserializer(ctxt);
        if (tb != null) {
            tb.writeEndObject();
            jp = tb.asParser(jp);
            // must move to point to the first token:
            jp.nextToken();
        }
        return deser.deserialize(jp, ctxt);
    }

    /* As per [JACKSON-352], also need to re-route "unknown" version. Need to think
     * this through bit more in future, but for now this does address issue and has
     * no negative side effects (at least within existing unit test suite).
     */
    @Override
    public Object deserializeTypedFromAny(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        /* [JACKSON-387]: Sometimes, however, we get an array wrapper; specifically
         *   when an array or list has been serialized with type information.
         */
        if (jp.getCurrentToken() == JsonToken.START_ARRAY) {
            return super.deserializeTypedFromArray(jp, ctxt);
        }
        return deserializeTypedFromObject(jp, ctxt);
    }    
    
    // These are fine from base class:
    //public Object deserializeTypedArray(JsonParser jp, DeserializationContext ctxt)
    //public Object deserializeTypedScalar(JsonParser jp, DeserializationContext ctxt)    
}
