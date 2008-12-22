package org.codehaus.jackson.map.type;

import java.util.*;
import java.lang.reflect.*;

/**
 * Class that knows how construct {@link JavaType} instances,
 * given various inputs.
 */
public class TypeFactory
{
    public final static TypeFactory instance = new TypeFactory();

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    public TypeFactory()
    {
    }

    /*
    //////////////////////////////////////////////////
    // Public factory methods
    //////////////////////////////////////////////////
     */

    /**
     * Factory method that can be used if only type information
     * available is of type {@link Class}. This means that there
     * will not be generic type information due to type erasure,
     * but at least it will be possible to recognize array
     * types and non-typed container types.
     * And for other types (primitives/wrappers, beans), this
     * is all that is needed.
     */
    public JavaType fromClass(Class<?> clz)
    {
        // First: is it an array?
        if (clz.isArray()) {
            return new ArrayType(clz, fromClass(clz.getComponentType()));
        }
        /* Maps and Collections aren't quite as hot; problem is, due
         * to type erasure we can't know typing and can only assume
         * base Object...
         */
        // !!! TBI

        return SimpleType.construct(clz);
    }
}
