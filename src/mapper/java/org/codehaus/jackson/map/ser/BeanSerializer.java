package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collection;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.schema.SchemaAware;
import org.codehaus.jackson.schema.JsonSchema;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.map.*;

/**
 * Serializer class that can serialize arbitrary bean objects.
 *<p>
 * Implementation note: we will post-process resulting serializer,
 * to figure out actual serializers for final types. This must be
 * done from {@link #resolve} method, and NOT from constructor;
 * otherwise we could end up with an infinite loop.
 */
public class BeanSerializer
    extends JsonSerializer<Object>
    implements ResolvableSerializer, SchemaAware
{
    final protected String _className;

    final protected BeanPropertyWriter[] _props;

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
             *   StackOverflow: usually due to infinite loop. But that
             *   usually gets hidden within an InvocationTargetException...
             */
            Throwable t = e;
            while (t instanceof InvocationTargetException && t.getCause() != null) {
                t = t.getCause();
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            throw JsonMappingException.wrapWithPath(t, bean, _props[i].getName());
        } catch (StackOverflowError e) {
            /* 04-Sep-2009, tatu: Dealing with this is tricky, since we do not
             *   have many stack frames to spare... just one or two; can't
             *   make many calls.
             */
            JsonMappingException mapE = new JsonMappingException("Infinite recursion (StackOverflowError)");
            mapE.prependPath(new JsonMappingException.Reference(bean, _props[i].getName()));
            throw mapE;
        }
    }

    //@Override
    public JsonNode getSchema(SerializerProvider provider, Type typeHint)
            throws JsonMappingException
    {
        ObjectNode o = JsonNodeFactory.instance.objectNode();
        //todo: should the classname go in the title?
        //o.put("title", _className);
        o.put("type", "object");
        o.put("optional", true);
        ObjectNode propertiesNode = o.objectNode();
        for (int i = 0; i < _props.length; i++) {
            BeanPropertyWriter prop = _props[i];
            Type hint = prop.getSerializationType();
            if (hint == null) {
                hint = prop.getGenericPropertyType();
            }
            JsonSerializer<Object> ser = provider.findValueSerializer(prop.getSerializationType() == null ? prop.getReturnType() : prop.getSerializationType());
            JsonNode schemaNode = (ser instanceof SchemaAware) ?
                    ((SchemaAware) ser).getSchema(provider, hint) : 
                    JsonSchema.getDefaultSchemaNode();
            o.put("items", schemaNode);
            propertiesNode.put(prop.getName(), schemaNode);
        }
        o.put("properties", propertiesNode);
        return o;
    }

    /*
   ////////////////////////////////////////////////////////
   // ResolvableSerializer impl
   ////////////////////////////////////////////////////////
    */

    public void resolve(SerializerProvider provider)
        throws JsonMappingException
    {
        //AnnotationIntrospector ai = provider.getConfig().getAnnotationIntrospector();
        for (int i = 0, len = _props.length; i < len; ++i) {
            BeanPropertyWriter prop = _props[i];
            if (prop.hasSerializer()) {
                continue;
            }
            // Was the serialization type hard-coded? If so, use it
            Class<?> type = prop.getSerializationType();
            /* It not, we can use declared return type if and only if
             * declared type is final -- if not, we don't really know
             * the actual type until we get the instance.
             */
            if (type == null) {
                Class<?> rt = prop.getReturnType();
                if (!Modifier.isFinal(rt.getModifiers())) {
                    continue;
                }
                type = rt;
            }
            _props[i] = prop.withSerializer(provider.findValueSerializer(type));
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
