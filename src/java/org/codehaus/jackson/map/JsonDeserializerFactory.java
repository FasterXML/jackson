package org.codehaus.jackson.map;

import org.codehaus.jackson.map.type.JavaType;

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
     * reuse) a deserializer that can convert Json content into values of
     * specified Java type.
     *
     * @param type Type to be deserialized
     */
    public abstract <T> JsonDeserializer<T> createDeserializer(JavaType type);
}
