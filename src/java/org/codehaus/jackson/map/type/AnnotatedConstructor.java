package org.codehaus.jackson.map.type;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

public final class AnnotatedConstructor
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

    public Annotation getAnnotation(Class<Annotation> acls)
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

