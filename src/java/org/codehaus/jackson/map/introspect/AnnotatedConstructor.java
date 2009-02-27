package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedConstructor
	extends Annotated
{
    Constructor<?> _constructor;

    final AnnotationMap _annotations = new AnnotationMap();

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedConstructor(Constructor<?> constructor)
    {
        _constructor = constructor;
        // Also, let's find annotations we already have
        for (Annotation a : constructor.getDeclaredAnnotations()) {
            _annotations.add(a);
        }
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

    /*
    //////////////////////////////////////////////////////
    // Annotated impl
    //////////////////////////////////////////////////////
     */

    public Constructor<?> getAnnotated() { return _constructor; }

    public int getModifiers() { return _constructor.getModifiers(); }

    public String getName() { return _constructor.getName(); }

    public <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
    }

    /*
    //////////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////////
     */

    public Class<?>[] getParameterTypes() {
        return _constructor.getParameterTypes();
    }

    public void fixAccess()
    {
        ClassUtil.checkAndFixAccess(_constructor);
    }
}

