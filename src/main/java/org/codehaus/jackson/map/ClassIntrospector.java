package org.codehaus.jackson.map;

/**
 * Helper class used to introspect features of POJO value classes
 * used with Jackson. The main use is for finding out
 * POJO construction (creator) and value access (getters, setters)
 * methods and annotations that define configuration of using
 * those methods.
 */
public abstract class ClassIntrospector<T extends BeanDescription>
{
	protected ClassIntrospector() { }
	
    /*
    ///////////////////////////////////////////////////////
    // Factory methods
    ///////////////////////////////////////////////////////
     */
    
    /**
     * Factory method that constructs an introspector that has all
     * information needed for serialization purposes.
     */
    public abstract T forSerialization(SerializationConfig cfg, Class<?> c);

    /**
     * Factory method that constructs an introspector that has all
     * information needed for deserialization purposes.
     */
    public abstract T forDeserialization(DeserializationConfig cfg, Class<?> c);

    /**
     * Factory method that constructs an introspector that has
     * information necessary for creating instances of given
     * class ("creator"), as well as class annotations, but
     * no information on member methods
     */
    public abstract T forCreation(DeserializationConfig cfg, Class<?> c);

    /**
     * Factory method that constructs an introspector that only has
     * information regarding annotations class itself has, but nothing
     * on methods or constructors.
     */
    public abstract T forClassAnnotations(Class<?> c);
}

