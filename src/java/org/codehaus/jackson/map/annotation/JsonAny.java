package org.codehaus.jackson.map.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import java.lang.reflect.Method;

/**
 * Marker annotation that can be used to define a specific property 
 * to be a "fallback" container of all unknown entries when
 * deserializing Java objects from Json content.
 * It is similar to {@link javax.xml.bind.annotation.XmlAny}
 * in behavior; and can only be used to denote a single property
 * per type. Also, type itself has to be either a {@link java.util.Map}
 * or a Bean type.
 *<p>
 * If used, all otherwise unmapped key-value pairs from Json Object
 * structs are added to the property (of type Map or bean).
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonAny
{
}
