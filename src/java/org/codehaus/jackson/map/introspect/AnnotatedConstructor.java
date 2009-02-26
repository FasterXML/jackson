package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

public final class AnnotatedConstructor
	extends Annotated
{
    Constructor<?> _constructor;

    final AnnotationMap _annotations = new AnnotationMap();

    public AnnotatedConstructor(Constructor<?> constructor)
    {
        _constructor = constructor;
        // Also, let's find annotations we already have
        for (Annotation a : constructor.getDeclaredAnnotations()) {
            _annotations.add(a);
        }
    }

    public Constructor<?> getAnnotated() { return _constructor; }

    public <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
    }

    public Class<?>[] getParameterTypes() {
        return _constructor.getParameterTypes();
    }

    /**
     * Constructor called to add annotations that have not yet been
     * added to this instance/
     */
    public void addAnnotationsNotPresent(Constructor<?> constructor)
    {
        for (Annotation a : constructor.getDeclaredAnnotations()) {
            _annotations.addIfNotPresent(a);
        }
    }
}

