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
    // API for serialization (Object-to-JSON)
    /////////////////////////////////////////////////
     */

    /**
     * Method to deserialize JSON content into a non-container
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
     * Method to deserialize JSON content into a Java type, reference
     * to which is passed as argument. Type is passed using so-called
     * "super type token" (see )
     * and specifically needs to be used if the root type is a 
     * parameterized (generic) container type.
     */
    public abstract <T> T readValue(JsonParser jp, TypeReference<?> valueTypeRef)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize JSON content as tree expressed
     * using set of {@link JsonNode} instances. Returns
     * root of the resulting tree (where root can consist
     * of just a single node if the current event is a
     * value event, not container).
     */
    public abstract <T> T readValue(JsonParser jp, JavaType valueType)
        throws IOException, JsonProcessingException;

    /**
     * Method to deserialize JSON content as tree expressed
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

    /*
    /////////////////////////////////////////////////
    // API for Tree Model handling
    /////////////////////////////////////////////////
     */

    /**
     * Method for construct root level Object nodes
     * for Tree Model instances.
     *
     * @since 1.2
     */
    public abstract JsonNode createObjectNode();

    /**
     * Method for construct root level Array nodes
     * for Tree Model instances.
     *
     * @since 1.2
     */
    public abstract JsonNode createArrayNode();

    /**
     * Method for constructing a {@link JsonParser} for reading
     * contents of a JSON tree, as if it was external serialized
     * JSON content.
     *
     * @since 1.3
     */
    public abstract JsonParser treeAsTokens(JsonNode n);

    /*
     * Method for constructing a {@link JsonGenerator} that can
     * be used to add content to a JSON tree.
     *
     * @param containerNode Container node to add contents to via created generator.
     *   If node is not a container node (as per {@link JsonNode#isContainerNode}),
     *   {@link IllegalArgumentException} will be thrown
     *
c    public abstract JsonGenerator treeFromTokens(JsonNode containerNode)
        throws IllegalArgumentException;
    */

    /**
     * Convenience method for converting given JSON tree into instance of specified
     * value type. This is equivalent to first constructing a {@link JsonParser} to
     * iterate over contents of the tree, and using that parser for data binding.
     * 
     * @since 1.3
     */
    public abstract <T> T treeToValue(JsonNode n, Class<T> valueType)
        throws IOException, JsonProcessingException;
}
