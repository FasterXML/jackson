package org.codehaus.jackson.map;

/**
 * Abstract class that defines API used by {@link SerializerProvider}
 * to obtain actual
 * {@link JsonSerializer} instances from multiple distinct factories.
 */
public abstract class SerializerFactory
{
    /**
     * Enumeration that defines all togglable features for configurable
     * serializer factories (most notably,
     * {@link org.codehaus.jackson.map.ser.CustomSerializerFactory}).
     */
    public enum Feature {
        /**
         * Feature that determines whether "getter" methods are
         * automatically detected based on standard Bean naming convention
         * or not. If yes, then all public zero-argument methods that
         * start with prefix "get" (or, "is" if return type is boolean)
         * are considered as getters. If disabled, only methods explicitly
         * annotated are considered getters.
         *<p>
         * Note that this feature has lower precedence than per-class
         * annotations, and is only used if there isn't more granular
         * configuration available.
         *<P>
         * Feature is enabled by default.
         */
        AUTO_DETECT_GETTERS(true)
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
    // Basic SerializerFactory API:
    /////////////////////////////////////////////////////////
     */

    /**
     * Method called to create (or, for completely immutable serializers,
     * reuse) a serializer for given type.
     *
     * @param type Type to be serialized
     */
    public abstract <T> JsonSerializer<T> createSerializer(Class<T> type);

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
