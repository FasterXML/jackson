package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Intermediate base class that encapsulates features that
 * constructors and methods share.
 */
public abstract class AnnotatedWithParams
    extends AnnotatedMember
{
    /**
     * Annotations directly associated with the annotated
     * entity.
     */
    protected final AnnotationMap _annotations;

    /**
     * Annotations associated with parameters of the annotated
     * entity (method or constructor parameters)
     */
    protected final AnnotationMap[] _paramAnnotations;

    /*
    //////////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////////
     */

    protected AnnotatedWithParams(AnnotationMap classAnn, AnnotationMap[] paramAnn)
    {
        _annotations = classAnn;
        _paramAnnotations = paramAnn;
    }

    /**
     * Method called to override a class annotation, usually due to a mix-in
     * annotation masking or overriding an annotation 'real' class
     */
    public final void addOrOverride(Annotation a)
    {
        _annotations.add(a);
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
        _annotations.addIfNotPresent(a);
    }

    /*
    //////////////////////////////////////////////////////
    // Partial Annotated impl
    //////////////////////////////////////////////////////
     */

    public final <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
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

    public final int getAnnotationCount() { return _annotations.size(); }
}
