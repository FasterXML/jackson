package org.codehaus.jackson.map.introspect;

import java.lang.reflect.*;

import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.type.JavaType;

public final class AnnotatedMethod
    extends AnnotatedWithParams
{
    final protected Method _method;

    // // Simple lazy-caching:

    protected Class<?>[] _paramTypes;

    /*
    /*****************************************************
    /* Life-cycle
    /*****************************************************
     */

    public AnnotatedMethod(Method method, AnnotationMap classAnn, AnnotationMap[] paramAnnotations)
    {
        super(classAnn, paramAnnotations);
        _method = method;
    }

    /**
     * Method that constructs a new instance with settings (annotations, parameter annotations)
     * of this instance, but with different physical {@link Method}.
     * 
     * @since 1.7
     */
    public AnnotatedMethod withMethod(Method m)
    {
        return new AnnotatedMethod(m, _annotations, _paramAnnotations);
    }
    
    /*
    /*****************************************************
    /* Annotated impl
    /*****************************************************
     */

    @Override
    public Method getAnnotated() { return _method; }

    @Override
    public int getModifiers() { return _method.getModifiers(); }

    @Override
    public String getName() { return _method.getName(); }

    /**
     * For methods, this returns declared return type, which is only
     * useful with getters (setters do not return anything; hence "void"
     * type is returned here)
     */
    @Override
    public Type getGenericType() {
        return _method.getGenericReturnType();
    }

    /**
     * For methods, this returns declared return type, which is only
     * useful with getters (setters do not return anything; hence "void"
     * type is returned here)
     */
    @Override
    public Class<?> getRawType() {
        return _method.getReturnType();
    }

    /**
     * As per [JACKSON-468], we need to also allow declaration of local
     * type bindings; mostly it will allow defining bounds.
     */
    @Override
    public JavaType getType(TypeBindings bindings)
    {
        return getType(bindings, _method.getTypeParameters());
    }
    
    /*
    /********************************************************
    /* AnnotatedMember impl
    /********************************************************
     */

    @Override
    public Class<?> getDeclaringClass() { return _method.getDeclaringClass(); }

    @Override
    public Member getMember() { return _method; }
    
    /*
    /*****************************************************
    /* Extended API, generic
    /*****************************************************
     */

    @Override
    public AnnotatedParameter getParameter(int index) {
        return new AnnotatedParameter(this, getParameterType(index), _paramAnnotations[index]);
    }

    @Override
    public int getParameterCount() {
        return getParameterTypes().length;
    }

    public Type[] getParameterTypes() {
        return _method.getGenericParameterTypes();
    }

    @Override
    public Class<?> getParameterClass(int index)
    {
        Class<?>[] types = _method.getParameterTypes();
        return (index >= types.length) ? null : types[index];
    }

    @Override
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

    //public Type getGenericReturnType() { return _method.getGenericReturnType(); }

    //public Class<?> getReturnType() { return _method.getReturnType(); }

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName() + "("
            +getParameterCount()+" params)";
    }

    /*
    /********************************************************
    /* Extended API, specific annotations
    /********************************************************
     */

    @Override
    public String toString()
    {
        return "[method "+getName()+", annotations: "+_annotations+"]";
    }
}
