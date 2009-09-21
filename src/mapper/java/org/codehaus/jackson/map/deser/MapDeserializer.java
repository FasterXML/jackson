package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.*;

import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Basic serializer that can take Json "Object" structure and
 * construct a {@link java.util.Map} instance, with typed contents.
 *<p>
 * Note: for untyped content (one indicated by passing Object.class
 * as the type), {@link UntypedObjectDeserializer} is used instead.
 * It can also construct {@link java.util.Map}s, but not with specific
 * POJO types, only other containers and primitives/wrappers.
 */
public class MapDeserializer
    extends StdDeserializer<Map<Object,Object>>
    implements ResolvableDeserializer
{
    // // Configuration: typing, deserializers

    final JavaType _mapType;

    /**
     * Key deserializer used, if not null. If null, String from json
     * content is used as is.
     */
    final KeyDeserializer _keyDeserializer;

    /**
     * Value deserializer.
     */
    final JsonDeserializer<Object> _valueDeserializer;

    // // Instance construction settings:

    final Constructor<Map<Object,Object>> _defaultCtor;

    /**
     * If the Map is to be instantiated using non-default constructor
     * or factory method
     * that takes one or more named properties as argument(s),
     * this creator is used for instantiation.
     */
    protected Creator.PropertyBased _propertyBasedCreator;

    /*
    ////////////////////////////////////////////////////////////
    // Life-cycle
    ////////////////////////////////////////////////////////////
     */

    public MapDeserializer(JavaType mapType, Constructor<Map<Object,Object>> defCtor,
                           KeyDeserializer keyDeser, JsonDeserializer<Object> valueDeser)
    {
        super(Map.class);
        _mapType = mapType;
        _defaultCtor = defCtor;
        _keyDeserializer = keyDeser;
        _valueDeserializer = valueDeser;
    }

    /**
     * Method called to add constructor and/or factory method based
     * creators to be used with Map, instead of default constructor.
     */
    public void setCreators(CreatorContainer creators)
    {
        _propertyBasedCreator = creators.propertyBasedCreator();
    }

    /*
    /////////////////////////////////////////////////////////
    // Validation, post-processing
    /////////////////////////////////////////////////////////
     */

    public void validateCreators()
        throws JsonMappingException
    {
        if (_defaultCtor == null
            && _propertyBasedCreator == null) {
            throw new JsonMappingException("Can not create deserializer for Map type "+getMapClass().getName()+": no default/delegating constructor or factory methods found");
        }
    }

    /**
     * Method called to finalize setup of this deserializer,
     * after deserializer itself has been registered. This
     * is needed to handle recursive and transitive dependencies.
     */
    public void resolve(DeserializationConfig config, DeserializerProvider provider)
        throws JsonMappingException
    {
        // just need to worry about property-based one
        if (_propertyBasedCreator != null) {
            // Need to / should not create separate
            HashMap<JavaType, JsonDeserializer<Object>> seen = new HashMap<JavaType, JsonDeserializer<Object>>();
            for (SettableBeanProperty prop : _propertyBasedCreator.properties()) {
                prop.setValueDeserializer(findDeserializer(config, provider, prop.getType(), prop.getPropertyName(), seen));
            }
        }
    }

    /*
    ////////////////////////////////////////////////////////////
    // Deserializer API
    ////////////////////////////////////////////////////////////
     */

    @Override
    public Map<Object,Object> deserialize(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.mappingException(getMapClass());
        }
        if (_propertyBasedCreator != null) {
            return _deserializeUsingCreator(jp, ctxt);
        }
        Map<Object,Object> result;
        try {
            result = _defaultCtor.newInstance();
        } catch (Exception e) {
            throw ctxt.instantiationException(getMapClass(), e);
        }
        _readAndBind(jp, ctxt, result);
        return result;
    }

    @Override
    public Map<Object,Object> deserialize(JsonParser jp, DeserializationContext ctxt,
                                          Map<Object,Object> result)
        throws IOException, JsonProcessingException
    {
        // Ok: must point to START_OBJECT
        if (jp.getCurrentToken() != JsonToken.START_OBJECT) {
            throw ctxt.mappingException(getMapClass());
        }
        _readAndBind(jp, ctxt, result);
        return result;
    }

    /*
    /////////////////////////////////////////////////////////
    // Other public accessors
    /////////////////////////////////////////////////////////
     */

    @SuppressWarnings("unchecked")
    public final Class<?> getMapClass() { return (Class<Map<Object,Object>>) _mapType.getRawClass(); }

    @Override public JavaType getValueType() { return _mapType; }

    /*
    ////////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////////
     */

    protected final void _readAndBind(JsonParser jp, DeserializationContext ctxt,
                                      Map<Object,Object> result)
        throws IOException, JsonProcessingException
    {
        KeyDeserializer keyDes = _keyDeserializer;
        JsonDeserializer<Object> valueDes = _valueDeserializer;

        while ((jp.nextToken()) != JsonToken.END_OBJECT) {
            // Must point to field name
            String fieldName = jp.getCurrentName();
            Object key = (keyDes == null) ? fieldName : keyDes.deserializeKey(fieldName, ctxt);
            // And then the value...
            JsonToken t = jp.nextToken();
            // Note: must handle null explicitly here; value deserializers won't
            Object value = (t == JsonToken.VALUE_NULL) ? null : valueDes.deserialize(jp, ctxt);
            /* !!! 23-Dec-2008, tatu: should there be an option to verify
             *   that there are no duplicate field names? (and/or what
             *   to do, keep-first or keep-last)
             */
            result.put(key, value);
        }
    }

    @SuppressWarnings("unchecked") 
    public Map<Object,Object> _deserializeUsingCreator(JsonParser jp, DeserializationContext ctxt)
        throws IOException, JsonProcessingException
    {
        final Creator.PropertyBased creator = _propertyBasedCreator;
        PropertyValueBuffer buffer = creator.startBuilding(jp, ctxt);

        while (true) {
            // end of JSON object?
            if (jp.nextToken() == JsonToken.END_OBJECT) {
                // if so, can just construct and leave...
                return (Map<Object,Object>)creator.build(buffer);
            }
            String propName = jp.getCurrentName();
            // creator property?
            SettableBeanProperty prop = creator.findCreatorProperty(propName);
            if (prop != null) {
                // Last property to set?
                if (buffer.assignParameter(prop.getCreatorIndex(), prop.deserialize(jp, ctxt))) {
                    Map<Object,Object> result = (Map<Object,Object>)creator.build(buffer);
                    _readAndBind(jp, ctxt, result);
                    return result;
                }
                continue;
            }
            // other property? needs buffering
            String fieldName = jp.getCurrentName();
            Object key = (_keyDeserializer == null) ? fieldName : _keyDeserializer.deserializeKey(fieldName, ctxt);
            // And then the value...
            JsonToken t = jp.nextToken();
            // Note: must handle null explicitly here; value deserializers won't
            Object value = (t == JsonToken.VALUE_NULL) ? null : _valueDeserializer.deserialize(jp, ctxt);
            buffer.bufferMapProperty(key, value);
        }
    }
}
