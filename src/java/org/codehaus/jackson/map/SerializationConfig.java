package org.codehaus.jackson.map;

import org.codehaus.jackson.annotate.*;

/**
 * Object that contains baseline configuration for serialization
 * process. An instance is owned by {@link ObjectMapper}, which makes
 * a copy that is passed during serialization process to
 * {@link SerializerProvider} and {@link SerializerFactory}.
 *<p>
 * Note: although configuration settings can be changed at any time
 * (for factories and instances), they are not guaranteed to have
 * effect if called after constructing relevant mapper or serializer
 * instance. This because some objects may be configured, constructed and
 * cached first time they are needed.
 */
public class SerializationConfig
{
    /**
     * Enumeration that defines togglable features that guide
     * the serialization feature.
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
        AUTO_DETECT_GETTERS(true),

            OUTPUT_NULL_PROPERTIES(true)
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

    protected int _featureFlags = DEFAULT_FEATURE_FLAGS;

    /*
    ///////////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////////
     */

    public SerializationConfig()  { }

    private SerializationConfig(int f)  { _featureFlags = f; }

    /**
     * Method that is called to create a non-shared copy of the configuration
     * to be used for a serialization operation.
     */
    public SerializationConfig createUnshared()
    {
    	return new SerializationConfig(_featureFlags);
    }

    /**
     * Method that checks class annotations that the argument Object has,
     * and modifies settings of this configuration object accordingly,
     * similar to how those annotations would affect actual value classes
     * annotated with them, but with global scope. Note that not all
     * annotations have global significance, and thus only subset of
     * Jackson annotations will have any effect.
     *<p>
     * Ones that are known to have effect are:
     *<ul>
     * <li>{@link JsonWriteNullProperties}</li>
     * <li>{@link JsonAutoDetect}</li>
     *</ul>
     * 
     * @param annotatiedClass Class of which class annotations to use
     *   for changing configuration settings
     */
    public void fromAnnotations(Class<?> annotatedClass)
    {
    	JsonWriteNullProperties nullProps = annotatedClass.getAnnotation(JsonWriteNullProperties.class);
    	if (nullProps != null) {
    		set(Feature.OUTPUT_NULL_PROPERTIES, nullProps.value());
    	}
    	JsonAutoDetect autoDetect = annotatedClass.getAnnotation(JsonAutoDetect.class);
    	if (autoDetect != null) {
    		boolean set = false;
    		for (JsonMethod m : autoDetect.value()) {
    			if (m == JsonMethod.GETTER || m == JsonMethod.ALL) {
    				set = true;
    				break;
    			}
    		}
    		set(Feature.AUTO_DETECT_GETTERS, set); 		
    	}
    }
    
    /*
    ///////////////////////////////////////////////////////////
    // Accessors
    ///////////////////////////////////////////////////////////
     */

    /**
     * Method for checking whether given feature is enabled or not
     */
    public final boolean isEnabled(Feature f) {
        return (_featureFlags & f.getMask()) != 0;
    }

    /*
    ////////////////////////////////////////////////////
    // Configuration: on/off features
    ////////////////////////////////////////////////////
     */

    /**
     * Method for enabling specified  features
     * (check
     * {@link org.codehaus.jackson.map.SerializerFactory.Feature}
     * for list of features)
     */
    public void enable(Feature f) {
        _featureFlags |= f.getMask();
    }

    /**
     * Method for disabling specified  features
     * (check
     * {@link org.codehaus.jackson.map.SerializerFactory.Feature}
     * for list of features)
     */
    public void disable(Feature f) {
        _featureFlags &= ~f.getMask();
    }

    public void set(Feature f, boolean state)
    {
        if (state) {
            enable(f);
        } else {
            disable(f);
        }
    }

    //protected int getFeatures() { return _features; }

    /*
    ///////////////////////////////////////////////////////////
    // Debug support
    ///////////////////////////////////////////////////////////
     */

    @Override public String toString()
    {
        return "[SerializationConfig: flags=0x"+Integer.toHexString(_featureFlags)+"]";
    }

    /*
    ///////////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////////
     */
}
