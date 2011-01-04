package org.codehaus.jackson.map;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.deser.BeanDeserializerModifier;
import org.codehaus.jackson.map.type.*;
import org.codehaus.jackson.type.JavaType;

/**
 * Abstract class that defines API used by {@link DeserializerProvider}
 * to obtain actual
 * {@link JsonDeserializer} instances from multiple distinct factories.
 *<p>
 * Since there are multiple broad categories of deserializers, there are 
 * multiple factory methods:
 *<ul>
 * <li>There is one method to support JSON scalar types: here access is
 *   by declared value type
 *  </li>
 * <li>For JSON "Array" type, we need 2 methods: one to deal with expected
 *   Java arrays; and the other for other Java containers (Lists, Sets)
 *  </li>
 * <li>For JSON "Object" type, we need 2 methods: one to deal with
 *   expected Java {@link java.util.Map}s, and another for actual
 *   Java objects (beans)
 *  </li>
 * </ul>
 *<p>
 * All above methods take 2 type arguments, except for the first one
 * which takes just a single argument.
 */
public abstract class DeserializerFactory
{
    protected final static Deserializers[] NO_DESERIALIZERS = new Deserializers[0];

    /*
    /**********************************************************
    /* Helper class to contain configuration settings
    /**********************************************************
     */

    /**
     * Configuration settings container class for bean deserializer factory
     * 
     * @since 1.7
     */
    public abstract static class Config
    {
        /**
         * Fluent/factory method used to construct a configuration object that
         * has same deserializer providers as this instance, plus one specified
         * as argument. Additional provider will be added before existing ones,
         * meaning it has priority over existing definitions.
         */
        public abstract Config withAdditionalDeserializers(Deserializers additional);

        /**
         * Fluent/factory method used to construct a configuration object that
         * has same configuration as this instance plus one additional
         * deserialiazer modifier. Added modified has highest priority (that is, it
         * gets called before any already registered modifier).
         */
        public abstract Config withDeserializerModifier(BeanDeserializerModifier modifier);
        
        public abstract Iterable<Deserializers> deserializers();

        public abstract Iterable<BeanDeserializerModifier> deserializerModifiers();

        public abstract boolean hasDeserializers();

        public abstract boolean hasDeserializerModifiers();
    }

    /*
    /********************************************************
    /* Configuration handling
    /********************************************************
     */

    /**
     * @since 1.7
     */
    public abstract Config getConfig();
    
    /**
     * Method used for creating a new instance of this factory, but with different
     * configuration. Reason for specifying factory method (instead of plain constructor)
     * is to allow proper sub-classing of factories.
     *<p>
     * Note that custom sub-classes <b>must override</b> implementation
     * of this method, as it usually requires instantiating a new instance of
     * factory type. Check out javadocs for
     * {@link org.codehaus.jackson.map.deser.BeanDeserializerFactory} for more details.
     * 
     * @since 1.7
     */
    public abstract DeserializerFactory withConfig(Config config);

    /**
     * Convenience method for creating a new factory instance with additional deserializer
     * provider.
     * 
     * @since 1.7
     */
    public final DeserializerFactory withAdditionalDeserializers(Deserializers additional) {
        return withConfig(getConfig().withAdditionalDeserializers(additional));
    }

    /**
     * Convenience method for creating a new factory instance with additional
     * {@link BeanDeserializerModifier}.
     * 
     * @since 1.7
     */
    public final DeserializerFactory withDeserializerModifier(BeanDeserializerModifier modifier) {
        return withConfig(getConfig().withDeserializerModifier(modifier));
    }
    
