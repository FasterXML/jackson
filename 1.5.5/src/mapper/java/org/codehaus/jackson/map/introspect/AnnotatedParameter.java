package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * Object that represents method parameters, mostly so that associated
 * annotations can be processed conveniently.
 */
public final class AnnotatedParameter
    extends Annotated
{
    final Type _type;

    final AnnotationMap _annotations;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedParameter(Type type,  AnnotationMap ann)
    {
        _type = type;
        _annotations = ann;
    }

    public void addOrOverride(Annotation a)
    {
        _annotations.add(a);
    }

    /*
    //////////////////////////////////////////////////////
    // Annotated impl
    //////////////////////////////////////////////////////
     */

    /// Unfortunately, there is no matching JDK type...
    public AnnotatedElement getAnnotated() { return null; }

    /// Unfortunately, there is no matching JDK type...
    public int getModifiers() { return 0; }

    public String getName() { return ""; }

    public <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
    }

    public Type getGenericType() {
        /* Hmmh. Could figure out real type (require it to be passed).
         * But for now, let's assume we don't really need this method.
         */
        return getRawType();
    }

    public Class<?> getRawType() {
        // should never be called
        throw new IllegalStateException();
    }
    
    /*
    //////////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////////
     */

    public Type getParameterType() { return _type; }
}

