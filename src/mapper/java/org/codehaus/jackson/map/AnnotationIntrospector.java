package org.codehaus.jackson.map;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.introspect.*;

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
    // General annotations for serialization+deserialization
    ///////////////////////////////////////////////////////
    */

    /**
     * Method that can be called to figure out generic namespace
     * property for an annotated object. Most commonly used
     * for XML compatibility purposes (since currently only
     * JAXB annotations provide namespace information), but
     * theoretically is not limited to XML.
     *
     * @return Null if annotated thing does not define any
     *   namespace information; non-null namespace (which may
     *   be empty String) otherwise
     */
    public abstract String findNamespace(Annotated ann);

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
     * @return True, if class is considered cachable within context,
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
     *<p>
     * Note that this method should <b>ONLY</b> return true for such
     * explicit ignoral cases; and not if method just happens not to
     * be visible for annotation processor.
     *
     * @return True, if an annotation is found to indicate that the
     *    method should be ignored; false if not.
     */
    public abstract boolean isIgnorableMethod(AnnotatedMethod m);

    /**
     * @since 1.2
     */
    public abstract boolean isIgnorableConstructor(AnnotatedConstructor c);

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
    public abstract JsonSerialize.Inclusion findSerializationInclusion(Annotated a, JsonSerialize.Inclusion defValue);

    /**
     * Method for accessing annotated type definition that a
     * method/field can have, to be used as the type for serialization
     * instead of the runtime type.
     * Type returned (if any) needs to be widening conversion (super-type).
     * Declared return type of the method is also considered acceptable.
     *
     * @return Class to use instead of runtime type
     */
    public abstract Class<?> findSerializationType(Annotated a);

    /**
     * Method for accessing declared typing mode annotated (if any).
     * This is used for type detection, unless more granular settings
     * (such as actual exact type; or serializer to use which means
     * no type information is needed) take precedence.
     *
     * @since 1.2
     *
     * @return Typing mode to use, if annotation is found; null otherwise
     */
    public abstract JsonSerialize.Typing findSerializationTyping(Annotated a);

    /*
    ///////////////////////////////////////////////////////
    // Serialization: class annotations
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether (regular) getter-method auto detection
     * should be enabled.
     * Getter methods are methods with name "getXxx()" (for property "xxx");
     * "isXxx()" are not included (instead those are considered
     * "is getters")
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findGetterAutoDetection(AnnotatedClass ac);

    /**
     * Method for checking whether there is a class annotation that
     * indicates whether (regular) getter-method auto detection
     * should be enabled.
     * Getter methods are methods with name "getXxx()" (for property "xxx");
     * "isXxx()" are not included (instead those are considered
     * "is getters")
     *
     * @since 1.3
     *
     * @return null if no relevant annotation is located; Boolean.TRUE
     *   if enabling annotation found, Boolean.FALSE if disabling annotation
     */
    public abstract Boolean findIsGetterAutoDetection(AnnotatedClass ac);

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
     * or field.
     * Type of definition is either instance (of type
     * {@link JsonDeserializer}) or Class (of type
     * <code>Class<JsonDeserializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     */
    public abstract Object findDeserializer(Annotated am);

    /**
     * Method for getting a deserializer definition for keys of
     * associated <code>Map</code> property.
     * Type of definition is either instance (of type
     * {@link JsonDeserializer}) or Class (of type
     * <code>Class<JsonDeserializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     * 
     * @since 1.3
     */
    public abstract Class<? extends KeyDeserializer> findKeyDeserializer(Annotated am);

    /**
     * Method for getting a deserializer definition for content (values) of
     * associated <code>Collection</code>, <code>array</code> or
     * <code>Map</code> property.
     * Type of definition is either instance (of type
     * {@link JsonDeserializer}) or Class (of type
     * <code>Class<JsonDeserializer></code>); if value of different
     * type is returned, a runtime exception may be thrown by caller.
     * 
     * @since 1.3
     */
    public abstract Class<? extends JsonDeserializer<?>> findContentDeserializer(Annotated am);

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
     * Method for checking whether given annotated item (method, constructor)
     * has an annotation
     * that suggests that the method is a "creator" (aka factory)
     * method to be used for construct new instances of deserialized
     * values.
     *
     * @return True if such annotation is found (and is not disabled),
     *   false otherwise
     */
    public abstract boolean hasCreatorAnnotation(Annotated a);

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
    // Deserialization: parameter annotations (for
    // creator method parameters)
    ///////////////////////////////////////////////////////
    */

    /**
     * Method for checking whether given set of annotations indicates
     * property name for associated parameter.
     * No actual parameter object can be passed since JDK offers no
     * representation; just annotations.
     */
    public abstract String findPropertyNameForParam(AnnotatedParameter param);

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

        // // // General annotations

        /**
         * Combination logic is such that if the primary returns
         * non-null non-empty namespace, that is returned.
         * Otherwise, if secondary returns non-null non-empty
         * namespace, that is returned.
         * Otherwise empty String is returned if either one
         * returned empty String; otherwise null is returned
         * (in case where both returned null)
         */
        @Override
        public String findNamespace(Annotated ann)
        {
            String ns1 = _primary.findNamespace(ann);
            if (ns1 == null) {
                return _secondary.findNamespace(ann);
            } else if (ns1.length() > 0) {
                return ns1;
            }
            // Ns1 is empty; how about secondary?
            String ns2 = _secondary.findNamespace(ann);
            return (ns2 == null) ? ns1 : ns2;
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
        public boolean isIgnorableMethod(AnnotatedMethod m) {
            return _primary.isIgnorableMethod(m) || _secondary.isIgnorableMethod(m);
        }
        
        @Override
        public boolean isIgnorableConstructor(AnnotatedConstructor c) {
            return _primary.isIgnorableConstructor(c) || _secondary.isIgnorableConstructor(c);
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
        public JsonSerialize.Inclusion findSerializationInclusion(Annotated a,
                                                                   JsonSerialize.Inclusion defValue)
        {
            /* This is bit trickier: need to combine results in a meaningful
             * way. Seems like it should be a disjoint; that is, most
             * restrictive value should be returned.
             * For enumerations, comparison is done by indexes, which
             * works: largest value is the last one, which is the most
             * restrictive value as well.
             */
            JsonSerialize.Inclusion v1 = _primary.findSerializationInclusion(a, defValue);
            JsonSerialize.Inclusion v2 = _secondary.findSerializationInclusion(a, defValue);
            if (v1 == null) {
                return v2;
            }
            if (v2 == null) {
                return v1;
            }
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

        @Override
        public JsonSerialize.Typing findSerializationTyping(Annotated a)
        {
            JsonSerialize.Typing result = _primary.findSerializationTyping(a);
            if (result == null) {
                result = _secondary.findSerializationTyping(a);
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

        @Override
        public Boolean findIsGetterAutoDetection(AnnotatedClass ac)
        {
            Boolean result = _primary.findIsGetterAutoDetection(ac);
            if (result == null) {
                result = _secondary.findIsGetterAutoDetection(ac);
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
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findGettablePropertyName(am);
                if (str2 != null) {
                    result = str2;
                }
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
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findSerializablePropertyName(af);
                if (str2 != null) {
                    result = str2;
                }
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
        public Class<? extends KeyDeserializer> findKeyDeserializer(Annotated am)
        {
            Class<? extends KeyDeserializer> result = _primary.findKeyDeserializer(am);
            if (result == null || result == KeyDeserializer.None.class) {
                result = _secondary.findKeyDeserializer(am);
            }
            return result;
        }

        @Override
        public Class<? extends JsonDeserializer<?>> findContentDeserializer(Annotated am)
        {
            Class<? extends JsonDeserializer<?>> result = _primary.findContentDeserializer(am);
            if (result == null || result == JsonDeserializer.None.class) {
                result = _secondary.findContentDeserializer(am);
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
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findSettablePropertyName(am);
                if (str2 != null) {
                    result = str2;
                }
            }
            return result;
        }
        
        @Override
        public boolean hasAnySetterAnnotation(AnnotatedMethod am)
        {
            return _primary.hasAnySetterAnnotation(am) || _secondary.hasAnySetterAnnotation(am);
        }

        @Override
        public boolean hasCreatorAnnotation(Annotated a)
        {
            return _primary.hasCreatorAnnotation(a) || _secondary.hasCreatorAnnotation(a);
        }
        
        // // // Deserialization: field annotations

        @Override
        public String findDeserializablePropertyName(AnnotatedField af)
        {
            String result = _primary.findDeserializablePropertyName(af);
            if (result == null) {
                result = _secondary.findDeserializablePropertyName(af);
            } else if (result.length() == 0) {
                /* Empty String is a default; can be overridden by
                 * more explicit answer from secondary entry
                 */
                String str2 = _secondary.findDeserializablePropertyName(af);
                if (str2 != null) {
                    result = str2;
                }
            }
            return result;
        }

        // // // Deserialization: parameter annotations (for creators)

        @Override
        public String findPropertyNameForParam(AnnotatedParameter param)
        {
            String result = _primary.findPropertyNameForParam(param);
            if (result == null) {
                result = _secondary.findPropertyNameForParam(param);
            }
            return result;
        }
    }

}
