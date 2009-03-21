package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;

import org.codehaus.jackson.annotate.JsonWriteNullProperties;
import org.codehaus.jackson.map.util.ClassUtil;

public final class AnnotatedMethod
	extends Annotated
{
    Method _method;

    final AnnotationMap _annotations = new AnnotationMap();

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    public AnnotatedMethod(Method method)
    {
        _method = method;
        // Also, let's find annotations we already have
        for (Annotation a : method.getDeclaredAnnotations()) {
            _annotations.add(a);
        }
    }

    /**
     * Method called to add annotations that have not yet been
     * added to this instance/
     */
    public void addAnnotationsNotPresent(Method method)
    {
        for (Annotation a : method.getDeclaredAnnotations()) {
            _annotations.addIfNotPresent(a);
        }
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
    
    /*
    //////////////////////////////////////////////////////
    // Extended API, generic
    //////////////////////////////////////////////////////
     */

    public Type[] getGenericParameterTypes() { return _method.getGenericParameterTypes(); }
    public Class<?>[] getParameterTypes() { return _method.getParameterTypes(); }
    public Class<?> getReturnType() { return _method.getReturnType(); }

    public Class<?> getDeclaringClass() { return _method.getDeclaringClass(); }

    public String getFullName() {
        return getDeclaringClass().getName() + "#" + getName();
    }

    public <A extends Annotation> boolean hasAnnotation(Class<A> acls)
    {
        return _annotations.get(acls) != null;
    }

    public int getAnnotationCount() { return _annotations.size(); }

    public void fixAccess()
    {
        ClassUtil.checkAndFixAccess(_method);
    }

    /*
    //////////////////////////////////////////////////////
    // Extended API, specific annotations
    //////////////////////////////////////////////////////
     */

   public boolean willWriteNullProperties(boolean defValue)
    {
        JsonWriteNullProperties ann = getAnnotation(JsonWriteNullProperties.class);
        return (ann == null) ? defValue : ann.value();
    }

    public String toString()
    {
        return "[method "+getName()+", annotations: "+_annotations+"]";
    }
}

