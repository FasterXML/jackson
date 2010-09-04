package org.codehaus.jackson.map;

import org.codehaus.jackson.type.JavaType;

/**
 * Helper class used to introspect features of POJO value classes
 * used with Jackson. The main use is for finding out
 * POJO construction (creator) and value access (getters, setters)
 * methods and annotations that define configuration of using
 * those methods.
 */
public abstract class ClassIntrospector<T extends BeanDescription>
{
    /*
    ///////////////////////////////////////////////////////
    // Helper interfaces
    ///////////////////////////////////////////////////////
     */

    /**
     * Interface used for decoupling details of how mix-in annotation
     * definitions are accessed (via this interface), and how
     * they are stored (defined by classes that implement the interface)
     */
    public interface MixInResolver
    {
        /**
         * Method that will check if there are "mix-in" classes (with mix-in
         * annotations) for given class
         */
        public Class<?> findMixInClassFor(Class<?> cls);
    }

    protected ClassIntrospector() { }
	
    /*
    ///////////////////////////////////////////////////////
    // Public API: factory methods
    ///////////////////////////////////////////////////////
     */
    
    /**
     * Factory method that constructs an introspector that has all
     * information needed for serialization purposes.
     */
    public abstract T forSerialization(SerializationConfig cfg, JavaType type,
                                       MixInResolver r);

    /**
     * Factory method that constructs an introspector that has all
     * information needed for deserialization purposes.
     */
    public abstract T forDeserialization(DeserializationConfig cfg, JavaType type,
                                         MixInResolver r);
    
    /**
     * Factory method that constructs an introspector that has
     * information necessary for creating instances of given
     * class ("creator"), as well as class annotations, but
     * no information on member methods
     */
    public abstract T forCreation(DeserializationConfig cfg, JavaType type,
                                  MixInResolver r);

    /**
     * Factory method that constructs an introspector that only has
     * information regarding annotations class itself (or its supertypes) has,
     * but nothing on methods or constructors.
     */
    public abstract T forClassAnnotations(MapperConfig<?> cfg, Class<?> c,
                                          MixInResolver r);

    /**
     * Factory method that constructs an introspector that only has
     * information regarding annotations class itself has (but NOT including
     * its supertypes), but nothing on methods or constructors.
     * 
     * @since 1.5
     */
    public abstract T forDirectClassAnnotations(MapperConfig<?> cfg, Class<?> c,
            MixInResolver r);

}

