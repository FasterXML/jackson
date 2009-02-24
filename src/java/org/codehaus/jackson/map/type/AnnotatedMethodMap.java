package org.codehaus.jackson.map.type;

import java.lang.reflect.Method;
import java.util.*;

/**
 * Simple helper class used to keep track of collection of
 * {@link AnnotatedMethod}s, accessible by lookup. Lookup
 * is usually needed for augmenting and overriding annotations.
 */
public final class AnnotatedMethodMap
{
    LinkedHashMap<MethodKey,AnnotatedMethod> _methods;

    public AnnotatedMethodMap() { }

    /**
     * Method called to add specified annotation in the Map.
     */
    public void add(AnnotatedMethod am)
    {
        if (_methods == null) {
            _methods = new LinkedHashMap<MethodKey,AnnotatedMethod>();
        }
        _methods.put(new MethodKey(am.getAnnotated()), am);
    }

    public AnnotatedMethod find(Method m)
    {
        if (_methods == null) {
            return null;
        }
        return _methods.get(new MethodKey(m));
    }

    public Collection<AnnotatedMethod> getMethods()
    {
        if (_methods != null) {
            return _methods.values();
        }
        return Collections.emptyList();
    }
}
