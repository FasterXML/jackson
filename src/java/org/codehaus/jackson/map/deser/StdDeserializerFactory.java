package org.codehaus.jackson.map.deser;

import java.io.IOException;
import java.util.*;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonDeserializerFactory;
import org.codehaus.jackson.map.JsonDeserializerProvider;
import org.codehaus.jackson.map.type.*;

/**
 * Factory class that can provide deserializers for standard JDK classes,
 * as well as custom classes that extend standard classes or implement
 * one of "well-known" interfaces (such as {@link java.util.Collection}).
 *<p>
 * Since all the deserializers are eagerly instantiated, and there is
 * no additional introspection or customazibility of these types,
 * this factory is stateless. This means that other delegating
 * factories (or {@link JsonDeserializerProvider}s) can just use the
 * shared singleton instance via static {@link #instance} field.
 */
public class StdDeserializerFactory
    extends JsonDeserializerFactory
{
    /*
    ////////////////////////////////////////////////////////////
    // Configuration, lookup tables/maps
    ////////////////////////////////////////////////////////////
     */

    /**
     * We will pre-create serializers for common non-structured
     * (that is things other than Collection, Map or array)
     * types.
     */
    final static HashMap<JavaType, JsonDeserializer<Object>> _concrete = 
        new HashMap<JavaType, JsonDeserializer<Object>>();
    static {
        // !!! TODO
    }

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

    /**
     * Main serializer constructor method. The base implementation within
     * this class first calls a fast lookup method that can find serializers
     * for well-known JDK classes; and if that fails, a slower one that
     * tries to check out which interfaces given Class implements.
     * Sub-classes can (and do) change this behavior to alter behavior.
     */
    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> createDeserializer(JavaType type, JsonDeserializerProvider p)
    {
        // First, fast lookup for exact type:
        JsonDeserializer<Object> ser = _concrete.get(type);
        if (ser != null) {
            return ser;
        }
        // And lacking that, divide by type
        if (type instanceof ArrayType) {
            return createArrayDeserializer((ArrayType) type, p);
        }
        if (type instanceof MapType) {
            return createMapDeserializer((MapType) type, p);
        }
        if (type instanceof CollectionType) {
            return createCollectionDeserializer((CollectionType) type, p);
        }
        return createBeanDeserializer(type, p);
    }

    /*
    ////////////////////////////////////////////////////////////
    // Specific type-specific factory methods
    ////////////////////////////////////////////////////////////
     */

    protected JsonDeserializer<Object> createArrayDeserializer(ArrayType type, JsonDeserializerProvider p)
    {
        Class<?> arrayClass = type.getRawClass();

        // First, special type(s):

        // EnumMap requires special handling
        if (EnumMap.class.isAssignableFrom(arrayClass)) {
        }

        // !!! TBI
        return null;
    }

    protected JsonDeserializer<Object> createMapDeserializer(MapType type, JsonDeserializerProvider p)
    {
        // !!! TBI
        return null;
    }

    protected JsonDeserializer<Object> createCollectionDeserializer(CollectionType type, JsonDeserializerProvider p)
    {
        // !!! TBI
        return null;
    }

    protected JsonDeserializer<Object> createBeanDeserializer(JavaType type, JsonDeserializerProvider p)
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


