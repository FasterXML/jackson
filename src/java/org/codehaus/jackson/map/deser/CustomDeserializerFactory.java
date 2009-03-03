package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.map.DeserializerFactory;
import org.codehaus.jackson.map.type.*;

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
    // Configuration, basic
    ////////////////////////////////////////////////////
     */

    /**
     * Features (of type {@link DeserializerFactory.Feature})
     * that are enabled
     */
    private int _features = DEFAULT_FEATURE_FLAGS;

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

    public CustomDeserializerFactory() { super(); }

    /*
    ////////////////////////////////////////////////////
    // Configuration: on/off features
    ////////////////////////////////////////////////////
     */

    /**
     * Method for enabling specified  features
     * (check
     * {@link org.codehaus.jackson.map.DeserializerFactory.Feature}
     * for list of features)
     */
    public void enableFeature(Feature f) {
        _features |= f.getMask();
    }

    /**
     * Method for disabling specified  features
     * (check
     * {@link org.codehaus.jackson.map.DeserializerFactory.Feature}
     * for list of features)
     */
    public void disableFeature(Feature f) {
        _features &= ~f.getMask();
    }

    public void setFeature(Feature f, boolean state)
    {
        if (state) {
            enableFeature(f);
        } else {
            disableFeature(f);
        }
    }

    /**
     * To make features configurable, need to override this method.
     */
    @Override
    protected int _getFeatures() { return _features; }

    /*
    ////////////////////////////////////////////////////
    // DeserializerFactory API
    ////////////////////////////////////////////////////
     */

    //public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p) throws JsonMappingException;

    //public JsonDeserializer<?> createArrayDeserializer(ArrayType type, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createCollectionDeserializer(CollectionType type, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createEnumDeserializer(Class<?> enumClass, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createTreeDeserializer(Class<? extends JsonNode> nodeClass, DeserializerProvider p) throws JsonMappingException

    /*
    ////////////////////////////////////////////////////
    // Internal methods
    ////////////////////////////////////////////////////
     */
}
