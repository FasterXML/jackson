package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.schema.SchemaAware;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.type.JavaType;

/**
 * Serializer class that can serialize Object that have a
 * {@link org.codehaus.jackson.annotate.JsonValue} annotation to
 * indicate that serialization should be done by calling the method
 * annotated, and serializing result it returns.
 * <p/>
 * Implementation note: we will post-process resulting serializer
 * (much like what is done with {@link BeanSerializer})
 * to figure out actual serializers for final types. This must be
 * done from {@link #resolve} method, and NOT from constructor;
 * otherwise we could end up with an infinite loop.
 */
public final class JsonValueSerializer
    extends SerializerBase<Object>
    implements ResolvableSerializer, SchemaAware
{
    final Method _accessorMethod;

    protected JsonSerializer<Object> _valueSerializer;
    
    /**
     * @param ser Explicit serializer to use, if caller knows it (which
     *            occurs if and only if the "value method" was annotated with
     *            {@link org.codehaus.jackson.map.annotate.JsonSerialize#using}), otherwise
     *            null
     */
    public JsonValueSerializer(Method valueMethod, JsonSerializer<Object> ser)
    {
        super(Object.class);
        _accessorMethod = valueMethod;
        _valueSerializer = ser;
    }

    public void serialize(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws IOException, JsonGenerationException
    {
        try {
            Object value = _accessorMethod.invoke(bean);
            JsonSerializer<Object> ser;

            if (value == null) {
                ser = prov.getNullValueSerializer();
            } else {
                ser = _valueSerializer;
                if (ser == null) {
                    Class<?> c = value.getClass();
                    /* 10-Mar-2010, tatu: Ideally we would actually separate out type
                     *   serializer from value serializer; but, alas, there's no access
                     *   to serializer factory at this point... 
                     */
                    // let's cache it, may be needed soon again
                    ser = prov.findTypedValueSerializer(c, true);
                }
            }
            ser.serialize(value, jgen, prov);
        } catch (IOException ioe) {
            throw ioe;
        } catch (Exception e) {
            Throwable t = e;
            // Need to unwrap this specific type, to see infinite recursion...
            while (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            // Errors shouldn't be wrapped (and often can't, as well)
            if (t instanceof Error) {
                throw (Error) t;
            }
            // let's try to indicate the path best we can...
            throw JsonMappingException.wrapWithPath(t, bean, _accessorMethod.getName() + "()");
        }
    }

    //@Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException
    {
        return (_valueSerializer instanceof SchemaAware) ?
                ((SchemaAware) _valueSerializer).getSchema(provider, null) :
                JsonSchema.getDefaultSchemaNode();
    }
    
    /*
    /*******************************************************
    /* ResolvableSerializer impl
    /*******************************************************
     */

    /**
     * We can try to find the actual serializer for value, if we can
     * statically figure out what the result type must be.
     */
    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        if (_valueSerializer == null) {
            /* Note: we can only assign serializer statically if the
             * declared type is final -- if not, we don't really know
             * the actual type until we get the instance.
             */
            // 10-Mar-2010, tatu: Except if static typing is to be used
            if (provider.isEnabled(SerializationConfig.Feature.USE_STATIC_TYPING)
                    || Modifier.isFinal(_accessorMethod.getReturnType().getModifiers())) {
                JavaType t = TypeFactory.type(_accessorMethod.getGenericReturnType());
                // false -> no need to cache
                /* 10-Mar-2010, tatu: Ideally we would actually separate out type
                 *   serializer from value serializer; but, alas, there's no access
                 *   to serializer factory at this point... 
                 */
                _valueSerializer = provider.findTypedValueSerializer(t, false);
            }
        }
    }

    /*
    /*******************************************************
    /* Other methods
    /*******************************************************
     */

    @Override
    public String toString()
    {
        return "(@JsonValue serializer for method " + _accessorMethod.getDeclaringClass() + "#" + _accessorMethod.getName() + ")";
    }
}
