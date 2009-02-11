package org.codehaus.jackson.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation that can be used to explicitly mark
 * {@link org.codehaus.jackson.map.JsonDeserializer} used to deserialize
 * instances of the class annotated, or the value of property
 * that is modifier using (setter) method annotated.
 *<p>
 * Note that although type deserializers do have generic type information,
 * that information is not available during processing. As a result,
 * only thing that can be checked during annotation processing
 * is that class used does indeed implement
 * {@link org.codehaus.jackson.map.JsonDeserializer}; but not whether
 * it declares that it can handle type given.
 *<p>
 * Note also that this method does NOT imply that the associated
 * method (when applied to one) is implicitly a setter:
 * rather, it must be recognized as one either due to its naming,
 * or by associated {@link JsonSetter} annotation.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonUseDeserializer
{
    /**
     * Class to instantiate to get the serializer instance used for
     * serializing associated value. Depending on what is annotated,
     * value is either an instance of annotated class (used globablly
     * anywhere where class serializer is needed); or only used for
     * serializing property access via a getter method.
     */
    public Class<?> value();
}
