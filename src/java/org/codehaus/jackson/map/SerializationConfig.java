package org.codehaus.jackson.map;

import java.text.DateFormat;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.util.DateFormatHolder;

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
        // // // Features for annotation handling

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

        // // // Generic output features

            /**
             * Feature that determines the default settings of whether Bean
             * properties with null values are to be written out.
             *<P>
             * Feature is enabled by default (null properties written).
             */
            WRITE_NULL_PROPERTIES(true),

        // // // Features for datatype-specific serialization

            /**
             * Feature that determines whether {@link java.util.Date}s
             * (and Date-based things like {@link java.util.Calendar}s) are to be
             * serialized as numeric timestamps (true; the default),
             * or as textual representation (false).
             * If textual representation is used, the actual format is
             * one returned by a call to {@link #getDateFormat}.
             */
            WRITE_DATES_AS_TIMESTAMPS(true)

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
    ///////////////////////////////////////////////////////////
    // Configuration settings
    ///////////////////////////////////////////////////////////
     */

    protected int _featureFlags = DEFAULT_FEATURE_FLAGS;

    /**
     * Textual data format to use for serialization (if enabled by
     * {@link Feature#WRITE_DATES_AS_TIMESTAMPS} being set to false).
     * Defaults to a ISO-8601 compliant format accessed from
     * {@link DateFormatHolder}.
     * Note that format object is <b>not to be used as is</b> by caller:
     * since date format objects are not thread-safe, caller has to
     * create a clone first.
     */
    protected DateFormat _dateFormat = DateFormatHolder.getBlueprintISO8601Format();

    /*
    ///////////////////////////////////////////////////////////
    // Life-cycle
    ///////////////////////////////////////////////////////////
     */

    public SerializationConfig()  { }

    protected SerializationConfig(SerializationConfig src)
    {
        _featureFlags = src._featureFlags;
        _dateFormat = src._dateFormat;
    }

    /**
     * Method that is called to create a non-shared copy of the configuration
     * to be used for a serialization operation.
     * Note that if sub-classing
     * and sub-class has additional instance methods,
     * this method <b>must</b> be overridden to produce proper sub-class
     * instance.
     */
    public SerializationConfig createUnshared()
    {
    	return new SerializationConfig(this);
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
     * @param annotatedClass Class of which class annotations to use
     *   for changing configuration settings
     */
    public void fromAnnotations(Class<?> annotatedClass)
    {
    	JsonWriteNullProperties nullProps = annotatedClass.getAnnotation(JsonWriteNullProperties.class);
    	if (nullProps != null) {
            set(Feature.WRITE_NULL_PROPERTIES, nullProps.value());
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

    public DateFormat getDateFormat() { return _dateFormat; }

    /*
    ////////////////////////////////////////////////////
    // Configuration: on/off features
    ////////////////////////////////////////////////////
     */

    /**
     * Method for enabling specified feature.
     */
    public void enable(Feature f) {
        _featureFlags |= f.getMask();
    }

    /**
     * Method for disabling specified feature.
     */
    public void disable(Feature f) {
        _featureFlags &= ~f.getMask();
    }

    /**
     * Method for enabling or disabling specified feature.
     */
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
    ////////////////////////////////////////////////////
    // Configuration: other
    ////////////////////////////////////////////////////
     */

    /**
     * Method that will set the textual serialization to use for
     * serializing Dates (and Calendars); or if null passed, simply
     * disable textual serialization and use timestamp.
     * Also, will enable/disable feature
     * {@link Feature#WRITE_DATES_AS_TIMESTAMPS}: enable, if argument
     * is null; disable if non-null.
     */
    public void setDateFormat(DateFormat df) {
        _dateFormat = df;
        // Also: enable/disable usage of 
        set(Feature.WRITE_DATES_AS_TIMESTAMPS, (df == null));
    }

    /*
    ///////////////////////////////////////////////////////////
    // Debug support
    ///////////////////////////////////////////////////////////
     */

    @Override public String toString()
    {
        return "[SerializationConfig: flags=0x"+Integer.toHexString(_featureFlags)+"]";
    }
}
