package org.codehaus.jackson.map.introspect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Type;

import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.type.JavaType;

public final class AnnotatedConstructor
    extends AnnotatedWithParams
{
    protected final Constructor<?> _constructor;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
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
    /**********************************************************
    /* Annotated impl
    /**********************************************************
     */

    @Override
    public Constructor<?> getAnnotated() { return _constructor; }

    @Override
    public int getModifiers() { return _constructor.getModifiers(); }

    @Override
    public String getName() { return _constructor.getName(); }

    @Override
    public Type getGenericType() {
        return getRawType();
    }

    @Override
    public Class<?> getRawType() {
        return _constructor.getDeclaringClass();
    }

    // note: copied verbatim from AnnotatedMethod; hard to generalize
    /**
     * As per [JACKSON-468], we need to also allow declaration of local
     * type bindings; mostly it will allow defining bounds.
     */
    @Override
    public JavaType getType(TypeBindings bindings)
    {
        return getType(bindings, _constructor.getTypeParameters());
    }
    
    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
     */

    @Override
    public AnnotatedParameter getParameter(int index) {
        return new AnnotatedParameter(this, getParameterType(index), _paramAnnotations[index]);
    }

    @Override
    public int getParameterCount() {
        return _constructor.getParameterTypes().length;
    }

    @Override
    public Class<?> getParameterClass(int index)
    {
        Class<?>[] types = _constructor.getParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    @Override
    public Type getParameterType(int index)
    {
        Type[] types = _constructor.getGenericParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    /*
    /**********************************************************
    /* AnnotatedMember impl
    /**********************************************************
     */

    @Override
    public Class<?> getDeclaringClass() { return _constructor.getDeclaringClass(); }

    @Override
    public Member getMember() { return _constructor; }

    /*
    /**********************************************************
    /* Extended API, specific annotations
    /**********************************************************
     */

    @Override
    public String toString() {
        return "[constructor for "+getName()+", annotations: "+_annotations+"]";
    }
}

