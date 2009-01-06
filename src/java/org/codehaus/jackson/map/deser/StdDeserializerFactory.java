package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.DeserializerFactory;
import org.codehaus.jackson.map.DeserializerProvider;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.type.*;

/**
 * Factory class that can provide deserializers for standard JDK classes,
 * as well as custom classes that extend standard classes or implement
 * one of "well-known" interfaces (such as {@link java.util.Collection}).
 *<p>
 * Since all the deserializers are eagerly instantiated, and there is
 * no additional introspection or customazibility of these types,
 * this factory is stateless. This means that other delegating
 * factories (or {@link DeserializerProvider}s) can just use the
 * shared singleton instance via static {@link #instance} field.
 */
public class StdDeserializerFactory
    extends DeserializerFactory
{
    // // Can cache some types

    final static JavaType _typeObject = TypeFactory.instance.fromClass(Object.class);
    final static JavaType _typeString = TypeFactory.instance.fromClass(String.class);

    /*
    ////////////////////////////////////////////////////////////
    // Life cycle
    ////////////////////////////////////////////////////////////
     */

    public final static StdDeserializerFactory instance = new StdDeserializerFactory();

    /**
     * We will provide default constructor to allow sub-classing,
     * but make it protected so that no non-singleton instances of
     * the class will be instantiated.
     */
    protected StdDeserializerFactory() { }

    /*
    ////////////////////////////////////////////////////////////
    // JsonDeserializerFactory impl
    ////////////////////////////////////////////////////////////
     */

    public JsonDeserializer<Object> createArrayDeserializer(ArrayType type, DeserializerProvider p)
    {
        Class<?> arrayClass = type.getRawClass();

        // First, special type(s):

        // !!! TBI
        return null;
    }

    public JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p)
    {
        JavaType keyType = type.getKeyType();
        // Value handling is identical for all, so:
        JavaType valueType = type.getValueType();
        JsonDeserializer<Object> valueDes = p.findValueDeserializer(valueType, this);

        Class<?> mapClass = type.getRawClass();
        // But EnumMap requires special handling for keys
        if (EnumMap.class.isAssignableFrom(mapClass)) {
            return new EnumMapDeserializer(new EnumResolver(keyType.getRawClass()), valueDes);
        }

        /* Otherwise, generic handler works ok; need a key deserializer (null 
         * indicates 'default' here)
         */
        KeyDeserializer keyDes = (_typeString.equals(keyType)) ? null : p.findKeyDeserializer(keyType, this);
        return new MapDeserializer(type, keyDes, valueDes);
    }

    public JsonDeserializer<Object> createCollectionDeserializer(CollectionType type, DeserializerProvider p)
    {
        // !!! TBI
        return null;
    }

    public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
    {
        // !!! TBI
        return null;
    }

    public JsonDeserializer<Object> createEnumDeserializer(SimpleType type, DeserializerProvider p)
    {
        // !!! TBI
        return null;
    }

    /*
    ////////////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////////////
     */

    /*
    ////////////////////////////////////////////////////////////
    // Concrete deserializers
    ////////////////////////////////////////////////////////////
     */
}


