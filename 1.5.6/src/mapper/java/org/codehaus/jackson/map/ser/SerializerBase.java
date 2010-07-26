package org.codehaus.jackson.map.ser;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.node.JsonNodeFactory;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.schema.SchemaAware;

/**
 * Base class used by all standard serializers. Provides some convenience
 * methods for implementing {@link SchemaAware}
 */
public abstract class SerializerBase<T>
    extends JsonSerializer<T>
    implements SchemaAware
{
    protected final Class<T> _handledType;
    
    protected SerializerBase(Class<T> t) {
        _handledType = t;
    }

    /**
     * Alternate constructor that is (alas!) needed to work
     * around kinks of generic type handling
     */
    @SuppressWarnings("unchecked")
    protected SerializerBase(Class<?> t, boolean dummy) {
        _handledType = (Class<T>) t;
    }

    public final Class<T> handledType() { return _handledType; }
    
    public abstract void serialize(T value, JsonGenerator jgen, SerializerProvider provider)
        throws IOException, JsonGenerationException;

    public abstract JsonNode getSchema(SerializerProvider provider, Type typeHint)
        throws JsonMappingException;
    
    protected ObjectNode createObjectNode() {
        return JsonNodeFactory.instance.objectNode();
    }
    
    protected ObjectNode createSchemaNode(String type)
    {
        ObjectNode schema = createObjectNode();
        schema.put("type", type);
        return schema;
    }
    
    protected ObjectNode createSchemaNode(String type, boolean isOptional)
    {
        ObjectNode schema = createSchemaNode(type);
        schema.put("optional", isOptional);
        return schema;
    }

    /**
     * Method that will modify caught exception (passed in as argument)
     * as necessary to include reference information, and to ensure it
     * is a subtype of {@link IOException}, or an unchecked exception.
     *<p>
     * Rules for wrapping and unwrapping are bit complicated; essentially:
     *<ul>
     * <li>Errors are to be passed as is (if uncovered via unwrapping)
     * <li>"Plain" IOExceptions (ones that are not of type
     *   {@link JsonMappingException} are to be passed as is
     *</ul>
     */
    public void wrapAndThrow(Throwable t, Object bean, String fieldName)
        throws IOException
    {
        /* 05-Mar-2009, tatu: But one nasty edge is when we get
         *   StackOverflow: usually due to infinite loop. But that
         *   usually gets hidden within an InvocationTargetException...
         */
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // Ditto for IOExceptions... except for mapping exceptions!
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, fieldName);
    }

    public void wrapAndThrow(Throwable t, Object bean, int index)
        throws IOException
    {
        while (t instanceof InvocationTargetException && t.getCause() != null) {
            t = t.getCause();
        }
        // Errors and "plain" IOExceptions to be passed as is
        if (t instanceof Error) {
            throw (Error) t;
        }
        // Ditto for IOExceptions... except for mapping exceptions!
        if (t instanceof IOException && !(t instanceof JsonMappingException)) {
            throw (IOException) t;
        }
        // [JACKSON-55] Need to add reference information
        throw JsonMappingException.wrapWithPath(t, bean, index);
    }
}
