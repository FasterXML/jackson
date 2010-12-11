package org.codehaus.jackson.map.introspect;

import java.lang.annotation.Annotation;
import java.util.*;

import org.codehaus.jackson.map.util.Annotations;

/**
 * Simple helper class used to keep track of collection of
 * Jackson Annotations associated with annotatable things
 * (methods, constructors, classes).
 * Note that only Jackson-owned annotations are tracked (for now?).
 */
public final class AnnotationMap implements Annotations
{
    protected HashMap<Class<? extends Annotation>,Annotation> _annotations;

    public AnnotationMap() { }

    @SuppressWarnings("unchecked")
    public <A extends Annotation> A get(Class<A> cls)
    {
        if (_annotations == null) {
            return null;
        }
        return (A) _annotations.get(cls);
    }

    public int size() {
        return (_annotations == null) ? 0 : _annotations.size();
    }

    /**
     * Method called to add specified annotation in the Map, but
     * only if it didn't yet exist.
     */
    public void addIfNotPresent(Annotation ann)
    {
        if (_annotations == null || !_annotations.containsKey(ann.annotationType())) {
            _add(ann);
        }
    }

    /**
     * Method called to add specified annotation in the Map.
     */
    public void add(Annotation ann) {
        _add(ann);
    }

    @Override
    public String toString()
    {
        if (_annotations == null) {
            return "[null]";
        }
        return _annotations.toString();
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected final void _add(Annotation ann)
    {
        if (_annotations == null) {
            _annotations = new HashMap<Class<? extends Annotation>,Annotation>();
        }
        _annotations.put(ann.annotationType(), ann);
    }
}


