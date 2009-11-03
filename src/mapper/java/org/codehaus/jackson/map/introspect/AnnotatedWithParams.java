package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;

import org.codehaus.jackson.map.util.ClassUtil;

/**
 * Intermediate base class that encapsulates features that
 * constructors and methods share.
 */
public abstract class AnnotatedWithParams
    extends Annotated
{
    protected final AnnotationMap _classAnnotations;

    protected final AnnotationMap[] _paramAnnotations;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    protected AnnotatedWithParams(AnnotationMap classAnn, AnnotationMap[] paramAnn)
    {
        _classAnnotations = classAnn;
        _paramAnnotations = paramAnn;
    }

    /**
     * Method called to override a class annotation, usually due to a mix-in
     * annotation masking or overriding an annotation 'real' class
     */
    public final void addOrOverride(Annotation a)
    {
        _classAnnotations.add(a);
    }

    /**
     * Method called to override a method parameter annotation,
     * usually due to a mix-in
     * annotation masking or overriding an annotation 'real' method
     * has.
     */
    public final void addOrOverrideParam(int paramIndex, Annotation a)
    {
        AnnotationMap old = _paramAnnotations[paramIndex];
        if (old == null) {
            old = new AnnotationMap();
            _paramAnnotations[paramIndex] = old;
        }
        old.add(a);
    }

    /**
     * Method called to augment annotations, by adding specified
     * annotation if and only if it is not yet present in the
     * annotation map we have.
     */
    public final void addIfNotPresent(Annotation a)
    {
        _classAnnotations.addIfNotPresent(a);
    }

    /*
    //////////////////////////////////////////////////////
    // Partial Annotated impl
    //////////////////////////////////////////////////////
     */

    public final <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _classAnnotations.get(acls);
    }

    /*
    //////////////////////////////////////////////////////
    // Extended API
    //////////////////////////////////////////////////////
     */

    public final AnnotationMap getParameterAnnotations(int index)
    {
        if (_paramAnnotations != null) {
            if (index >= 0 && index <= _paramAnnotations.length) {
                return _paramAnnotations[index];
            }
        }
        return null;
    }

    public abstract AnnotatedParameter getParameter(int index);

    public abstract int getParameterCount();

    public abstract Class<?> getParameterClass(int index);

    public abstract Type getParameterType(int index);

    public final int getAnnotationCount() { return _classAnnotations.size(); }
}
