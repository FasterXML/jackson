package org.codehaus.jackson.map;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.Versioned;
import org.codehaus.jackson.map.deser.BeanDeserializerModifier;
import org.codehaus.jackson.map.ser.BeanSerializerModifier;

/**
 * Simple interface for extensions that can be registered with {@link ObjectMapper}
 * to provide a well-defined set of extensions to default functionality; such as
 * support for new data types.
 *
 * @since 1.7
 */
public abstract class Module
    implements Versioned
{
    /*
    /**********************************************************
    /* Simple accessors
    /**********************************************************
     */
    
    /**
     * Method that returns identifier for module; this can be used by Jackson
     * for informational purposes, as well as in associating extensions with
     * module that provides them.
     */
    public abstract String getModuleName();

    /**
     * Method that returns version of this module. Can be used by Jackson for
     * informational purposes.
     */
    public abstract Version version();

    /*
    /**********************************************************
    /* Life-cycle: registration
    /**********************************************************
     */
    
    /**
     * Method called by {@link ObjectMapper} when module is registered.
     * It is called to let module register functionality it provides,
     * using callback methods passed-in context object exposes.
     */
    public abstract void setupModule(SetupContext context);
    
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    /**
     * Interface Jackson exposes to modules for purpose of registering
     * extended functionality.
     */
    public interface SetupContext
    {
        /*
        /**********************************************************
        /* Simple accessors
        /**********************************************************
         */
        
        /**
         * Method that returns version information about {@link ObjectMapper} 
         * that implements this context. Modules can use this to choose
         * different settings or initialization order; or even decide to fail
         * set up completely if version is compatible with module.
         */
        public Version getMapperVersion();

        /**
         * Method that returns current deserialization configuration
         * settings. Since modules may be interested in these settings,
         * caller should make sure to make changes to settings before
         * module registrations.
         */
        public DeserializationConfig getDeserializationConfig();

        /**
         * Method that returns current serialization configuration
         * settings. Since modules may be interested in these settings,
         * caller should make sure to make changes to settings before
         * module registrations.
         */
        public SerializationConfig getSeserializationConfig();
        
        /*
        /**********************************************************
        /* Handler registration
        /**********************************************************
         */
        
        /**
         * Method that module can use to register additional deserializers to use for
         * handling types.
         * 
         * @param d Object that can be called to find deserializer for types supported
         *   by module (null returned for non-supported types)
         */
        public void addDeserializers(Deserializers d);
        
        /**
         * Method that module can use to register additional serializers to use for
         * handling types.
         * 
         * @param s Object that can be called to find serializer for types supported
         *   by module (null returned for non-supported types)
         */
        public void addSerializers(Serializers s);

        /**
         * Method that module can use to register additional modifier objects to
         * customize configuration and construction of bean deserializers.
         * 
         * @param mod Modifier to register
         */
        public void addBeanDeserializerModifier(BeanDeserializerModifier mod);

        /**
         * Method that module can use to register additional modifier objects to
         * customize configuration and construction of bean serializers.
         * 
         * @param mod Modifier to register
         */
        public void addBeanSerializerModifier(BeanSerializerModifier mod);
        
        /**
         * Method for registering specified {@link AnnotationIntrospector} as the highest
         * priority introspector (will be chained with existing introspector(s) which
         * will be used as fallbacks for cases this introspector does not handle)
         * 
         * @param ai Annotation introspector to register.
         */
        public void insertAnnotationIntrospector(AnnotationIntrospector ai);

        /**
         * Method for registering specified {@link AnnotationIntrospector} as the lowest
         * priority introspector, chained with existing introspector(s) and called
         * as fallback for cases not otherwise handled.
         * 
         * @param ai Annotation introspector to register.
         */
        public void appendAnnotationIntrospector(AnnotationIntrospector ai);

        /**
         * Method used for defining mix-in annotations to use for augmenting
         * specified class or interface.
         * All annotations from
         * <code>mixinSource</code> are taken to override annotations
         * that <code>target</code> (or its supertypes) has.
         *<p>
         * Note: mix-ins are registered both for serialization and deserialization
         * (which can be different internally).
         *<p>
         * Note: currently only one set of mix-in annotations can be defined for
         * a single class; so if multiple modules register mix-ins, highest
         * priority one (last one registered) will have priority over other modules.
         *
         * @param target Class (or interface) whose annotations to effectively override
         * @param mixinSource Class (or interface) whose annotations are to
         *   be "added" to target's annotations, overriding as necessary
         */
        public void setMixInAnnotations(Class<?> target, Class<?> mixinSource);
    }
}
