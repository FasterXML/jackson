package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.map.type.TypeFactory;

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

    public JavaType getType() {
        return TypeFactory.type(getGenericType());
    }

    /**
     * @since 1.5
     */
    public abstract Type getGenericType();

    /**
     * @since 1.5
     */
    public abstract Class<?> getRawType();

}

