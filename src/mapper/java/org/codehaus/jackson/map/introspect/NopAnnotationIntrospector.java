package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.annotate.JsonSerialize.Typing;

/**
 * Dummy, "no-operation" implementation of {@link AnnotationIntrospector}.
 * Can be used as is to suppress handling of annotations; or as a basis
 * for simple complementary annotators
 */
public class NopAnnotationIntrospector
    extends AnnotationIntrospector
{
    /**
     * Static immutable and shareable instance that can be used as
     * "null" introspector: one that never finds any annotation
     * information.
     */
    public final static NopAnnotationIntrospector instance = new NopAnnotationIntrospector();

    /*
    ////////////////////////////////////////////////////
    // General annotation properties
    ////////////////////////////////////////////////////
     */

    @Override
    public boolean isHandled(Annotation ann) {
        return false;
    }

    /*
    ////////////////////////////////////////////////////
    // General annotations
    ////////////////////////////////////////////////////
     */

    @Override
    public String findNamespace(Annotated ann)
    {
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // General class annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public Boolean findCachability(AnnotatedClass ac) {
        return null;
    }

    @Override
    public Boolean findCreatorAutoDetection(AnnotatedClass ac) {
        return null;
    }

    @Override
    public String findDeserializablePropertyName(AnnotatedField af) {
        return null;
    }

    @Override
    public Class<?> findDeserializationContentType(Annotated am) {
        return null;
    }

    @Override
    public Class<?> findDeserializationKeyType(Annotated am) {
        return null;
    }

    @Override
    public Class<?> findDeserializationType(Annotated am) {
        return null;
    }

    @Override
    public Object findDeserializer(Annotated am) { return null; }

    @Override
    public Class<KeyDeserializer> findKeyDeserializer(Annotated am) { return null; }

    @Override
    public Class<JsonDeserializer<?>> findContentDeserializer(Annotated am) { return null; }

    @Override
    public String findEnumValue(Enum<?> value) {
        return null;
    }

    @Override
    public Boolean findFieldAutoDetection(AnnotatedClass ac) {
        return null;
    }

    @Override
    public String findGettablePropertyName(AnnotatedMethod am) {
        return null;
    }

    @Override
    public Boolean findGetterAutoDetection(AnnotatedClass ac) {
        return null;
    }

    /**
     * @since 1.3
     */
    @Override
    public Boolean findIsGetterAutoDetection(AnnotatedClass ac) {
        return null;
    }

    @Override
    public String findPropertyNameForParam(AnnotatedParameter param) {
        return null;
    }

    @Override
    public String findSerializablePropertyName(AnnotatedField af) {
        return null;
    }

    @Override
    public Inclusion findSerializationInclusion(Annotated a, Inclusion defValue) {
        return Inclusion.ALWAYS;
    }

    @Override
    public Class<?> findSerializationType(Annotated a) {
        return null;
    }

    @Override
    public Typing findSerializationTyping(Annotated a) {
        return null;
    }

    @Override
    public Object findSerializer(Annotated am) {
        return null;
    }

    @Override
    public String findSettablePropertyName(AnnotatedMethod am) {
        return null;
    }

    @Override
    public Boolean findSetterAutoDetection(AnnotatedClass ac) {
        return null;
    }

    @Override
    public boolean hasAnySetterAnnotation(AnnotatedMethod am) {
        return false;
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am) {
        return false;
    }

    @Override
    public boolean hasCreatorAnnotation(Annotated a) {
        return false;
    }

    @Override
    public boolean isIgnorableConstructor(AnnotatedConstructor c) {
        return false;
    }

    @Override
    public boolean isIgnorableField(AnnotatedField f) {
        return false;
    }

    @Override
    public boolean isIgnorableMethod(AnnotatedMethod m) {
        return false;
    }

}
