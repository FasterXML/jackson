package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link JsonDeserializerProvider}
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
public abstract class JsonDeserializerFactory
{
    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert Json scalar values (String,
     * number, boolean) into given Java type.
     *
     * @param type Type to be deserialized
     */
    public abstract <T> JsonDeserializer<T> createScalarDeserializer(Class<T> type);

    /**
     * Method called to create  a deserializer that can convert Json Array (== "list")
     * values into Java array values of specified type.
     *
     * @param containerType Expected Java array type (for example,
     *   <code>String[].class</code>)
     * @param elementType Expected type of values contained within
     *   container, which is usually same as what
     *   {@link Class#getComponentType} for the array type returns
     *   (but not always, if annotations indicate more accurate type)
     */
    public abstract <T> JsonDeserializer<T> createArrayDeserializer(Class<T> arrayType);

    /**
     * Method called to create  a deserializer that can convert Json Array (== "list")
     * values into given Java non-array container types.
     *
     * @param containerType Expected Java container type (for example,
     *   {@link java.util.ArrayList}.class)
     * @param elementType Expected type of values contained within
     *   container.
     */
    public abstract <T> JsonDeserializer<T> createArrayDeserializer(Class<T> containerType, Class<?> elementType);

   /**
     * Method called to create deserializer that can convert Json Object (== "map")
     * values into specified Java {@link java.util.Map} type.
     *
     * @param containerType Expected Java container type (for example,
     *   {@link java.util.ArrayList}.class)
     * @param keyType elementType Expected type of keys of the resulting
     *   Map.
     * @param valueType elementType Expected type of values of the resulting
     *   Map.
     */
    public abstract <T> JsonDeserializer<T> createMapDeserializer(Class<T> mapType, Class<?> keyType, Class<?> valueType);

   /**
     * Method called to create deserializer that can convert Json Object (== "map")
     * values into specified Java bean type.
     */
    public abstract <T> JsonDeserializer<T> createObjectDeserializer(Class<T> beanType);
}
