package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedConstructor
	extends Annotated
{
    final Constructor<?> _constructor;

    final AnnotationMap _annotations;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedConstructor(Constructor<?> constructor, AnnotationMap annMap)
    {
        _constructor = constructor;
        _annotations = annMap;
    }

    /**
     * Method called to override an annotation, usually due to a mix-in
     * annotation masking or overriding an annotation 'real' constructor
     * has.
     */
    public void addOrOverride(Annotation a)
    {
        _annotations.add(a);
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

    public Class<?> getType() {
        return _constructor.getDeclaringClass();
    }

    /*
    //////////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////////
     */

    public int getParameterCount() {
        return _constructor.getParameterTypes().length;
    }

    public Class<?> getParameterClass(int index)
    {
        Class<?>[] types = _constructor.getParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    public Type getParameterType(int index)
    {
        Type[] types = _constructor.getGenericParameterTypes();
        return (index >= types.length) ? null : types[index];
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

    /*
    //////////////////////////////////////////////////////
    // Extended API, specific annotations
    //////////////////////////////////////////////////////
     */

    public String toString()
    {
        return "[constructor for "+getName()+", annotations: "+_annotations+"]";
    }
}

