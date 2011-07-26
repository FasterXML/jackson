package org.codehaus.jackson.map;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.introspect.AnnotatedMember;
import org.codehaus.jackson.map.util.Annotations;
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
     * this property; usually class in which member is declared
     * (or its subtype if processing subtype).
     */
    public <A extends Annotation> A getContextAnnotation(Class<A> acls);

    /**
     * Method for accessing primary physical entity that represents the property;
     * annotated field, method or constructor property.
     */
    public AnnotatedMember getMember();
    
    /*
    /**********************************************************
    /* Simple stand-alone implementation, useful as a placeholder
    /**********************************************************
     */

    public static class Std implements BeanProperty
    {
        protected final String _name;
        protected final JavaType _type;

        /**
         * Physical entity (field, method or constructor argument) that
         * is used to access value of property (or in case of constructor
         * property, just placeholder)
         */
        protected final AnnotatedMember _member;

        /**
         * Annotations defined in the context class (if any); may be null
         * if no annotations were found
         */
        protected final Annotations _contextAnnotations;
        
        public Std(String name, JavaType type, Annotations contextAnnotations, AnnotatedMember member)
        {
            _name = name;
            _type = type;
            _member = member;
            _contextAnnotations = contextAnnotations;
        }

        public Std withType(JavaType type) {
            return new Std(_name, type, _contextAnnotations, _member);
        }
        
        public <A extends Annotation> A getAnnotation(Class<A> acls) {
            return _member.getAnnotation(acls);
        }

        public <A extends Annotation> A getContextAnnotation(Class<A> acls) {
            return (_contextAnnotations == null) ? null : _contextAnnotations.get(acls);
        }
        
        public String getName() {
            return _name;
        }

        public JavaType getType() {
            return _type;
        }

        public AnnotatedMember getMember() {
            return _member;
        }
    }
}
