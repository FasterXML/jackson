package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public final class AnnotatedMethod
	extends Annotated
{
    Method _method;

    final AnnotationMap _annotations = new AnnotationMap();

    public AnnotatedMethod(Method method)
    {
        _method = method;
        // Also, let's find annotations we already have
        for (Annotation a : method.getDeclaredAnnotations()) {
            _annotations.add(a);
        }
    }

    public String getName() { return _method.getName(); }

    public Method getAnnotated() { return _method; }

    public <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
    }
    
    public Class<?>[] getParameterTypes() { return _method.getParameterTypes(); }
    public Class<?> getReturnType() { return _method.getReturnType(); }

    public <A extends Annotation> boolean hasAnnotation(Class<A> acls)
    {
        return _annotations.get(acls) != null;
    }

    public int getAnnotationCount() { return _annotations.size(); }

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

    @Override
        public String toString()
    {
        return "[method "+getName()+", annotations: "+_annotations+"]";
    }
}

