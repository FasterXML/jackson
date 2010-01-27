package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.type.JavaType;

/**
 * Interface that defines standard API for converting types
 * to type identifiers and vice versa. Used by type resolvers
 * ({@link org.codehaus.jackson.map.TypeSerializer},
 * {@link org.codehaus.jackson.map.TypeDeserializer}) for converting
 * between type and matching id; id is stored in JSON and needed for
 * creating instances of proper subtypes when deserializing values.
 * 
 * @author tatu
 * @since 1.5
 */
public interface TypeIdResolver
{
    /**
     * Method that will be called once before any type resolution calls;
     * used to initialize instance with configuration. This is necessary
     * since instances may be created via reflection, without ability to
     * call specific constructor to pass in configuration settings.
     * 
     * @param baseType Base type for which this id resolver instance is
     *   used
     */
    public void init(JavaType baseType);
    
    /**
     * Method called to serialize type of the type of given value
     * as a String to include in serialized JSON content.
     */
    public String idFromValue(Object value);

    /**
     * Method called to resolve type from given type identifier.
     */
    public JavaType typeFromId(String id);
}
