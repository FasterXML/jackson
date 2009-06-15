package org.codehaus.jackson.map;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.Annotated;
import org.codehaus.jackson.map.introspect.AnnotatedClass;
import org.codehaus.jackson.map.introspect.AnnotatedField;
import org.codehaus.jackson.map.introspect.AnnotatedMethod;

/**
 * Abstract class that defines API used for introspecting annotation-based
 * configuration for serialization and deserialization. Separated
 * so that different sets of annotations can be supported, and support
 * plugged-in dynamically.
 */
public abstract class AnnotationIntrospector
{
    /*
    ///////////////////////////////////////////////////////
    // Generic annotation properties, lookup
    ///////////////////////////////////////////////////////
    */

    /**
     * Method called by framework to determine whether given annotation
     * is handled by this introspector.
     */
    public abstract boolean isHandled(Annotation ann);

    /*
    ///////////////////////////////////////////////////////
    // General class annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method that checks whether specified class has annotations
     * that indicate that it is (or is not) cachable. Exact
     * semantics depend on type of class annotated and using
     * class (factory or provider).
     *<p>
     * Currently only used
     * with deserializers, to determine whether provider
     * should cache instances, and if no annotations are found,
     * assumes non-cachable instances.
     *
     * @return True, if class is considered cachable within context;
     *   False if not, and null if introspector does not care either
     *   way.
     */
    public abstract Boolean findCachability(AnnotatedClass ac);

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether field auto detection should be enabled.
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findFieldAutoDetection(AnnotatedClass ac);

    /*
    ///////////////////////////////////////////////////////
    // General method annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is an annotation that
     * indicates that given method should be ignored for all
     * operations (serialization, deserialization).
     *
     * @return True, if an annotation is found to indicate that the
     *    method should be ignored; false if not.
     */
    public abstract boolean isIgnorableMethod(AnnotatedMethod m);


    /*
    ////////////////////////////////////////////////////
    // General field annotations
    ////////////////////////////////////////////////////
     */

    /**
     * Method for checking whether there is an annotation that
     * indicates that given field should be ignored for all
     * operations (serialization, deserialization).
     *
     * @return True, if an annotation is found to indicate that the
     *    field should be ignored; false if not.
     */
    public abstract boolean isIgnorableField(AnnotatedField f);

    /*
    ///////////////////////////////////////////////////////
    // Serialization: general annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for getting a serializer definition on specified method
     * or field. Type of definition is either instance (of type
     * {@link JsonSerializer}) or Class (of type
     * <code>Class<JsonSerializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public abstract Object findSerializer(Annotated am);

    /**
     * Method for checking whether given annotated entity (class, method,
     * field) defines which Bean/Map properties are to be included in
     * serialization.
     * If no annotation is found, method should return given second
     * argument; otherwise value indicated by the annotation
     *
     * @return Enumerated value indicating which properties to include
     *   in serialization
     */
    public abstract JsonSerialize.Properties findSerializationInclusion(Annotated a, JsonSerialize.Properties defValue);

    /**
     * Method for accessing annotated type definition that a
     * method can have, to be used as the type for serialization
     * instead of the runtime type.
     * Type returned (if any) needs to be widening conversion (super-type).
     * Declared return type of the method is also considered acceptable.
     *
     * @return Class to use instead of runtime type
     */
    public abstract Class<?> findSerializationType(Annotated a);

    /*
    ///////////////////////////////////////////////////////
    // Serialization: class annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether getter-method auto detection should be enabled.
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findGetterAutoDetection(AnnotatedClass ac);

    /*
    ///////////////////////////////////////////////////////
    // Serialization: method annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether given method has an annotation
     * that suggests property name associated with method that
     * may be a "getter". Should return null if no annotation
     * is found; otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use standard bean name detection if applicable;
     * method name if not".
     */
    public abstract String findGettablePropertyName(AnnotatedMethod am);

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the return value of annotated method
     * should be used as "the value" of the object instance; usually
     * serialized as a primitive value such as String or number.
     *
     * @return True if such annotation is found (and is not disabled);
     *   false if no enabled annotation is found
     */
    public abstract boolean hasAsValueAnnotation(AnnotatedMethod am);

    /**
     * Method for determining the String value to use for serializing
     * given enumeration entry; used when serializing enumerations
     * as Strings (the standard method).
     *
     * @return Serialized enum value.
     */
    public abstract String findEnumValue(Enum<?> value);

