package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used for configuring details of if and how type information is
 * used with JSON serialization and deserialization, to preserve information
 * about actual class of Object instances. This is necessarily for polymorphic
 * types, and may also be needed to link abstract declared types and matching
 * concrete implementation.
 *<p>
 * Some examples of typical annotations:
 *<pre>
 *  // Include Java class name ("com.myempl.ImplClass") as JSON property "class"
 *  \@JsonTypeInfo(use=Id.CLASS, include=As.PROPERTY, property="class")
 *  
 *  // Include logical type name (defined in impl classes) as wrapper; 2 annotations
 *  \@JsonTypeInfo(use=Id.NAME, include=As.WRAPPER_OBJECT)
 *  \@JsonSubTypes({com.myemp.Impl1.class, com.myempl.Impl2.class})
 *</pre>
 * Alternatively you can also define fully customized type handling by using
 * {@link org.codehaus.jackson.map.annotate.JsonTypeResolver} annotation.
 * 
 * @see org.codehaus.jackson.map.annotate.JsonTypeResolver
 * @since 1.5
 * 
 * @author tatu
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotation
public @interface JsonTypeInfo
{    
    /*
     *******************************************************************
     * Value enumerations used for properties
     *******************************************************************
     */
    
    /**
     * Definition of different type identifiers that can be included in JSON
     * during serialization, and used for deserialization.
     */
    public enum Id {
        /**
         * This means that no explicit type metadata is included, and typing is
         * purely done using contextual information possibly augmented with other
         * annotations.
         *<p>
         * Note: no {@link org.codehaus.jackson.map.jsontype.TypeIdResolver}
         * is constructed if this value is used.
         */
        NONE(null),

        /**
         * Means that fully-qualified Java class name is used as the type identifier.
         */
        CLASS("@class"),

        /**
         * Means that Java class name with minimal path is used as the type identifier.
         * Minimal means that only class name, and that part of preceding Java
         * package name is included that is needed to construct fully-qualified name
         * given fully-qualified name of the declared supertype.
         * For example, for supertype "com.foobar.Base", and concrete type
         * "com.foo.Impl", only "Impl" would be included; and for "com.foo.impl.Impl2"
         * only "impl.Impl2" would be included.
         *<p>
         * If all related classes are in the same Java package, this option can reduce
         * amount of type information overhead, especially for small types.
         * However, please note that using this alternative is inherently risky since it
         * assumes that the
         * supertype can be reliably detected. Given that it is based on declared type
         * (since ultimate supertype, <code>java.lang.Object</code> would not be very
         * useful reference point), this may not always work as expected.
         */
        MINIMAL_CLASS("@c"),

        /**
         * Means that logical type name is used as type information; name will then need
         * to be separately resolved to actual concrete type (Class).
         */
        NAME("@type"),

        /**
         * Means that typing mechanism uses customized handling, with possibly
         * custom configuration. This means that semantics of other properties is
         * not defined by Jackson package, but by the custom implementation.
         */
        CUSTOM(null)
        ;

        private final String _defaultPropertyName;

        private Id(String defProp) {
            _defaultPropertyName = defProp;
        }

        public String getDefaultPropertyName() { return _defaultPropertyName; }
    }

    /**
     * Definition of standard type inclusion mechanisms for type metadata.
     * Used for standard metadata types, except for {@link Id#NONE}.
     * May or may not be used for custom types ({@link Id#CUSTOM}).
     */
    public enum As {
        /**
         * Inclusion mechanism that uses a single configurable property, included
         * along with actual data (POJO properties) as a separate meta-property.
         * <p>
         * Default choice for inclusion.
         */
        PROPERTY,

        /**
         * Inclusion mechanism that wraps typed JSON value (POJO
         * serialized as JSON) in
         * a JSON Object that has a single entry,
         * where field name is serialized type identifier,
         * and value is the actual JSON value.
         *<p>
         * Note: can only be used if type information can be serialized as
         * String. This is true for standard type metadata types, but not
         * necessarily for custom types.
         */
        WRAPPER_OBJECT,

        /**
         * Inclusion mechanism that wraps typed JSON value (POJO
         * serialized as JSON) in
         * a 2-element JSON array: first element is the serialized
         * type identifier, and second element the serialized POJO
         * as JSON Object.
         */
        WRAPPER_ARRAY,
        ;
    }
    
    /*
     *******************************************************************
     * Annotation properties
     *******************************************************************
     */
    
    /**
     * What kind of type metadata is to be used for serializing and deserializing
     * type information for instances of annotated type (and its subtypes
     * unless overridden)
     */
    public Id use();    
    
    /**
     * What mechanism is used for including type metadata (if any; for
     * {@link Id#NONE} nothing is included). Default
     *<p>
     * Note that for type metadata type of {@link Id#CUSTOM},
     * this setting may or may not have any effect.
     */
    public As include() default As.PROPERTY;

    /**
     * Property names used when type inclusion method ({@link As#PROPERTY}) is used
     * (or possibly when using type metadata of type {@link Id#CUSTOM}).
     *<p>
     * Default property name used if this property is not explicitly defined
     * (or is set to empty String) is based on
     * type metadata type ({@link #use}) used.
     */
    public String property() default "";
}
