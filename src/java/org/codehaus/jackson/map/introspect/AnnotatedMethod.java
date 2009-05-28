package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedMethod
    extends Annotated
{
    final Method _method;

    final AnnotationMap _annotations;

    // // Simple lazy-caching:

    public Class<?>[] _paramTypes;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedMethod(Method method, AnnotationMap annMap)
    {
        _method = method;
        _annotations = annMap;
    }

    /**
     * Method called to augment annotations, by adding specified
     * annotation if and only if it is not yet present in the
     * annotation map we have.
     */
    public void addIfNotPresent(Annotation a)
    {
        _annotations.addIfNotPresent(a);
    }

    /*
    //////////////////////////////////////////////////////
    // Annotated impl
    //////////////////////////////////////////////////////
     */

    public Method getAnnotated() { return _method; }

    public int getModifiers() { return _method.getModifiers(); }

    public String getName() { return _method.getName(); }

    public <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
    }

    /**
     * For methods, this returns declared return type, which is only
     * useful with getters (setters do not return anything; hence "void"
     * type is returned here)
     */
    public Class<?> getType() {
        return getReturnType();
    }

    /*
    //////////////////////////////////////////////////////
    // Extended API, generic
    //////////////////////////////////////////////////////
     */

    public Type[] getGenericParameterTypes() {
        return _method.getGenericParameterTypes();
    }

    public Class<?>[] getParameterTypes()
    {
        if (_paramTypes == null) {
            _paramTypes = _method.getParameterTypes();
        }
        return _paramTypes;
    }

    public int getParameterCount() {
        return getParameterTypes().length;
    }

    public Type getGenericReturnType() { return _method.getGenericReturnType(); }

    public Class<?> getReturnType() { return _method.getReturnType(); }

    public Class<?> getDeclaringClass() { return _method.getDeclaringClass(); }

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName() + "("
            +getParameterCount()+" params)";
    }

    public int getAnnotationCount() { return _annotations.size(); }

    /**
     * Method that can be called to modify access rights, by calling
     * {@link java.lang.reflect.AccessibleObject#setAccessible} on
     * the underlying annotated element.
     */
    public void fixAccess()
    {
        ClassUtil.checkAndFixAccess(_method);
    }

    /*
    //////////////////////////////////////////////////////
    // Extended API, specific annotations
    //////////////////////////////////////////////////////
     */

    public String toString()
    {
        return "[method "+getName()+", annotations: "+_annotations+"]";
    }
}

