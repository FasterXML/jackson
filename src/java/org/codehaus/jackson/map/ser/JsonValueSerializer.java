package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.*;

/**
 * Serializer class that can serialize Object that have a
 * {@link org.codehaus.jackson.annotate.JsonValue} annotation to
 * indicate that serialization should be done by calling the method
 * annotated, and serializing result it returns.
 *<p>
 * Implementation note: we will post-process resulting serializer
 * (much like what is done with {@link BeanSerializer})
 * to figure out actual serializers for final types. This must be
 * done from {@link #resolve} method, and NOT from constructor;
 * otherwise we could end up with an infinite loop.
 */
public final class JsonValueSerializer
    extends JsonSerializer<Object>
    implements ResolvableSerializer
{
    final Method _accessorMethod;

    JsonSerializer<Object> _serializer;

    /**
     * @param ser Explicit serializer to use, if caller knows it (which
     *   occurs if and only if the "value method" was annotated with
     *  {@link org.codehaus.jackson.annotate.JsonSerialize#using}), otherwise
     *  null
     */
    public JsonValueSerializer(Method valueMethod, JsonSerializer<Object> ser)
    {
        _accessorMethod = valueMethod;
        _serializer = ser;
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
                ser = _serializer;
                if (ser == null) {
                    ser = prov.findValueSerializer(value.getClass());
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
            throw JsonMappingException.wrapWithPath(t, bean, _accessorMethod.getName()+"()");
        }
    }
    
    /*
    ////////////////////////////////////////////////////////
    // ResolvableSerializer impl
    ////////////////////////////////////////////////////////
     */

    /**
     * We can try to find the actual serializer for value, if we can
     * statically figure out what the result type must be.
     */
    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        if (_serializer == null) {
            Class<?> rt = _accessorMethod.getReturnType();
            /* Note: we can only assign serializer statically if the
             * declared type is final -- if not, we don't really know
             * the actual type until we get the instance.
             */
            if (Modifier.isFinal(rt.getModifiers())) {
                _serializer = provider.findValueSerializer(rt);
            }
        }
    }

    /*
    ////////////////////////////////////////////////////////
    // Other methods
    ////////////////////////////////////////////////////////
     */

    @Override
    public String toString() {
        return "(@JsonValue serializer for method "+_accessorMethod.getDeclaringClass()+"#"+_accessorMethod.getName()+")";
    }
}
