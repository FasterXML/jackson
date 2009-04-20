package org.codehaus.jackson;

import java.io.IOException;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

/**
 * Abstract class that defines the interface that {@link JsonParser} and
 * {@link JsonGenerator} use to serialize and deserialize regular
 * Java objects (POJOs aka Beans).
 *<p>
 * The standard implementation of this class is
 * {@link org.codehaus.jackson.map.ObjectMapper}.
 */
public abstract class ObjectCodec
{
    protected ObjectCodec() { }

    /*
    /////////////////////////////////////////////////
    // API for serialization (Object-to-Json)
    /////////////////////////////////////////////////
     */

    /**
     * Method to deserialize Json content into a non-container
     * type (it can be an array type, however): typically a bean, array
     * or a wrapper type (like {@link java.lang.Boolean}).
     *<p>
     * Note: this method should NOT be used if the result type is a
     * container ({@link java.util.Collection} or {@link java.util.Map}.
     * The reason is that due to type erasure, key and value types
     * can not be introspected when using this method.
     */
    public abstract <T> T readValue(JsonParser jp, Class<T> valueType)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize Json content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     */
    public abstract <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize Json content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     */
    public abstract <T> T readValue(JsonParser jp, JavaType valueType)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize Json content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     */
    public abstract JsonNode readTree(JsonParser jp)
        throws IOException, JsonProcessingException;
    
    /*
    /////////////////////////////////////////////////
    // API for de-serialization (Json-to-Object)
    /////////////////////////////////////////////////
     */

    /**
     * Method to serialize given Java Object, using generator
     * provided.
     */
    public abstract void writeValue(JsonGenerator jgen, Object value)
        throws IOException, JsonProcessingException;

    /**
     * Method to serialize given Json Tree, using generator
     * provided.
     */
    public abstract void writeTree(JsonGenerator jgen, JsonNode rootNode)
        throws IOException, JsonProcessingException;
}
