package org.codehaus.jackson.map.deser;

import java.util.*;
import java.util.concurrent.*;

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
        @SuppressWarnings("unused")
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
            return new EnumMapDeserializer(EnumResolver.constructFor(keyType.getRawClass()), valueDes);
        }

        /* Otherwise, generic handler works ok; need a key deserializer (null 
         * indicates 'default' here)
         */
        KeyDeserializer keyDes = (_typeString.equals(keyType)) ? null : p.findKeyDeserializer(keyType, this);

        /* But there is one more twist: if we are being asked to instantiate
         * an interface or abstract Map, we need to either find something
         * that implements the thing, or give up.
         *
         * Note that we do NOT try to guess based on secondary interfaces
         * here; that would probably not work correctly since casts would
         * fail later on (as the primary type is not the interface we'd
         * be implementing)
         */
        if (type.isInterface()) {
            /* Let's try to use the most commonly used sensible concrete
             * implementation. May need to add new types in future.
             */
            if (mapClass == Map.class) {
                mapClass = LinkedHashMap.class;
            } else if (mapClass == ConcurrentMap.class) {
                mapClass = ConcurrentHashMap.class;
            } else if (mapClass == ConcurrentNavigableMap.class) {
                mapClass = ConcurrentSkipListMap.class;
            } else if (mapClass == SortedMap.class
                       || mapClass == NavigableMap.class) {
                mapClass = TreeMap.class;
            } else {
                throw new IllegalArgumentException("Can not find a deserializer for Map interface type "+type);
            }
        } else if (type.isAbstract()) {
            /* any abstract classes that make sense? (JDK AbstractMap
             * is an impl detail, shouldn't really be used...)
             */
            throw new IllegalArgumentException("Can not find a deserializer for abstract Map type "+type);
        }
        return new MapDeserializer(mapClass, keyDes, valueDes);
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
        JsonDeserializer<?> des = new EnumDeserializer(EnumResolver.constructFor(type.getRawClass()));

        @SuppressWarnings("unchecked") 
        JsonDeserializer<Object> result = (JsonDeserializer<Object>) des;

        return result;
    }
}


