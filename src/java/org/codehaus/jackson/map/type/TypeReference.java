package org.codehaus.jackson.map.type;

import java.lang.reflect.ParameterizedType;
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
    final Type _type;

    protected TypeReference()
    {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof Class) { // sanity check, should never happen
            throw new IllegalArgumentException("Internal error: TypeReference constructed without actual type information");
        }
        /* 22-Dec-2008, tatu: Not sure if this case is safe -- I suspect
         *   it is possible to make it fail?
         *   But let's deal with specifc
         *   case when we know an actual use case, and thereby suitable
         *   work arounds for valid case(s) and/or error to throw
         *   on invalid one(s).
         */
        _type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
    }

    public Type getType() { return _type; }

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

