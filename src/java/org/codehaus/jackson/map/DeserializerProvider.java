package org.codehaus.jackson.map;

import org.codehaus.jackson.map.type.JavaType;

/**
 * Abstract class that defines API used by {@link ObjectMapper} and
 * {@link JsonDeserializer}s to obtain deserializers capable of
 * re-constructing instances of handled type from Json content.
 */
public abstract class DeserializerProvider
{
    protected DeserializerProvider() { }

    /*
    //////////////////////////////////////////////////////
    // General deserializer locating method
    //////////////////////////////////////////////////////
     */

    /**
     * Method called to get hold of a deserializer for a value of given type;
     * or if no such deserializer can be found, a default handler (which
     * may do a best-effort generic serialization or just simply
     * throw an exception when invoked).
     *<p>
     * Note: this method is only called for value types; not for keys.
     * Key deserializers can be accessed using {@link #findKeyDeserializer}.
     *
     * @param type Declared type of the value to deserializer (obtained using
     *   'setter' method signature and/or type annotations
     * @param f Factory that is to be used for constructing type, if provider
     *   does not have a cached instance
     * @param referrer Type that contains the value, if any: null for
     *   root-level object.
     * @param refPropName Logical name of the property within instance of
     *   <code>referrer</code>, if through a property (field). Null for
     *   Collection and Array types, where reference is not through a
     *   field.
     */
    public abstract JsonDeserializer<Object> findValueDeserializer(JavaType type, DeserializerFactory f,
                                                                   JavaType referrer, String refPropName);

    /**
     * Method called to get hold of a deserializer to use for deserializing
     * keys for {@link java.util.Map}.
     */
    public abstract KeyDeserializer findKeyDeserializer(JavaType type);
}