    /*
    /**********************************************************
    /* Basic DeserializerFactory API:
    /**********************************************************
     */

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert JSON content into values of
     * specified Java "bean" (POJO) type.
     * At this point it is known that the type is not otherwise recognized
     * as one of structured types (array, Collection, Map) or a well-known
     * JDK type (enum, primitives/wrappers, String); this method only
     * gets called if other options are exhausted. This also means that
     * this method can be overridden to add support for custom types.
     *
     * @param type Type to be deserialized
     * @param p Provider that can be called to create deserializers for
     *   contained member types
     */
    public abstract JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, DeserializerProvider p,
            JavaType type, BeanProperty property)
        throws JsonMappingException;

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert JSON content into values of
     * specified Java type.
     *
     * @param type Type to be deserialized
     * @param p Provider that can be called to create deserializers for
     *   contained member types
     */
    public abstract JsonDeserializer<?> createArrayDeserializer(DeserializationConfig config, DeserializerProvider p,
            ArrayType type, BeanProperty property)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createCollectionDeserializer(DeserializationConfig config, DeserializerProvider p,
            CollectionType type, BeanProperty property)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createEnumDeserializer(DeserializationConfig config,DeserializerProvider p,
            JavaType type, BeanProperty property)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createMapDeserializer(DeserializationConfig config, DeserializerProvider p,
            MapType type, BeanProperty property)
        throws JsonMappingException;

    /**
     * Method called to create and return a deserializer that can construct
     * JsonNode(s) from JSON content.
     */
    public abstract JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config, DeserializerProvider p,
            JavaType type, BeanProperty property)
        throws JsonMappingException;

    /**
     * Method called to find and create a type information deserializer for given base type,
     * if one is needed. If not needed (no polymorphic handling configured for type),
     * should return null.
     *<p>
     * Note that this method is usually only directly called for values of container (Collection,
     * array, Map) types and root values, but not for bean property values.
     *
     * @param baseType Declared base type of the value to deserializer (actual
     *    deserializer type will be this type or its subtype)
     * 
     * @return Type deserializer to use for given base type, if one is needed; null if not.
     * 
     * @since 1.5
     */
    public TypeDeserializer findTypeDeserializer(DeserializationConfig config, JavaType baseType,
            BeanProperty property)
    {
        // Default implementation returns null for backwards compatibility reasons
        return null;
    }

    /*
    /********************************************************
    /* Older deprecated versions of creator methods
    /********************************************************
     */
    
    /**
     *<p>
     * Note: declared final to prevent sub-classes from overriding; choice is between
     * hard compile-time error and nastier runtime errors as this method should
     * not be called by core framework any more.
     * 
     * @deprecated Since 1.7 should use method that takes in property definition
     */
    @Deprecated
    final
    public TypeDeserializer findTypeDeserializer(DeserializationConfig config, JavaType baseType)
    {
        return findTypeDeserializer(config, baseType, null);
    }

    /**
     *<p>
     * Note: declared final to prevent sub-classes from overriding; choice is between
     * hard compile-time error and nastier runtime errors as this method should
     * not be called by core framework any more.
     * @deprecated Since 1.7 should use method that takes in property definition
     */
    @Deprecated
    final
    public JsonDeserializer<Object> createBeanDeserializer(DeserializationConfig config, JavaType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return createBeanDeserializer(config, p, type, null);
    }

    /**
     *<p>
     * Note: declared final to prevent sub-classes from overriding; choice is between
     * hard compile-time error and nastier runtime errors as this method should
     * not be called by core framework any more.
     * 
     * @deprecated Since 1.7 should use method that takes in property definition
     */
    @Deprecated
    final
    public JsonDeserializer<?> createArrayDeserializer(DeserializationConfig config, ArrayType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return createArrayDeserializer(config, p, type, null);
    }
    
    /**
     *<p>
     * Note: declared final to prevent sub-classes from overriding; choice is between
     * hard compile-time error and nastier runtime errors as this method should
     * not be called by core framework any more.
     * 
     * @deprecated Since 1.7 should use method that takes in property definition
     */
    @Deprecated
    final
    public JsonDeserializer<?> createCollectionDeserializer(DeserializationConfig config, CollectionType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return createCollectionDeserializer(config, p, type, null);
    }
    
    /**
     *<p>
     * Note: declared final to prevent sub-classes from overriding; choice is between
     * hard compile-time error and nastier runtime errors as this method should
     * not be called by core framework any more.
     * 
     * @deprecated Since 1.7 should use method that takes in property definition
     */
    @Deprecated
    final
    public JsonDeserializer<?> createEnumDeserializer(DeserializationConfig config, Class<?> enumClass, DeserializerProvider p)
        throws JsonMappingException
    {
        return createEnumDeserializer(config, p, TypeFactory.type(enumClass), null);
    }
    
    /**
     *<p>
     * Note: declared final to prevent sub-classes from overriding; choice is between
     * hard compile-time error and nastier runtime errors as this method should
     * not be called by core framework any more.
     * 
     * @deprecated Since 1.7 should use method that takes in property definition
     */
    @Deprecated
    final
    public JsonDeserializer<?> createMapDeserializer(DeserializationConfig config, MapType type, DeserializerProvider p)
        throws JsonMappingException
    {
        return createMapDeserializer(config, p, type, null);
    }

    /**
     *<p>
     * Note: declared final to prevent sub-classes from overriding; choice is between
     * hard compile-time error and nastier runtime errors as this method should
     * not be called by core framework any more.
     * 
     * @deprecated Since 1.7 should use method that takes in property definition
     */
    @Deprecated
    final
    public JsonDeserializer<?> createTreeDeserializer(DeserializationConfig config, Class<? extends JsonNode> nodeClass, DeserializerProvider p)
        throws JsonMappingException
    {
        return createTreeDeserializer(config, p, TypeFactory.type(nodeClass), null);
    }
}
