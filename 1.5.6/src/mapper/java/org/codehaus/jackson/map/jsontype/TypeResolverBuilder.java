package org.codehaus.jackson.map.jsontype;

import java.util.Collection;

import org.codehaus.jackson.annotate.JsonTypeInfo;
import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.type.JavaType;

/**
 * Interface that defines builders that are configured based on
 * annotations (like {@link JsonTypeInfo} or JAXB annotations),
 * and produce type serializers and deserializers used for
 * handling type information embedded in JSON to allow for safe
 * polymorphic type handling.
 *<p>
 * Builder is first initialized by calling {@link #init} method, and then
 * configured using <code>setXxx</code> (and <code>registerXxx</code>)
 * methods. Finally, after calling all configuration methods,
 * {@link #buildTypeSerializer} or {@link #buildTypeDeserializer}
 * will be called to get actual type resolver constructed
 * and used for resolving types for configured base type and its
 * subtypes.
 * 
 * @since 1.5
 * @author tatu
 */
public interface TypeResolverBuilder<T extends TypeResolverBuilder<T>>
{
    /*
     ***********************************************************
     * Actual builder methods
     ***********************************************************
      */

    /**
     * Method for building type serializer based on current configuration
     * of this builder.
     * 
     * @param baseType Base type that constructed resolver will
     *    handle; super type of all types it will be used for.
     */
    public TypeSerializer buildTypeSerializer(JavaType baseType,
            Collection<NamedType> subtypes);

    /**
     * Method for building type deserializer based on current configuration
     * of this builder.
     * 
     * @param baseType Base type that constructed resolver will
     *    handle; super type of all types it will be used for.
     * @param subtypes Known subtypes of the base type.
     */
    public TypeDeserializer buildTypeDeserializer(JavaType baseType,
            Collection<NamedType> subtypes);
    
    /*
     ***********************************************************
     * Initialization method(s) that must be called before other
     * configuration
     ***********************************************************
      */

    /**
     * Initialization method that is called right after constructing
     * the builder instance.
     *
     * @param idType Which type metadata is used
     * @param res (optional) Custom type id resolver used, if any
     * 
     * @return Resulting builder instance (usually this builder,
     *   but not necessarily)
     */
    public T init(JsonTypeInfo.Id idType, TypeIdResolver res);
    
    /*
    ***********************************************************
    * Methods for configuring resolver to build 
    ***********************************************************
     */
    
    /**
     * Method for specifying mechanism to use for including type metadata
     * in JSON.
     * If not explicitly called, setting defaults to
     * {@link JsonTypeInfo.As#PROPERTY}.
     * 
     * @param includeAs Mechanism used for including type metadata in JSON
     * 
     * @return Resulting builder instance (usually this builder,
     *   but not necessarily)
     */
    public T inclusion(JsonTypeInfo.As includeAs);

    /**
     * Method for specifying name of property used for including type
     * information. Not used for all inclusiong mechanisms;
     * usually only used with {@link JsonTypeInfo.As#PROPERTY}.
     *<p>
     * If not explicitly called, name of property to use is based on
     * defaults for {@JsonTypeInfo.id} configured.
     * 
     * @param propName Name of JSON property to use for including
     *    type information
     * 
     * @return Resulting builder instance (usually this builder,
     *   but not necessarily)
     */
    public T typeProperty(String propName);
}
