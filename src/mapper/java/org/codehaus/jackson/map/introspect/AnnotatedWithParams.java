package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import org.codehaus.jackson.map.type.TypeBindings;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

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
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    protected AnnotatedWithParams(AnnotationMap classAnn, AnnotationMap[] paramAnnotations)
    {
        _annotations = classAnn;
        _paramAnnotations = paramAnnotations;
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
    /**********************************************************
    /* Helper methods for subclasses
    /**********************************************************
     */

    protected  JavaType getType(TypeBindings bindings, TypeVariable<?>[] typeParams)
    {
        // [JACKSON-468] Need to consider local type binding declarations too...
        if (typeParams != null && typeParams.length > 0) {
            bindings = bindings.childInstance();
            for (TypeVariable<?> var : typeParams) {
                String name = var.getName();
                // to prevent infinite loops, need to first add placeholder ("<T extends Enum<T>>" etc)
                bindings._addPlaceholder(name);
                // About only useful piece of information is the lower bound (which is at least Object.class)
                Type lowerBound = var.getBounds()[0];
                JavaType type = (lowerBound == null) ? TypeFactory.unknownType()
                        : bindings.resolveType(lowerBound);
                bindings.addBinding(var.getName(), type);
            }
        }
        return bindings.resolveType(getGenericType());
    }

    /*
    /**********************************************************
    /* Partial Annotated impl
    /**********************************************************
     */

    @Override
    public final <A extends Annotation> A getAnnotation(Class<A> acls)
    {
        return _annotations.get(acls);
    }

    /*
    /**********************************************************
    /* Extended API
    /**********************************************************
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

    /**
     * Method called to fully resolve type of one of parameters, given
     * specified type variable bindings.
     * 
     * @since 1.8
     */
    public final JavaType resolveParameterType(int index, TypeBindings bindings) {
        return bindings.resolveType(getParameterType(index));
    }
    
    public final int getAnnotationCount() { return _annotations.size(); }
}
