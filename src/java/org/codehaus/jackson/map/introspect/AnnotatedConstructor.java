package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedConstructor
	extends Annotated
{
    final AnnotationIntrospector _annotationIntrospector;

    final Constructor<?> _constructor;

    final AnnotationMap _annotations = new AnnotationMap();

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedConstructor(Constructor<?> constructor, AnnotationIntrospector intr)
    {
        _constructor = constructor;
        _annotationIntrospector = intr;
        // Also, let's find annotations we already have
        for (Annotation a : constructor.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                _annotations.add(a);
            }
        }
    }

    /**
     * Constructor called to add annotations that have not yet been
     * added to this instance/
     */
    public void addAnnotationsNotPresent(Constructor<?> constructor)
    {
        for (Annotation a : constructor.getDeclaredAnnotations()) {
            if (_annotationIntrospector.isHandled(a)) {
                _annotations.addIfNotPresent(a);
            }
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

    /**
     * Method that can be called to modify access rights, by calling
     * {@link java.lang.reflect.AccessibleObject#setAccessible} on
     * the underlying annotated element.
     */
    public void fixAccess()
    {
        ClassUtil.checkAndFixAccess(_constructor);
    }
}

