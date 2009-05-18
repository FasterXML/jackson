package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;

import org.codehaus.jackson.annotate.*;
import org.codehaus.jackson.map.AnnotationIntrospector;

/**
 * {@link AnnotationIntrospector} implementation that handles standard
 * Jackson annotations.
 */
public class JacksonAnnotationIntrospector
    extends AnnotationIntrospector
{
    public JacksonAnnotationIntrospector() { }

    /*
    ////////////////////////////////////////////////////
    // General annotation properties
    ////////////////////////////////////////////////////
     */

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
    ////////////////////////////////////////////////////
    // Class annotations: general
    ////////////////////////////////////////////////////
     */

    public boolean isIgnorableMethod(AnnotatedMethod m)
    {
        JsonIgnore ann = m.getAnnotation(JsonIgnore.class);
        return (ann != null && ann.value());
    }

    /*
    ////////////////////////////////////////////////////
    // Class annotations: Serialization
    ////////////////////////////////////////////////////
     */

    public Boolean findGetterAutoDetection(AnnotatedClass ac)
    {
        JsonAutoDetect cann = ac.getAnnotation(JsonAutoDetect.class);
        if (cann != null) {
            JsonMethod[] methods = cann.value();
            if (methods != null) {
                for (JsonMethod jm : methods) {
                    if (jm.getterEnabled()) {
                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }
        return null;
    }

    /*
    ////////////////////////////////////////////////////
    // Class annotations: Deserialization
    ////////////////////////////////////////////////////
     */

    public Boolean findSetterAutoDetection(AnnotatedClass ac)
    {
        JsonAutoDetect cann = ac.getAnnotation(JsonAutoDetect.class);
        if (cann != null) {
            JsonMethod[] methods = cann.value();
            if (methods != null) {
                for (JsonMethod jm : methods) {
                    if (jm.setterEnabled()) {
                        return Boolean.TRUE;
                    }
                }
            }
            return Boolean.FALSE;
        }
        return null;
    }

    /*
    ///////////////////////////////////////////////////////
    // Method annotations: serialization
    ///////////////////////////////////////////////////////
    */

    public String findGettablePropertyName(AnnotatedMethod am)
    {
        JsonGetter ann = am.getAnnotation(JsonGetter.class);
        if (ann == null) {
            return null;
        }
        String propName = ann.value();
        // can it ever be null? I don't think so, but just in case:
        if (propName == null) {
            propName = "";
        }
        return propName;
    }

    public boolean hasAsValueAnnotation(AnnotatedMethod am)
    {
        JsonValue ann = am.getAnnotation(JsonValue.class);
        // value of 'false' means disabled...
        return (ann != null && ann.value());
    }

    /*
    ///////////////////////////////////////////////////////
    // Method annotations: deserialization
    ///////////////////////////////////////////////////////
    */

    public String findSettablePropertyName(AnnotatedMethod am)
    {
        JsonSetter ann = am.getAnnotation(JsonSetter.class);
        if (ann == null) {
            return null;
        }
        String propName = ann.value();
        // can it ever be null? I don't think so, but just in case:
        if (propName == null) {
            propName = "";
        }
        return propName;
    }

    public boolean hasAnySetterAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return am.hasAnnotation(JsonAnySetter.class);
    }

    public boolean hasCreatorAnnotation(AnnotatedMethod am)
    {
        /* No dedicated disabling; regular @JsonIgnore used
         * if needs to be ignored (and if so, is handled prior
         * to this method getting called)
         */
        return am.hasAnnotation(JsonCreator.class);
    }

    /*
    ////////////////////////////////////////////////////
    // Field annotations: general
    ////////////////////////////////////////////////////
     */

    public boolean isIgnorableField(AnnotatedField f)
    {
        JsonIgnore ann = f.getAnnotation(JsonIgnore.class);
        return (ann != null && ann.value());
    }
}
