package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.ResolvableSerializer;

/**
 * Serializer class that can serialize arbitrary bean objects.
 *<p>
 * Implementation note: we will post-process resulting serializer,
 * to figure out actual serializers for final types. This must be
 * done from {@link #resolve} method, and NOT from constructor;
 * otherwise we could end up with an infinite loop.
 */
public final class BeanSerializer
    extends JsonSerializer<Object>
    implements ResolvableSerializer
{
    final String _className;

    final WritableBeanProperty[] _props;

    public BeanSerializer(Class<?> type, WritableBeanProperty[] props)
    {
        // sanity check
        if (props.length == 0) {
            throw new IllegalArgumentException("Can not create BeanSerializer for type that has no properties");
        }
        _props = props;
        // let's store this for debugging
        _className = type.getName();
    }

    public BeanSerializer(Class<?> type, Collection<WritableBeanProperty> props)
    {
        this(type, props.toArray(new WritableBeanProperty[props.size()]));
    }

    public void serialize(Object bean, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException
    {
        jgen.writeStartObject();

        int i = 0;
        try {
            for (final int len = _props.length; i < len; ++i) {
                _props[i].serializeAsField(bean, jgen, provider);
            }
        } catch (IllegalAccessException e1) {
            throw new JsonGenerationException("Failed to serialize "+_props[i]+": "+e1.getMessage(), e1);
        } catch (InvocationTargetException e2) {
            throw new JsonGenerationException("Failed to serialize "+_props[i]+": "+e2.getMessage(), e2);
        }

        jgen.writeEndObject();
    }

    /*
    ////////////////////////////////////////////////////////
    // ResolvableSerializer impl
    ////////////////////////////////////////////////////////
     */

    public void resolve(SerializerProvider provider)
    {
        for (WritableBeanProperty prop : _props) {
            if (!prop.hasSerializer()) {
                Class<?> rt = prop.getReturnType();
                if (Modifier.isFinal(rt.getModifiers())) {
                    JsonSerializer<Object> ser = provider.findValueSerializer(rt);
                    prop.assignSerializer(ser);
                    continue;
                }
            }
        }
    }

    /*
    ////////////////////////////////////////////////////////
    // Std methods
    ////////////////////////////////////////////////////////
     */

    @Override
    public String toString()
    {
        return "BeanSerializer for "+_className;
    }
}
