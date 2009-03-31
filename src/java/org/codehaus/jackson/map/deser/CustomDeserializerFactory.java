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
 *   classes and interfaces and deserializers to use for deserializing
 *   instance of these classes.. These can be either specific ones
 *   (class/interface and declaration must match exactly)
 *   or generic ones (any sub-class or class implementing the interface);
 *   specific ones have precedence over generic ones (and
 *    precedence between generic ones is not defined).
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
     * Direct mappings that are only used for exact class type
     * matches, but not for sub-class checks.
     */
    HashMap<ClassKey,JsonDeserializer<Object>> _directClassMappings = null;

    /*
    ////////////////////////////////////////////////////
    // Configuration, generic (interface, super-class) mappings
    ////////////////////////////////////////////////////
     */

    /**
     * And then class-based mappings that are used both for exact and
     * sub-class matches.
     */
    HashMap<ClassKey,JsonDeserializer<Object>> _transitiveClassMappings = null;

    /**
     * And finally interface-based matches.
     */
    HashMap<ClassKey,JsonDeserializer<Object>> _interfaceMappings = null;

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
     * !!! 30-Mar-2009, tatu: Not used as of yet
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
     * Method used to add a generic (transitive) mapping from specified
     * class or its sub-classes into a deserializer.
     * When resolving a type into a deserializer, explicit class is checked
     * first, then immediate super-class, and so forth along inheritance
     * chain. But if this fails, implemented interfaces are checked;
     * ordering is done such that first interfaces implemented by
     * the exact type are checked (in order returned by
     * {@link Class#getInterfaces}), then super-type's and so forth.
     *<p>
     * Note that adding generic mappings may lead to problems with
     * sub-classing: if sub-classes add new properties, these may not
     * get properly deserialized.
     */
    @SuppressWarnings("unchecked")
    public <T> void addGenericMapping(Class<T> type, JsonDeserializer<T> deser)
    {
        // Interface to match?
        ClassKey key = new ClassKey(type);
        if (type.isInterface()) {
            if (_interfaceMappings == null) {
                _interfaceMappings = new HashMap<ClassKey,JsonDeserializer<Object>>();
            }
            _interfaceMappings.put(key, (JsonDeserializer<Object>)deser);
        } else { // nope, class:
            if (_transitiveClassMappings == null) {
                _transitiveClassMappings = new HashMap<ClassKey,JsonDeserializer<Object>>();
            }
            _transitiveClassMappings.put(key, (JsonDeserializer<Object>)deser);
        }
    }

    /**
     * Method used to add a mapping for specific type -- and only that
     * type -- to use specified deserializer.
     * This means that binding is not used for sub-types.
     *<p>
     * Note that whereas abstract classes and interface can not be used
     * with specific (direct) mappings for serialization, it is fine to
     * use them for deserialization. This because declared type can be
     * an abstract type or interface
     */
    @SuppressWarnings("unchecked")
    public <T> void addSpecificMapping(Class<T> forClass, JsonDeserializer<T> deser)
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
     * @param destinationClass Type to modify by adding annotations
     * @param classWithMixIns Type that contains annotations to add
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

    public JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        Class<?> cls = type.getRawClass();
        JsonDeserializer<Object> deser = null;
        ClassKey key = new ClassKey(cls);

        // First: exact matches
        if (_directClassMappings != null) {
            deser = _directClassMappings.get(key);
            if (deser != null) {
                return deser;
            }
        }

        // Still no match? How about more generic ones?
        // Mappings for super-classes?
        if (_transitiveClassMappings != null) {
            for (Class<?> curr = cls; (curr != null); curr = curr.getSuperclass()) {
                key.reset(curr);
                deser = _transitiveClassMappings.get(key);
                if (deser != null) {
                    return deser;
                }
            }
        }

        // And if still no match, how about interfaces?
        if (_interfaceMappings != null) {
            for (Class<?> curr = cls; (curr != null); curr = curr.getSuperclass()) {
                for (Class<?> iface : curr.getInterfaces()) {
                    key.reset(iface);
                    deser = _interfaceMappings.get(key);
                    if (deser != null) {
                        return deser;
                    }
                }
            }
        }
        /* And barring any other complications, let's just let
         * bean (or basic) serializer factory handle construction.
         */
        return super.createBeanDeserializer(type, p);
    }

    //public JsonDeserializer<?> createArrayDeserializer(ArrayType type, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createCollectionDeserializer(CollectionType type, DeserializerProvider p) throws JsonMappingException

    public JsonDeserializer<?> createEnumDeserializer(Class<?> enumClass, DeserializerProvider p) throws JsonMappingException
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
        return super.createEnumDeserializer(enumClass, p);
    }

    //public JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p) throws JsonMappingException

    //public JsonDeserializer<?> createTreeDeserializer(Class<? extends JsonNode> nodeClass, DeserializerProvider p) throws JsonMappingException
}