    /*
    ///////////////////////////////////////////////////////
    // Serialization: field annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether given member field represent
     * a serializable logical property; and if so, returns the
     * name of that property.
     * Should return null if no annotation is found (indicating it
     * is not a serializable field); otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use the field name as is".
     */
    public abstract String findSerializablePropertyName(AnnotatedField af);

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: general annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for getting a deserializer definition on specified method
     * or field. Type of definition is either instance (of type
     * {@link JsonDeserializer}) or Class (of type
     * <code>Class<JsonDeserializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public abstract Object findDeserializer(Annotated am);

    /**
     * Method for accessing annotated type definition that a
     * method can have, to be used as the type for serialization
     * instead of the runtime type.
     * Type must be a narrowing conversion
     * (i.e.subtype of declared type).
     * Declared return type of the method is also considered acceptable.
     *
     * @return Class to use for deserialization instead of declared type
     */
    public abstract Class<?> findDeserializationType(Annotated am);

    /**
     * Method for accessing additional narrowing type definition that a
     * method can have, to define more specific key type to use.
     * It should be only be used with {@link java.util.Map} types.
     *
     * @return Class specifying more specific type to use instead of
     *   declared type, if annotation found; null if not
     */
    public abstract Class<?> findDeserializationKeyType(Annotated am);

    /**
     * Method for accessing additional narrowing type definition that a
     * method can have, to define more specific content type to use;
     * content refers to Map values and Collection/array elements.
     * It should be only be used with Map, Collection and array types.
     *
     * @return Class specifying more specific type to use instead of
     *   declared type, if annotation found; null if not
     */
    public abstract Class<?> findDeserializationContentType(Annotated am);

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: class annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether creator-method auto detection should be enabled.
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findCreatorAutoDetection(AnnotatedClass ac);

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether setter-method auto detection should be enabled.
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findSetterAutoDetection(AnnotatedClass ac);

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: method annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether given method has an annotation
     * that suggests property name associated with method that
     * may be a "setter". Should return null if no annotation
     * is found; otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use standard bean name detection if applicable;
     * method name if not".
     */
    public abstract String findSettablePropertyName(AnnotatedMethod am);

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is to serve as "any setter";
     * method to be used for setting values of any properties for
     * which no dedicated setter method is found.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public abstract boolean hasAnySetterAnnotation(AnnotatedMethod am);

    /**
     * Method for checking whether given method has an annotation
     * that suggests that the method is a "creator" (aka factory)
     * method to be used for construct new instances of deserialized
     * values.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public abstract boolean hasCreatorAnnotation(AnnotatedMethod am);

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: field annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether given member field represent
     * a deserializable logical property; and if so, returns the
     * name of that property.
     * Should return null if no annotation is found (indicating it
     * is not a deserializable field); otherwise a non-null String.
     * If non-null value is returned, it is used as the property
     * name, except for empty String ("") which is taken to mean
     * "use the field name as is".
     */
    public abstract String findDeserializablePropertyName(AnnotatedField af);

    /*
    ///////////////////////////////////////////////////////
    // Helper classes
    ///////////////////////////////////////////////////////
    */

    /**
     * Helper class that allows using 2 introspectors such that one
     * introspector acts as the primary one to use; and second one
     * as a fallback used if the primary does not provide conclusive
     * or useful result for a method.
     *<p>
     * An obvious consequence of priority is that it is easy to construct
     * longer chains of introspectors by linking multiple pairs.
     * Currently most likely combination is that of using the default
     * Jackson provider, along with JAXB annotation introspector (available
     * since version 1.1).
     */
    public static class Pair
        extends AnnotationIntrospector
    {
        final AnnotationIntrospector _primary, _secondary;

        public Pair(AnnotationIntrospector p,
                    AnnotationIntrospector s)
        {
            _primary = p;
            _secondary = s;
        }

        // // // Generic annotation properties, lookup
        
        @Override
        public boolean isHandled(Annotation ann)
        {
            return _primary.isHandled(ann) || _secondary.isHandled(ann);
        }

        // // // General class annotations

        @Override
        public Boolean findCachability(AnnotatedClass ac)
        {
            Boolean result = _primary.findCachability(ac);
            if (result == null) {
                result = _secondary.findCachability(ac);
            }
            return result;
        }
        
        @Override
        public Boolean findFieldAutoDetection(AnnotatedClass ac)
        {
            Boolean result = _primary.findFieldAutoDetection(ac);
            if (result == null) {
                result = _secondary.findFieldAutoDetection(ac);
            }
            return result;
        }
        
        // // // General method annotations

        @Override
        public boolean isIgnorableMethod(AnnotatedMethod m)
        {
            return _primary.isIgnorableMethod(m) || _secondary.isIgnorableMethod(m);
        }
        
        // // // General field annotations

        @Override
        public boolean isIgnorableField(AnnotatedField f)
        {
            return _primary.isIgnorableField(f) || _secondary.isIgnorableField(f);
        }

        // // // Serialization: general annotations

        @Override
        public Object findSerializer(Annotated am)
        {
            Object result = _primary.findSerializer(am);
            /* Are there non-null results that should be ignored?
             * (i.e. should some validation be done here)
             * For now let's assume no
             */
            if (result == null) {
                result = _secondary.findSerializer(am);
            }
            return result;
        }

        @Override
        public JsonSerialize.Properties findSerializationInclusion(Annotated a,
                                                                   JsonSerialize.Properties defValue)
        {
            /* This is bit trickier: need to combine results in a meaningful
             * way. Seems like it should be a disjoint; that is, most
             * restrictive value should be returned.
             * For enumerations, comparison is done by indexes, which
             * works: largest value is the last one, which is the most
             * restrictive value as well.
             */
            JsonSerialize.Properties v1 = _primary.findSerializationInclusion(a, defValue);
            JsonSerialize.Properties v2 = _secondary.findSerializationInclusion(a, defValue);
            return (v1.compareTo(v2) < 0) ? v2 : v1;
        }
        
        @Override
        public Class<?> findSerializationType(Annotated a)
        {
            Class<?> result = _primary.findSerializationType(a);
            if (result == null) {
                result = _secondary.findSerializationType(a);
            }
            return result;
        }

        // // // Serialization: class annotations

        @Override
        public Boolean findGetterAutoDetection(AnnotatedClass ac)
        {
            Boolean result = _primary.findGetterAutoDetection(ac);
            if (result == null) {
                result = _secondary.findGetterAutoDetection(ac);
            }
            return result;
        }

        // // // Serialization: method annotations
        
        @Override
        public String findGettablePropertyName(AnnotatedMethod am)
        {
            String result = _primary.findGettablePropertyName(am);
            if (result == null) {
                result = _secondary.findGettablePropertyName(am);
            }
            return result;
        }
        
        @Override
        public boolean hasAsValueAnnotation(AnnotatedMethod am)
        {
            return _primary.hasAsValueAnnotation(am) || _secondary.hasAsValueAnnotation(am);
        }
        
        @Override
        public String findEnumValue(Enum<?> value)
        {
            String result = _primary.findEnumValue(value);
            if (result == null) {
                result = _secondary.findEnumValue(value);
            }
            return result;
        }        

        // // // Serialization: field annotations

        @Override
        public String findSerializablePropertyName(AnnotatedField af)
        {
            String result = _primary.findSerializablePropertyName(af);
            if (result == null) {
                result = _secondary.findSerializablePropertyName(af);
            }
            return result;
        }

        // // // Deserialization: general annotations

        @Override
        public Object findDeserializer(Annotated am)
        {
            Object result = _primary.findDeserializer(am);
            if (result == null) {
                result = _secondary.findDeserializer(am);
            }
            return result;
        }
        
        @Override
        public Class<?> findDeserializationType(Annotated am)
        {
            Class<?> result = _primary.findDeserializationType(am);
            if (result == null) {
                result = _secondary.findDeserializationType(am);
            }
            return result;
        }

        @Override
        public Class<?> findDeserializationKeyType(Annotated am)
        {
            Class<?> result = _primary.findDeserializationKeyType(am);
            if (result == null) {
                result = _secondary.findDeserializationKeyType(am);
            }
            return result;
        }

        @Override
        public Class<?> findDeserializationContentType(Annotated am)
        {
            Class<?> result = _primary.findDeserializationContentType(am);
            if (result == null) {
                result = _secondary.findDeserializationContentType(am);
            }
            return result;
        }
        
        // // // Deserialization: class annotations
        
        @Override
        public Boolean findCreatorAutoDetection(AnnotatedClass ac)
        {
            Boolean result = _primary.findCreatorAutoDetection(ac);
            if (result == null) {
                result = _secondary.findCreatorAutoDetection(ac);
            }
            return result;
        }

        @Override
        public Boolean findSetterAutoDetection(AnnotatedClass ac)
        {
            Boolean result = _primary.findSetterAutoDetection(ac);
            if (result == null) {
                result = _secondary.findSetterAutoDetection(ac);
            }
            return result;
        }

        // // // Deserialization: method annotations

        @Override
        public String findSettablePropertyName(AnnotatedMethod am)
        {
            String result = _primary.findSettablePropertyName(am);
            if (result == null) {
                result = _secondary.findSettablePropertyName(am);
            }
            return result;
        }
        
        @Override
        public boolean hasAnySetterAnnotation(AnnotatedMethod am)
        {
            return _primary.hasAnySetterAnnotation(am) || _secondary.hasAnySetterAnnotation(am);
        }

        @Override
        public boolean hasCreatorAnnotation(AnnotatedMethod am)
        {
            return _primary.hasCreatorAnnotation(am) || _secondary.hasCreatorAnnotation(am);
        }
        
        // // // Deserialization: field annotations

        @Override
        public String findDeserializablePropertyName(AnnotatedField af)
        {
            String result = _primary.findDeserializablePropertyName(af);
            if (result == null) {
                result = _secondary.findDeserializablePropertyName(af);
            }
            return result;
        }
    }
}
