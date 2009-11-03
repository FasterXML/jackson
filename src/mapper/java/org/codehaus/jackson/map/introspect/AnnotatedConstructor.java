package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import org.codehaus.jackson.map.util.ClassUtil;

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

    public Class<?> getType() {
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
        return "[constructor for "+getName()+", annotations: "+_classAnnotations+"]";
    }
}

