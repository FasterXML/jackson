package org.codehaus.jackson.map;

import org.codehaus.jackson.type.JavaType;

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
     * @param referrer Type that contains the value, if any: null for
     *   root-level object.
     * @param refPropName Logical name of the property within instance of
     *   <code>referrer</code>, if through a property (field). Null for
     *   Collection and Array types, where reference is not through a
     *   field.
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable deserializer; including that of not
     *   finding any serializer
     */
    public abstract JsonDeserializer<Object> findValueDeserializer(JavaType type,
                                                                   JavaType referrer, String refPropName)
        throws JsonMappingException;

    /**
     * Method called to get hold of a deserializer to use for deserializing
     * keys for {@link java.util.Map}.
     *
     * @throws JsonMappingException if there are fatal problems with
     *   accessing suitable key deserializer; including that of not
     *   finding any serializer
     */
    public abstract KeyDeserializer findKeyDeserializer(JavaType type)
        throws JsonMappingException;

    /**
     * Method called to find out whether provider would be able to find
     * a deserializer for given type, using a root reference (i.e. not
     * through fields or membership in an array or collection)
     */
    public abstract boolean hasValueDeserializerFor(JavaType type);
}
