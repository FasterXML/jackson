package org.codehaus.jackson.map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Abstract class that defines API used by {@link DeserializerProvider}
 * to obtain actual
 * {@link JsonDeserializer} instances from multiple distinct factories.
 *<p>
 * Since there are multiple broad categories of deserializers, there are 
 * multiple factory methods:
 *<ul>
 * <li>There is one method to support Json scalar types: here access is
 *   by declared value type
 *  </li>
 * <li>For Json "Array" type, we need 2 methods: one to deal with expected
 *   Java arrays; and the other for other Java containers (Lists, Sets)
 *  </li>
 * <li>For Json "Object" type, we need 2 methods: one to deal with
 *   expected Java {@link java.util.Map}s, and another for actual
 *   Java objects (beans)
 *  </li>
 * </ul>
 *<p>
 * All above methods take 2 type arguments, except for the first one
 * which takes just a single argument.
 */
public abstract class DeserializerFactory
{
    /*
    /////////////////////////////////////////////////////////
    // Basic DeserializerFactory API:
    /////////////////////////////////////////////////////////
     */

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert Json content into values of
     * specified Java "bean" (POJO) type.
     * At this point it is known that the type is not otherwise recognized
     * as one of structured types (array, Collection, Map) or a well-known
     * JDK type (enum, primitives/wrappers, String); this method only
     * gets called if other options are exhausted. This also means that
     * this method can be overridden to add support for custom types.
     *
     * @param type Type to be deserialized
     * @param p Provider that can be called to create deserializers for
     *   contained member types
     */
    public abstract JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, JavaType type, DeserializerProvider p)
        throws JsonMappingException;

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert Json content into values of
     * specified Java type.
     *
     * @param type Type to be deserialized
     * @param p Provider that can be called to create deserializers for
     *   contained member types
     */
    public abstract JsonDeserializer<?> createArrayDeserializer(DeserializationConfig config, ArrayType type, DeserializerProvider p)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createCollectionDeserializer(DeserializationConfig config, CollectionType type, DeserializerProvider p)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createEnumDeserializer(DeserializationConfig config, Class<?> enumClass, DeserializerProvider p)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createMapDeserializer(DeserializationConfig config, MapType type, DeserializerProvider p)
        throws JsonMappingException;

    /**
     * Method called to create and return a deserializer that can construct
     * JsonNode(s) from Json content.
     */
    public abstract JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config, Class<? extends JsonNode> nodeClass, DeserializerProvider p)
        throws JsonMappingException;
}
