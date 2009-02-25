package org.codehaus.jackson.map;

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
 * <li>There is one method to support Json scalar types: here access is
 *   by declared value type
 *  </li>
 * <li>For Json "Array" type, we need 2 methods: one to deal with expected
 *   Java arrays; and the other for other Java containers (Lists, Sets)
 *  </li>
 * <li>For Json "Object" type, we need 2 methods: one to deal with
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
    /**
     * Enumeration that defines all togglable features for configurable
     * serializer factories (most notably,
     * {@link org.codehaus.jackson.map.ser.CustomSerializerFactory}).
     */
    public enum Feature {
        BOGUS(false) // just need a placeholder first
            ;

        final boolean _defaultState;

        /**
         * Method that calculates bit set (flags) of all features that
         * are enabled by default.
         */
        public static int collectDefaults()
        {
            int flags = 0;
            for (Feature f : values()) {
                if (f.enabledByDefault()) {
                    flags |= f.getMask();
                }
            }
            return flags;
        }
        
        private Feature(boolean defaultState) {
            _defaultState = defaultState;
        }
        
        public boolean enabledByDefault() { return _defaultState; }
    
        public int getMask() { return (1 << ordinal()); }
    }

    /**
     * Bitfield (set of flags) of all Features that are enabled
     * by default.
     */
    protected final static int DEFAULT_FEATURE_FLAGS = Feature.collectDefaults();

    /*
    /////////////////////////////////////////////////////////
    // Basic DeserializerFactory API:
    /////////////////////////////////////////////////////////
     */

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert Json content into values of
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
    public abstract JsonDeserializer<Object> createBeanDeserializer(JavaType type, DeserializerProvider p)
        throws JsonMappingException;

    /**
     * Method called to create (or, for completely immutable deserializers,
     * reuse) a deserializer that can convert Json content into values of
     * specified Java type.
     *
     * @param type Type to be deserialized
     * @param p Provider that can be called to create deserializers for
     *   contained member types
     */
    public abstract JsonDeserializer<?> createArrayDeserializer(ArrayType type, DeserializerProvider p)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createCollectionDeserializer(CollectionType type, DeserializerProvider p)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createEnumDeserializer(SimpleType type, DeserializerProvider p)
        throws JsonMappingException;

    public abstract JsonDeserializer<?> createMapDeserializer(MapType type, DeserializerProvider p)
        throws JsonMappingException;

    /**
     * Method for checking whether given feature is enabled or not
     */
    public final boolean isFeatureEnabled(Feature f) {
        return (_getFeatures() & f.getMask()) != 0;
    }

    /*
    /////////////////////////////////////////////////////////
    // Methods for sub-classes to override
    /////////////////////////////////////////////////////////
     */

    /**
     * Default implementation only returns default settings for
     * features: configurable sub-classes need to override this method.
     */
    protected int _getFeatures() { return DEFAULT_FEATURE_FLAGS; }
}
