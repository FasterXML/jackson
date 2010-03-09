package org.codehaus.jackson.map.introspect;

import java.util.*;
import java.lang.annotation.Annotation;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.JsonDeserializer;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.KeyDeserializer;
import org.codehaus.jackson.map.annotate.*;
import org.codehaus.jackson.map.jsontype.NamedType;
import org.codehaus.jackson.map.jsontype.TypeIdResolver;
import org.codehaus.jackson.map.jsontype.TypeResolverBuilder;
import org.codehaus.jackson.map.jsontype.impl.StdTypeResolverBuilder;
import org.codehaus.jackson.map.util.ClassUtil;
import org.codehaus.jackson.type.JavaType;

/**
 * {@link AnnotationIntrospector} implementation that handles standard
 * Jackson annotations.
 */
public class JacksonAnnotationIntrospector
    extends AnnotationIntrospector
{
    public JacksonAnnotationIntrospector() { }

    /*
    /****************************************************
    /* General annotation properties
    /****************************************************
     */

    @Override
    public boolean isHandled(Annotation ann)
    {
        Class<? extends Annotation> acls = ann.annotationType();

        /* 16-May-2009, tatu: used to check this like so...
           final String JACKSON_PKG_PREFIX = "org.codehaus.jackson";

           Package pkg = acls.getPackage();
           return (pkg != null) && (pkg.getName().startsWith(JACKSON_PKG_PREFIX));
        */

        // but this is more reliable, now that we have tag annotation:
        return acls.getAnnotation(JacksonAnnotation.class) != null;
    }

    /*
    /****************************************************
    /* General annotations
    /****************************************************
     */

    @Override
    public String findNamespace(Annotated ann)
    {
        // Jackson has no annotations for this (JAXB does)
        return null;
    }

    @Override
    public String findEnumValue(Enum<?> value)
    {
        return value.name();
    }
    
    /*
    /****************************************************
    /* General class annotations
    /****************************************************
     */

    @Override
    public Boolean findCachability(AnnotatedClass ac)
    {
        JsonCachable ann = ac.getAnnotation(JsonCachable.class);
        if (ann == null) {
            return null;
        }
        return ann.value() ? Boolean.TRUE : Boolean.FALSE;
    }

    @Override
    public String findRootName(AnnotatedClass ac)
    {
        // No annotation currently defined for this feature, so:
        return null;
    }

    @Override
    public String[] findPropertiesToIgnore(AnnotatedClass ac) {
        JsonIgnoreProperties ignore = ac.getAnnotation(JsonIgnoreProperties.class);
        return (ignore == null) ? null : ignore.value();
    }

    @Override
    public Boolean findIgnoreUnknownProperties(AnnotatedClass ac) {
        JsonIgnoreProperties ignore = ac.getAnnotation(JsonIgnoreProperties.class);
        return (ignore == null) ? null : ignore.ignoreUnknown();
    }

    /*
    /******************************************************
    /* Property auto-detection
    /******************************************************
     */
    
    @Override
    public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
        VisibilityChecker<?> checker)
    {
        JsonAutoDetect ann = ac.getAnnotation(JsonAutoDetect.class);
        return (ann == null) ? checker : checker.with(ann);
    }
    
    /*
    /****************************************************
    /* Class annotations for PM type handling (1.5+)
    /****************************************************
    */
    
    @Override
    public TypeResolverBuilder<?> findTypeResolver(AnnotatedClass ac, JavaType baseType)
    {
        // First: maybe we have explicit type resolver?
        TypeResolverBuilder<?> b;
        JsonTypeInfo info = ac.getAnnotation(JsonTypeInfo.class);
        JsonTypeResolver resAnn = ac.getAnnotation(JsonTypeResolver.class);
        if (resAnn != null) {
            /* let's not try to force access override (would need to pass
             * settings through if we did, since that's not doable on some
             * platforms)
             */
            b = ClassUtil.createInstance(resAnn.value(), false);
        } else { // if not, use standard one, if indicated by annotations
            if (info == null || info.use() == JsonTypeInfo.Id.NONE) {
                return null;
            }
            b = new StdTypeResolverBuilder();
        }
        // Does it define a custom type id resolver?
        JsonTypeIdResolver idResInfo = ac.getAnnotation(JsonTypeIdResolver.class);
        TypeIdResolver idRes = (idResInfo == null) ? null
                : ClassUtil.createInstance(idResInfo.value(), false);
        b = b.init(info.use(), idRes);
        b = b.inclusion(info.include());
        b = b.typeProperty(info.property());
        return b;
    }

    @Override
    public TypeResolverBuilder<?> findPropertyTypeResolver(AnnotatedMember am, JavaType baseType)
    {
        // No per-member type overrides (yet)
        return null;
    }

    @Override
    public TypeResolverBuilder<?> findPropertyContentTypeResolver(AnnotatedMember am, JavaType baseType)
    {
        // No per-member type overrides (yet)
        return null;
    }
    
    @Override
    public List<NamedType> findSubtypes(Annotated a)
    {
        JsonSubTypes t = a.getAnnotation(JsonSubTypes.class);
        if (t == null) return null;
        JsonSubTypes.Type[] types = t.value();
        ArrayList<NamedType> result = new ArrayList<NamedType>(types.length);
        for (JsonSubTypes.Type type : types) {
            result.add(new NamedType(type.value(), type.name()));
        }
        return result;
    }

    @Override        
    public String findTypeName(AnnotatedClass ac)
    {
        JsonTypeName tn = ac.getAnnotation(JsonTypeName.class);
        return (tn == null) ? null : tn.value();
    }

    /*
    ///////////////////////////////////////////////////////
    // General method annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public boolean isIgnorableMethod(AnnotatedMethod m) {
        return _isIgnorable(m);
    }

    @Override
    public boolean isIgnorableConstructor(AnnotatedConstructor c) {
        return _isIgnorable(c);
    }

    /*
    ////////////////////////////////////////////////////
    // General field annotations
    ////////////////////////////////////////////////////
     */

    @Override
    public boolean isIgnorableField(AnnotatedField f) {
        return _isIgnorable(f);
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: general annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public Class<? extends JsonSerializer<?>> findSerializer(Annotated a)
    {
        /* 21-May-2009, tatu: Slight change; primary annotation is now
         *    @JsonSerialize; @JsonUseSerializer is deprecated
         */
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<? extends JsonSerializer<?>> serClass = ann.using();
            if (serClass != JsonSerializer.None.class) {
                return serClass;
            }
        }
        // 31-Jan-2010, tatus: @JsonUseSerializer removed as of 1.5
        return null;
    }

    @Override
    public JsonSerialize.Inclusion findSerializationInclusion(Annotated a, JsonSerialize.Inclusion defValue)
    {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            return ann.include();
        }
        /* 23-May-2009, tatu: Will still support now-deprecated (as of 1.1)
         *   legacy annotation too:
         */
        JsonWriteNullProperties oldAnn = a.getAnnotation(JsonWriteNullProperties.class);
        if (oldAnn != null) {
            boolean writeNulls = oldAnn.value();
            return writeNulls ? JsonSerialize.Inclusion.ALWAYS : JsonSerialize.Inclusion.NON_NULL;
        }
        return defValue;
    }

    @Override
    public Class<?> findSerializationType(Annotated am)
    {
        // Primary annotation, JsonSerialize
        JsonSerialize ann = am.getAnnotation(JsonSerialize.class);
        if (ann != null) {
            Class<?> cls = ann.as();
            if (cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }
    
    @Override
    public JsonSerialize.Typing findSerializationTyping(Annotated a)
    {
        JsonSerialize ann = a.getAnnotation(JsonSerialize.class);
        return (ann == null) ? null : ann.typing();
    }

    @Override
    public Class<?>[] findSerializationViews(Annotated a)
    {
        JsonView ann = a.getAnnotation(JsonView.class);
        return (ann == null) ? null : ann.value();
    }
    
    /*
    ///////////////////////////////////////////////////////
    // Serialization: class annotations
    ///////////////////////////////////////////////////////
    */


    public String[] findSerializationPropertyOrder(AnnotatedClass ac) {
        JsonPropertyOrder order = ac.getAnnotation(JsonPropertyOrder.class);
        return (order == null) ? null : order.value();
    }

    public Boolean findSerializationSortAlphabetically(AnnotatedClass ac) {
        JsonPropertyOrder order = ac.getAnnotation(JsonPropertyOrder.class);
        return (order == null) ? null : order.alphabetic();
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: method annotations
    ///////////////////////////////////////////////////////
    */

    @SuppressWarnings("deprecation")
    @Override
    public String findGettablePropertyName(AnnotatedMethod am)
    {
        /* 22-May-2009, tatu: JsonProperty is the primary annotation
         *   to check for
         */
        JsonProperty pann = am.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        /* 22-May-2009, tatu: JsonGetter is deprecated as of 1.1
         *    but still supported
         */
        JsonGetter ann = am.getAnnotation(JsonGetter.class);
        if (ann != null) {
            return ann.value();
        }
        /* 22-May-2009, tatu: And finally, JsonSerialize implies
         *   that there is a property, although doesn't define name
         */
        if (am.hasAnnotation(JsonSerialize.class)) {
            return "";
        }
        return null;
    }

    @Override
    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        JsonValue ann = am.getAnnotation(JsonValue.class);
        // value of 'false' means disabled...
        return (ann != null && ann.value());
    }

    /*
    ///////////////////////////////////////////////////////
    // Serialization: field annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public String findSerializablePropertyName(AnnotatedField af)
    {
        JsonProperty pann = af.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        // Also: having JsonSerialize implies it is such a property
        if (af.hasAnnotation(JsonSerialize.class)) {
            return "";
        }
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: general annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public Class<? extends JsonDeserializer<?>> findDeserializer(Annotated a)
    {
        /* 21-May-2009, tatu: Slight change; primary annotation is now
         *    @JsonDeserialize; @JsonUseDeserializer is deprecated
         */
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<? extends JsonDeserializer<?>> deserClass = ann.using();
            if (deserClass != JsonDeserializer.None.class) {
                return deserClass;
            }
        }
        // 31-Jan-2010, tatus: @JsonUseDeserializer removed as of 1.5
        return null;
    }

    @Override
    public Class<? extends KeyDeserializer> findKeyDeserializer(Annotated a)
    {
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<? extends KeyDeserializer> deserClass = ann.keyUsing();
            if (deserClass != KeyDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Class<? extends JsonDeserializer<?>> findContentDeserializer(Annotated a)
    {
        JsonDeserialize ann = a.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<? extends JsonDeserializer<?>> deserClass = ann.contentUsing();
            if (deserClass != JsonDeserializer.None.class) {
                return deserClass;
            }
        }
        return null;
    }

    @Override
    public Class<?> findDeserializationType(Annotated am, JavaType baseType,
            String propName)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.as();
            if (cls != NoClass.class) {
                return cls;
            }
        }

        /* TODO: !!! 21-May-2009, tatu: JsonClass is deprecated; will need to
         *    drop support at a later point (for 2.0?)
         */
        @SuppressWarnings("deprecation")
        JsonClass oldAnn = am.getAnnotation(JsonClass.class);
        if (oldAnn != null) {
            @SuppressWarnings("deprecation")
            Class<?> cls = oldAnn.value();
            if(cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    @Override
    public Class<?> findDeserializationKeyType(Annotated am, JavaType baseKeyType,
            String propName)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.keyAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }

        /* !!! 21-May-2009, tatu: JsonClass is deprecated; will need to
         *    drop support at a later point (for 2.0?)
         */
        @SuppressWarnings("deprecation")
        JsonKeyClass oldAnn = am.getAnnotation(JsonKeyClass.class);
        if (oldAnn != null) {
            @SuppressWarnings("deprecation")
            Class<?> cls = oldAnn.value();
            if(cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Class<?> findDeserializationContentType(Annotated am, JavaType baseContentType,
            String propName)
    {
        // Primary annotation, JsonDeserialize
        JsonDeserialize ann = am.getAnnotation(JsonDeserialize.class);
        if (ann != null) {
            Class<?> cls = ann.contentAs();
            if (cls != NoClass.class) {
                return cls;
            }
        }

        /* !!! 21-May-2009, tatu: JsonClass is deprecated; will need to
         *    drop support at a later point (for 2.0?)
         */
        JsonContentClass oldAnn = am.getAnnotation(JsonContentClass.class);
        if (oldAnn != null) {
            Class<?> cls = oldAnn.value();
            if(cls != NoClass.class) {
                return cls;
            }
        }
        return null;
    }

    /*
    /***************************************************
    /* Deserialization: Method annotations
    /***************************************************
     */

    @SuppressWarnings("deprecation")
    @Override
    public String findSettablePropertyName(AnnotatedMethod am)
    {
        /* 22-May-2009, tatu: JsonProperty is the primary annotation
         *   to check for
         */
        JsonProperty pann = am.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        /* 22-May-2009, tatu: JsonSetter is deprecated as of 1.1
         *    but still supported
         */
        JsonSetter ann = am.getAnnotation(JsonSetter.class);
        if (ann != null) {
            return ann.value();
        }
        /* 22-May-2009, tatu: And finally, JsonSerialize implies
         *   that there is a property, although doesn't define name
         */
        if (am.hasAnnotation(JsonDeserialize.class)) {
            return "";
        }
        return null;
    }

    @Override
    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return am.hasAnnotation(JsonAnySetter.class);
    }

    @Override
    public boolean hasCreatorAnnotation(Annotated a)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return a.hasAnnotation(JsonCreator.class);
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: field annotations
    ///////////////////////////////////////////////////////
    */

    @Override
    public String findDeserializablePropertyName(AnnotatedField af)
    {
        JsonProperty pann = af.getAnnotation(JsonProperty.class);
        if (pann != null) {
            return pann.value();
        }
        // Also: having JsonDeserialize implies it is such a property
        if (af.hasAnnotation(JsonDeserialize.class)) {
            return "";
        }
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Deserialization: parameters annotations
    ///////////////////////////////////////////////////////
    */

    @Override
        public String findPropertyNameForParam(AnnotatedParameter param)
    {
        if (param != null) {
            JsonProperty pann = param.getAnnotation(JsonProperty.class);
            if (pann != null) {
                return pann.value();
            }
            /* And can not use JsonDeserialize as we can not use
             * name auto-detection (names of local variables including
             * parameters are not necessarily preserved in bytecode)
             */
        }
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // Helper methods
    ////////////////////////////////////////////////////
     */

    protected boolean _isIgnorable(Annotated a)
    {
        JsonIgnore ann = a.getAnnotation(JsonIgnore.class);
        return (ann != null && ann.value());
    }
}
