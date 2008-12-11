package org.codehaus.jackson.map.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Method;

/**
 * Annotation that can be used to define factory method to use
 * for instantiating a propertu value type.
 *
 * @see org.codehaus.jackson.map.annotation.JsonType
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonTypeFactory
{
    /**
     * Name of static method to call to construct the property value type
     * instance. Simple name (no type decorations, i.e. not a
     * method signature serialization) is to be used; if multiple
     * variants exist, will try to match with the received Json
     * type (String, int, boolean etc). Method has to take exactly
     * one argument.
     */
    public String value();

    /**
     * Class that contains the factory method; if omitted, Class
     * of the type itself is assumed (which comes from property type
     * or {@link JsonType} annotation).
     */
    public Class factoryClass();
}
