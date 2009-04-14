package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.*;

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

    final BeanPropertyWriter[] _props;

    public BeanSerializer(Class<?> type, BeanPropertyWriter[] props)
    {
        // sanity check
        if (props.length == 0) {
            throw new IllegalArgumentException("Can not create BeanSerializer for type that has no properties");
        }
        _props = props;
        // let's store this for debugging
        _className = type.getName();
    }

    public BeanSerializer(Class<?> type, Collection<BeanPropertyWriter> props)
    {
        this(type, props.toArray(new BeanPropertyWriter[props.size()]));
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
            jgen.writeEndObject();
        } catch (IOException ioe) {
            // first: IOExceptions (and sub-classes) to be passed as is
            throw ioe;
        } catch (Exception e) {
            // [JACKSON-55] Need to add reference information
            /* 05-Mar-2009, tatu: But one nasty edge is when we get
             *   StackOverflow: usually due to infinite loop. But that gets
             *   hidden within an InvocationTargetException...
             */
            Throwable t = e;
            while (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw JsonMappingException.wrapWithPath(t, bean, _props[i].getName());
        }
    }

    /*
    ////////////////////////////////////////////////////////
    // ResolvableSerializer impl
    ////////////////////////////////////////////////////////
     */

    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        for (int i = 0, len = _props.length; i < len; ++i) {
            BeanPropertyWriter prop = _props[i];
            if (!prop.hasSerializer()) {
                Class<?> rt = prop.getReturnType();
                /* Note: we can only assign serializer statically if the
                 * declared type is final -- if not, we don't really know
                 * the actual type until we get the instance.
                 */
                if (Modifier.isFinal(rt.getModifiers())) {
                    _props[i] = prop.withSerializer(provider.findValueSerializer(rt));
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
