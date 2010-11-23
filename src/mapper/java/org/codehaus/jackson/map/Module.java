package org.codehaus.jackson.map;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.Versioned;

/**
 * Simple interface for extensions that can be registered with {@link ObjectMapper}
 * to provide a well-defined set of extensions to default functionality; such as
 * support for new datatypes.
 *
 * @since 1.7
 */
public abstract class Module
    implements Versioned
{
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
        /**
         * Method that returns version information about {@link ObjectMapper} 
         * that implements this context. Modules can use this to choose
         * different settings or initialization order; or even decide to fail
         * set up completely if version is compatible with module.
         */
        public Version getMapperVersion();

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
        public void addDeserializers(Serializers s);
    
    }
}
