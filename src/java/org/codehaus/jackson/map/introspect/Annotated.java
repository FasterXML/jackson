package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;

/**
 * Shared base class used for anything on which annotations (included
 * within a {@link AnnotationMap}).
 */
public abstract class Annotated
{
    public abstract AnnotatedElement getAnnotated();

    public abstract <A extends Annotation> A getAnnotation(Class<A> acls);
}
