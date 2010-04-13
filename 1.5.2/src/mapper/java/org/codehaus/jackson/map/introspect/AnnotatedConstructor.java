package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

public final class AnnotatedConstructor
    extends AnnotatedWithParams
{
    final Constructor<?> _constructor;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedConstructor(Constructor<?> constructor,
                                AnnotationMap classAnn, AnnotationMap[] paramAnn)
    {
        super(classAnn, paramAnn);
        if (constructor == null) {
            throw new IllegalArgumentException("Null constructor not allowed");
        }
        _constructor = constructor;
    }

    /*
    //////////////////////////////////////////////////////
    // Annotated impl
    //////////////////////////////////////////////////////
     */

    public Constructor<?> getAnnotated() { return _constructor; }

    public int getModifiers() { return _constructor.getModifiers(); }

    public String getName() { return _constructor.getName(); }

    public Type getGenericType() {
        return getRawType();
    }

    public Class<?> getRawType() {
        return _constructor.getDeclaringClass();
    }

    /*
    //////////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////////
     */

    public AnnotatedParameter getParameter(int index) {
        return new AnnotatedParameter(getParameterType(index), _paramAnnotations[index]);
    }

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

    /*
    //////////////////////////////////////////////////////
    // AnnotatedMember impl
    //////////////////////////////////////////////////////
     */

    public Class<?> getDeclaringClass() { return _constructor.getDeclaringClass(); }

    public Member getMember() { return _constructor; }

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

