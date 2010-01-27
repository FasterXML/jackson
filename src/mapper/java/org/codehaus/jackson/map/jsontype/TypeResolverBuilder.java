package org.codehaus.jackson.map.jsontype;

import org.codehaus.jackson.map.TypeDeserializer;
import org.codehaus.jackson.map.TypeSerializer;
import org.codehaus.jackson.map.annotate.JsonTypeInfo;
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
 * {@link #build} will be called to get actual type resolver constructed
 * and used for resolving types for configured base type and its
 * subtypes.
 * 
 * @since 1.5
 * @author tatu
 */
public interface TypeResolverBuilder<T extends TypeResolverBuilder<T>>
{
    /**
     * Marker in annotations to denote that no resolver builder is defined.
     */
    public abstract class NONE implements TypeResolverBuilder<NONE> { }

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
    public TypeSerializer buildTypeSerializer(JavaType baseType);

    /**
     * Method for building type deserializer based on current configuration
     * of this builder.
     * 
     * @param baseType Base type that constructed resolver will
     *    handle; super type of all types it will be used for.
     */
    public TypeDeserializer buildTypeDeserializer(JavaType baseType);
    
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
     * @param (optional) Custom type id resolver used, if any
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

    /**
     * Method for adding relationship between type builder handles, and
     * one of its subtypes. Relationship between type and subtype is only
     * needed for deserialization; but additionally type name to use for type may
     * also be specified, and this names is used for serialization as well.
     * If type name is passed as null, d
     *<p>
     * Calling this method is useful for type id mechanism of
     * {@link JsonTypeInfo.Id#NAME}; other types generally ignore it
     * (although {@link JsonTypeInfo.Id#CUSTOM} can use it).
     *
     * @param type Subtype in question (must be a sub-class of class that
     *   this builder was constructed for
     * @param name (optional) Name to use, if non-null and non-empty;
     *    if null or empty String, default naming strategy is used (which
     *    usually creates name based on class name)
     * 
     * @return Resulting builder instance (usually this builder,
     *   but not necessarily)
     */
    public T registerSubtype(Class<?> type, String name);
}
