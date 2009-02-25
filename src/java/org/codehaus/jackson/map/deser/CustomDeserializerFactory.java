package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.ser.ClassKey;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Deserializer factory implementation that allows for configuring
 * mapping between types and deserializers to use, by using
 * multiple types of overrides. Existing mappings established by
 * {@link BeanDeserializerFactory} (and its super class,
 * {@link BasicDeserializerFactory}) are used if no overrides are
 * defined.
 *<p>
 * Unlike base deserializer factories,
 * this factory is stateful because
 * of configuration settings. It is thread-safe, however, as long as
 * all configuration as done before using the factory -- a single
 * instance can be shared between providers and mappers.
 *<p>
 * Configurations currently available are:
 *<ul>
 * <li>Ability to define "mix-in annotations": associations between types
 *   (classes, interfaces) to deserialize, and a "mix-in" type which will
 *   be used so that all of its annotations are added to the deserialized
 *   type. Mixed-in annotations have priority over annotations that the
 *   deserialized type has. In effect this allows for overriding annotations
 *   types have; this is useful when type definition itself can not be
 *   modified
 *  </li>
 */
public class CustomDeserializerFactory
    extends BeanDeserializerFactory
{
    /*
    ////////////////////////////////////////////////////
    // Configuration
    ////////////////////////////////////////////////////
     */

    /**
     * Factory to use if we do not have a registered deserializer for
     * given type
     */
    final DeserializerFactory _fallbackFactory;

    /*
    //////////////////////////////////////////////////////////
    // Configuration: mappings that define "mix-in annotations"
    //////////////////////////////////////////////////////////
     */

    /**
     * Mapping that defines how to apply mix-in annotations: key is
     * the type to received additional annotations, and value is the
     * type that has annotations to "mix in".
     */
    HashMap<ClassKey,ClassKey> _mixInAnnotations;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle, constructors
    ////////////////////////////////////////////////////
     */

    /**
     * Default constructor will use {@link BeanSerializerFactory}
     * as the fallback serializer factory.
     */
    public CustomDeserializerFactory() {
        this(BeanDeserializerFactory.instance);
    }

    /**
     * Default constructor will use {@link BeanSerializerFactory}
     * as the fallback serializer factory.
     */
    public CustomDeserializerFactory(DeserializerFactory fallbackFactory) {
        _fallbackFactory = fallbackFactory;
    }

    /*
    ////////////////////////////////////////////////////
    // DeserializerFactory API
    ////////////////////////////////////////////////////
     */

    public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return _fallbackFactory.createBeanDeserializer(type, p);
    }

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert Json content into values of
     * specified Java type.
     *
     * @param type Type to be deserialized
     * @param p Provider that can be called to create deserializers for
     *   contained member types
     */
    public JsonDeserializer<?> createArrayDeserializer(ArrayType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return _fallbackFactory.createArrayDeserializer(type, p);
    }

    public JsonDeserializer<?> createCollectionDeserializer(CollectionType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return _fallbackFactory.createCollectionDeserializer(type, p);
    }

    public JsonDeserializer<?> createEnumDeserializer(SimpleType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return _fallbackFactory.createEnumDeserializer(type, p);
    }

    public JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return _fallbackFactory.createMapDeserializer(type, p);
    }

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */
}
