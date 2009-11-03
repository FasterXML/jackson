package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedMethod
    extends AnnotatedWithParams
{
    final Method _method;

    // // Simple lazy-caching:

    public Class<?>[] _paramTypes;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedMethod(Method method,
                           AnnotationMap classAnn, AnnotationMap[] paramAnn)
    {
        super(classAnn, paramAnn);
        _method = method;
    }

    /*
    //////////////////////////////////////////////////////
    // Annotated impl
    //////////////////////////////////////////////////////
     */

    public Method getAnnotated() { return _method; }

    public int getModifiers() { return _method.getModifiers(); }

    public String getName() { return _method.getName(); }


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

    public AnnotatedParameter getParameter(int index) {
        return new AnnotatedParameter(getParameterType(index), _paramAnnotations[index]);
    }

    public int getParameterCount() {
        return getParameterTypes().length;
    }

    public Type[] getParameterTypes() {
        return _method.getGenericParameterTypes();
    }

    public Class<?> getParameterClass(int index)
    {
        Class<?>[] types = _method.getParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    public Type getParameterType(int index)
    {
        Type[] types = _method.getGenericParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    public Class<?>[] getParameterClasses()
    {
        if (_paramTypes == null) {
            _paramTypes = _method.getParameterTypes();
        }
        return _paramTypes;
    }

    public Type getGenericReturnType() { return _method.getGenericReturnType(); }

    public Class<?> getReturnType() { return _method.getReturnType(); }

    public Class<?> getDeclaringClass() { return _method.getDeclaringClass(); }

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName() + "("
            +getParameterCount()+" params)";
    }

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
        return "[method "+getName()+", annotations: "+_classAnnotations+"]";
    }
}

