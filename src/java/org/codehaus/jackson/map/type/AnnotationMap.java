package org.codehaus.jackson.map.type;

import java.lang.annotation.Annotation;
import java.util.*;

/**
 * Simple helper class used to keep track of collection of
 * Jackson Annotations associated with annotatable things
 * (methods, constructors, classes).
 * Note that only Jackson-owned annotations are tracked (for now?).
 */
public final class AnnotationMap
{
    /**
     * To recognize annotations that Jackson package defines, let's
     * just assume they'll all be under {@link org.codehaus.jackson}
     * package. This is only used as an optimization, to reduce number
     * of annotations we keep track of, so that we can ignore
     * annotations we don't care about.
     */
    final static String JACKSON_PKG_PREFIX = "org.codehaus.jackson";

    HashMap<Class<? extends Annotation>,Annotation> _annotations;

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
        if (_isJacksonAnnotation(ann)) {
            if (_annotations == null || !_annotations.containsKey(ann.annotationType())) {
                _add(ann);
            }
        }
    }

    /**
     * Method called to add specified annotation in the Map.
     */
    public void add(Annotation ann)
    {
        if (_isJacksonAnnotation(ann)) {
            _add(ann);
        }
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
    ///////////////////////////////////////////////////////
    // Helper methods
    ///////////////////////////////////////////////////////
     */

    protected boolean _isJacksonAnnotation(Annotation ann)
    {
        Class<? extends Annotation> acls = ann.annotationType();
        Package pkg = acls.getPackage();
        /* Let's be conservative and also include ones where we
         * don't know the package
         */
        return (pkg == null) || (pkg.getName().startsWith(JACKSON_PKG_PREFIX));
    }

    protected void _add(Annotation ann)
    {
        if (_annotations == null) {
            _annotations = new HashMap<Class<? extends Annotation>,Annotation>();
        }
        _annotations.put(ann.annotationType(), ann);
    }
}


