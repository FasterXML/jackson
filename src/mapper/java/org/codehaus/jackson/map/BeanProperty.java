package org.codehaus.jackson.map;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.type.JavaType;

/**
 * Bean properties are logical entities that represent data
 * Java objects ("beans", although more accurately POJOs)
 * contain; and that are accessed using some combination
 * of methods (getter, setter), field and constructor
 * parameter.
 *
 * @since 1.7
 */
public interface BeanProperty
{
    /**
     * Method to get logical name of the property
     */
    public String getName();
    
    /**
     * Method to get declared type of the property.
     */
    public JavaType getType();

    /**
     * Method for finding annotation associated with this property;
     * meaning annotation associated with one of entities used to
     * access property.
     */
    public <A extends Annotation> A getAnnotation(Class<A> acls);

    /**
     * Method for finding annotation associated with context of
     * this property; usually class in which member is declared,
     * but for constructor parameters it can contain annotations
     * from constructor first, then from enclosing class.
     */
//    public <A extends Annotation> A getEnclosingAnnotation(Class<A> acls);

    /**
     * Method for accessing primary physical entity that represents the property;
     * annotated field, method or constructor property.
     */
    public AnnotatedMember getMember();
}
