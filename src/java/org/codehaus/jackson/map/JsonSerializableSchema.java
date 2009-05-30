package org.codehaus.jackson.map;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Interface that can be implemented by objects that know how to
 * serialize themselves to Json, using {@link JsonGenerator}
 * (and {@link SerializerProvider} if necessary).
 *<p>
 * Note that implementing this interface binds implementing object
 * closely to Jackson API, and that it is often not necessary to do
 * so -- if class is a bean, it can be serialized without
 * implementing this interface. * @author Ryan Heaton
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonSerializableSchema {

    /**
     * The schema type for this JsonSerializable instance.
     * Possible values: "string", "number", "boolean", "object", "array", "null", "any"
     *
     * @return The schema type for this JsonSerializable instance.
     */
    String schemaType() default "any";

    /**
     * If the schema type is "object", the node that defines the properties of the object.
     *
     * @return The node representing the schema properties, or "##irrelevant" if irrelevant.
     */
    String schemaObjectPropertiesDefinition() default "##irrelevant";

    /**
     * If the schema type if "array", the node that defines the schema for the items in the array.
     *
     * @return The schema for the items in the array, or "##irrelevant" if irrelevant.
     */
    String schemaItemDefinition() default "##irrelevant";
}
