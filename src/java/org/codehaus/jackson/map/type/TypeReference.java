package org.codehaus.jackson.map.type;

import java.lang.reflect.Type;

/**
 * This class is based on ideas from
 * [http://gafter.blogspot.com/2006/12/super-type-tokens.html].
 */
public abstract class TypeReference<T>
/* And to further make things more robust, we'll use the suggestion
 * from comments, of ensuring that a Type argument is indeed given
 */
    implements Comparable<TypeReference<T>>
{
    protected TypeReference() { }

    public Type getType() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class) { // sanity check, should never happen
            throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
        }
        return superClass;
    }

    /**
     * The only reason we define this method (and require implementation
     * of <code>Comparable</code>) is to prevent constructing a
     * reference without type information.
     */
    public int compareTo(TypeReference<T> o) {
        // just need an implemented, not a good one... hence:
        return 0;
    }
}

