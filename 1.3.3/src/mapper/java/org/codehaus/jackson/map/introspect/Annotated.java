package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;

/**
 * Shared base class used for anything on which annotations (included
 * within a {@link AnnotationMap}).
 */
public abstract class Annotated
{
    public abstract <A extends Annotation> A getAnnotation(Class<A> acls);

    public final <A extends Annotation> boolean hasAnnotation(Class<A> acls)
    {
        return getAnnotation(acls) != null;
    }

    public abstract AnnotatedElement getAnnotated();

    protected abstract int getModifiers();

    public final boolean isPublic() {
        return Modifier.isPublic(getModifiers());
    }

    public abstract String getName();

    /**
     * Method used with annotated things that have class type.
     */
    public abstract Class<?> getType();
}
