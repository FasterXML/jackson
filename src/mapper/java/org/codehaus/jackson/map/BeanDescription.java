package org.codehaus.jackson.map;

import java.util.*;

import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;
import org.codehaus.jackson.map.introspect.VisibilityChecker;
import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.util.Annotations;
import org.codehaus.jackson.type.JavaType;

/**
 * Basic container for information gathered by {@link ClassIntrospector} to
 * help in constructing serializers and deserializers.
 * Note that the main implementation type is
 * {@link org.codehaus.jackson.map.introspect.BasicBeanDescription},
 * meaning that it is safe to upcast to this type.
 * 
 * @author tatu
 */
public abstract class BeanDescription
{
    /*
    /**********************************************************
    /* Configuration
    /**********************************************************
     */

    /**
     * Bean type information, including raw class and possible
     * * generics information
     */
    protected final JavaType _type;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected BeanDescription(JavaType type)
    {
    	_type = type;
    }

    /*
    /**********************************************************
    /* Simple accesors
    /**********************************************************
     */

    /**
     * Method for accessing declared type of bean being introspected,
     * including full generic type information (from declaration)
     */
    public JavaType getType() { return _type; }

    public Class<?> getBeanClass() { return _type.getRawClass(); }

    public abstract boolean hasKnownClassAnnotations();


    /**
     * Accessor for type bindings that may be needed to fully resolve
     * types of member object, such as return and argument types of
     * methods and constructors, and types of fields.
     */
    public abstract TypeBindings bindingsForBeanType();

    /**
     * Method for resolving given JDK type, using this bean as the
     * generic type resolution context.
     * 
     * @since 1.9
     */
    public abstract JavaType resolveType(java.lang.reflect.Type jdkType);
    
    /**
     * Method for accessing collection of annotations the bean
     * class has.
     * 
     * @since 1.7
     */
    public abstract Annotations getClassAnnotations();
    
    /*
    /**********************************************************
    /* Basic API
    /**********************************************************
     */
    
    /**
     * @param ignoredProperties (optional, may be null) Names of properties
     *   to ignore; getters for these properties are not to be returned.
     *   
     * @return Ordered Map with logical property name as key, and
     *    matching getter method as value.
     *    
     * @since 1.9
     */
    public abstract Map<String,AnnotatedMethod> findGetters(Collection<String> ignoredProperties);
    
    /**
     * @return Ordered Map with logical property name as key, and
     *    matching setter method as value.
     */
    public abstract Map<String,AnnotatedMethod> findSetters();

    /**
     * @since 1.9
     */
    public abstract Map<String,AnnotatedField> findDeserializableFields(Collection<String> ignoredProperties);

    /**
     * @since 1.9
     */
    public abstract Map<String,AnnotatedField> findSerializableFields(Collection<String> ignoredProperties);
    
    /**
     * @since 1.9
     */
    public abstract AnnotatedMethod findAnyGetter() throws IllegalArgumentException;

    /**
     * @since 1.9
     */
    public abstract AnnotatedMethod findAnySetter() throws IllegalArgumentException;
    
    /**
     * @since 1.9
     */
    public abstract Set<String> getIgnoredPropertyNames();

    /*
    /**********************************************************
    /* Deprecated methods
    /**********************************************************
     */

    /**
     * @deprecated Since 1.9 use the non-deprecated version
     */
    @Deprecated
    public LinkedHashMap<String,AnnotatedMethod> findGetters(VisibilityChecker<?> visibilityChecker,
            Collection<String> ignoredProperties) {
        return (LinkedHashMap<String,AnnotatedMethod>)findGetters(ignoredProperties);
    }

    /**
     * @deprecated Since 1.9 use the non-deprecated version
     */
    @Deprecated
    public LinkedHashMap<String,AnnotatedMethod> findSetters(VisibilityChecker<?> visibilityChecker) {
        return (LinkedHashMap<String,AnnotatedMethod>)findSetters();
    }

    /**
     * @deprecated Since 1.9 use the non-deprecated version
     */
    @Deprecated
    public LinkedHashMap<String,AnnotatedField> findDeserializableFields(VisibilityChecker<?> visibilityChecker,
            Collection<String> ignoredProperties)
    {
        return (LinkedHashMap<String,AnnotatedField>) findDeserializableFields(ignoredProperties);
    }

    /**
     * @deprecated Since 1.9 use the non-deprecated version
     */
    @Deprecated
    public Map<String,AnnotatedField> findSerializableFields(VisibilityChecker<?> visibilityChecker,
            Collection<String> ignoredProperties) {
        return (LinkedHashMap<String,AnnotatedField>) findSerializableFields(ignoredProperties);
    }

}
