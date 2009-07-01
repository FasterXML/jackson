package org.codehaus.jackson.map.deser;

import java.util.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.*;
import org.codehaus.jackson.map.type.*;

/**
 * Deserializer factory implementation that allows for configuring
 * mapping between types and deserializers to use, by using
 * multiple types of overrides. Existing mappings established by
 * {@link BeanDeserializerFactory} (and its super class,
 * {@link BasicDeserializerFactory}) are used if no overrides are
 * defined.
 *<p>
 * Unlike base deserializer factories, this factory is stateful because
 * of configuration settings. It is thread-safe, however, as long as
 * all configuration as done before using the factory -- a single
 * instance can be shared between providers and mappers.
 *<p>
 * Configurations currently available are:
 *<ul>
 * <li>Ability to define explicit mappings between simple non-generic
 *   classes/interfaces and deserializers to use for deserializing
 *   instance of these classes. Mappings are one-to-one (i.e. there is
 *   no "generic" variant for handling sub- or super-classes/interfaces).
 *  </li>
 *</ul>
 *<p>
 * In near future, following features are planned to be added:
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
    // Configuration, direct/special mappings
    ////////////////////////////////////////////////////
     */

    /**
     * Direct mappings that are used for exact class and interface type
     * matches.
     */
    HashMap<ClassKey,JsonDeserializer<Object>> _directClassMappings = null;

    /*
    //////////////////////////////////////////////////////////
    // Configuration: mappings that define "mix-in annotations"
    //////////////////////////////////////////////////////////
     */

    /**
     * Mapping that defines how to apply mix-in annotations: key is
     * the type to received additional annotations, and value is the
     * type that has annotations to "mix in".
     *<p>
     * Annotations associated with the value classes will be used to
     * override annotations of the key class, associated with the
     * same field or method. They can be further masked by sub-classes:
     * you can think of it as injecting annotations between the target
     * class and its sub-classes (or interfaces)
     *
     * @since 1.2
     */
    HashMap<ClassKey,Class<?>> _mixInAnnotations;

    /*
    ////////////////////////////////////////////////////
    // Life-cycle, constructors
    ////////////////////////////////////////////////////
     */

    public CustomDeserializerFactory() { super(); }

    /*
    ////////////////////////////////////////////////////
    // Configuration: type-to-serializer mappings
    ////////////////////////////////////////////////////
     */

    /**
     * Method used to add a mapping for specific type -- and only that
     * type -- to use specified deserializer.
     * This means that binding is not used for sub-types.
     *<p>
     * Note that both class and interfaces can be mapped, since the type
     * is derived from method declarations; and hence may be abstract types
     * and interfaces. This is different from custom serialization where
     * only class types can be directly mapped.
     *
     * @param forClass Class to deserialize using specific deserializer.
     * @param deser Deserializer to use for the class. Declared type for
     *   deserializer may be more specific (sub-class) than declared class
     *   to map, since that will still be compatible (deserializer produces
     *   sub-class which is assignable to field/method)
     */
    @SuppressWarnings("unchecked")
    public <T> void addSpecificMapping(Class<T> forClass, JsonDeserializer<? extends T> deser)
    {
        ClassKey key = new ClassKey(forClass);
        if (_directClassMappings == null) {
            _directClassMappings = new HashMap<ClassKey,JsonDeserializer<Object>>();
        }
        _directClassMappings.put(key, (JsonDeserializer<Object>)deser);
    }

    /**
     * Method to use for adding mix-in annotations that Class
     * <code>classWithMixIns</code> contains into class
     * <code>destinationClass</code>. Mixing in is done when introspecting
     * class annotations and properties.
     * Annotations from <code>classWithMixIns</code> (and its supertypes)
     * will <b>override</b>
     * anything <code>destinationClass</code> (and its super-types)
     * has already.
     *
     * @param destinationClass Class to modify by adding annotations
     * @param classWithMixIns Class that contains annotations to add
     *
     * @since 1.2
     */
    public void addMixInAnnotationMapping(Class<?> destinationClass,
                                          Class<?> classWithMixIns)
    {
        if (_mixInAnnotations == null) {
            _mixInAnnotations = new HashMap<ClassKey,Class<?>>();
        }
        _mixInAnnotations.put(new ClassKey(destinationClass), classWithMixIns);
    }


    /*
    ////////////////////////////////////////////////////
    // DeserializerFactory API
    ////////////////////////////////////////////////////
     */

    @Override
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        Class<?> cls = type.getRawClass();
        ClassKey key = new ClassKey(cls);

        // Do we have a match?
        if (_directClassMappings != null) {
            JsonDeserializer<Object> deser = _directClassMappings.get(key);
            if (deser != null) {
                return deser;
            }
        }
        // If not, let super class do its job
        return super.createBeanDeserializer(config, type, p);
    }

    //public JsonDeserializer<?> createArrayDeserializer(DeserializationConfig config, ArrayType type, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createCollectionDeserializer(DeserializationConfig config, CollectionType type, DeserializerProvider p) throws JsonMappingException

    @Override
    public JsonDeserializer<?> createEnumDeserializer(DeserializationConfig config, Class<?> enumClass, DeserializerProvider p) throws JsonMappingException
    {
        /* Enums can't extend anything (or implement); must be a direct
         * match, if anything:
         */
        if (_directClassMappings != null) {
            ClassKey key = new ClassKey(enumClass);
            JsonDeserializer<?> deser = _directClassMappings.get(key);
            if (deser != null) {
                return deser;
            }
        }
        return super.createEnumDeserializer(config, enumClass, p);
    }

    //public JsonDeserializer<?> createMapDeserializer(DeserializationConfig config, MapType type, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config, Class<? extends JsonNode> nodeClass, DeserializerProvider p) throws JsonMappingException
}
