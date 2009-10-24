package org.codehaus.jackson.map.type;

import java.util.*;
import java.lang.reflect.*;

import org.codehaus.jackson.type.JavaType;
import org.codehaus.jackson.type.TypeReference;

/**
 * Class used for constracting concrete {@link JavaType} instances,
 * given various inputs.
 *<p>
 * Typical usage patterns is to statically import factory methods
 * of this class, to allow convenient instantiation of structured
 * types, especially {@link Collection} and {@link Map} types
 * to represent generic types. For example
 *<pre>
 * mapType(Integer.class)
 *</pre>
 * to represent
 *<pre>
 *  Map&lt;String,Integer>
 *</pre>
 * This is an alternative to using {@link TypeReference} that would
 * be something like
 *<pre>
 *  new TypeReference&lt;Map&lt;String,Integer>>() { }
 *</pre>
 */
public class TypeFactory
{
    public final static TypeFactory instance = new TypeFactory();

    /*
    //////////////////////////////////////////////////
    // Life-cycle
    //////////////////////////////////////////////////
     */

    private TypeFactory() { }

    /*
    //////////////////////////////////////////////////
    // Public factory methods
    //////////////////////////////////////////////////
     */

    /**
     * Factory method for constructing {@link JavaType} from given
     * "raw" type; which may be anything from simple {@link Class}
     * to full generic type.
     *
     * @since 1.3
     */
    public static JavaType type(Type t)
    {
        return instance._fromType(t, null);
    }

    /**
     * Factory method that can use given context to resolve
     * named generic types.
     *
     * @param context Type context that can be used for binding
     *   named formal type parameters, if any (if null, no context
     *   is used)
     */
    public static JavaType type(Type type, JavaType context)
    {
        return instance._fromType(type, context);
    }

    /**
     * Factory method that can be used if the full generic type has
     * been passed using {@link TypeReference}. This only needs to be
     * done if the root type to bind to is generic; but if so,
     * it must be done to get proper typing.
     */
    public static JavaType type(TypeReference<?> ref)
    {
        return type(ref.getType());
    }
    
    /**
     * Convenience factory method for constructing {@link JavaType} that
     * represent array that contains elements
     * of specified type.
     *
     * @since 1.3
     */
    public static JavaType arrayType(Class<?> elementType)
    {
        return arrayType(type(elementType));
    }

    /**
     * Convenience factory method for constructing {@link JavaType} that
     * represent array that contains elements
     * of specified type.
     * 
     * @since 1.3
     */
    public static JavaType arrayType(JavaType elementType)
    {
        return ArrayType.construct(elementType);
    }

    /**
     * Convenience factory method for constructing {@link JavaType} that
     * represent Collection of specified type and contains elements
     * of specified type
     *
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public static JavaType collectionType(Class<? extends Collection> collectionType, Class<?> elementType)
    {
        return collectionType(collectionType, type(elementType));
    }
    
    /**
     * Convenience factory method for constructing {@link JavaType} that
     * represent Collection of specified type and contains elements
     * of specified type
     *
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public static JavaType collectionType(Class<? extends Collection> collectionType, JavaType elementType)
    {
        return CollectionType.construct(collectionType, elementType);
    }

    /**
     * Convenience factory method for constructing {@link JavaType} that
     * represent Map of specified type and contains elements
     * of specified type
     *
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public static JavaType mapType(Class<? extends Map> mapType, Class<?> keyType, Class<?> valueType)
    {
        return mapType(mapType, type(keyType), type(valueType));
    }

    /**
     * Convenience factory method for constructing {@link JavaType} that
     * represent Map of specified type and contains elements
     * of specified type
     *
     * @since 1.3
     */
    @SuppressWarnings("unchecked")
    public static JavaType mapType(Class<? extends Map> mapType, JavaType keyType, JavaType valueType)
    {
        return MapType.construct(mapType, keyType, valueType);
    }

    /*
    ///////////////////////////////////////////////////////
    // Legacy methods
    ///////////////////////////////////////////////////////
     */

    /**
     * Factory method that can be used if only type information
     * available is of type {@link Class}. This means that there
     * will not be generic type information due to type erasure,
     * but at least it will be possible to recognize array
     * types and non-typed container types.
     * And for other types (primitives/wrappers, beans), this
     * is all that is needed.
     *
     * @deprecated Use {@link #type(Type)} instead
     */
    public static JavaType fromClass(Class<?> clz)
    {
        return instance._fromClass(clz, null);
    }

    /**
     * Factory method that can be used if the full generic type has
     * been passed using {@link TypeReference}. This only needs to be
     * done if the root type to bind to is generic; but if so,
     * it must be done to get proper typing.
     *
     * @deprecated Use {@link #type(Type)} instead
     */
    public static JavaType fromTypeReference(TypeReference<?> ref)
    {
        return type(ref.getType());
    }

    /**
     * Factory method that can be used if type information is passed
     * as Java typing returned from <code>getGenericXxx</code> methods
     * (usually for a return or argument type).
     *
     * @deprecated Use {@link #type(Type)} instead
     */
    public static JavaType fromType(Type type)
    {
        return instance._fromType(type, null);
    }

    /**
     * @param context Type context that can be used for binding
     *   named formal type parameters, if any (if null, no context
     *   is used)
     *
     * @deprecated Use {@link #type(Type,JavaType)} instead
     */
    public static JavaType fromType(Type type, JavaType context)
    {
        return instance._fromType(type, context);
    }

    /*
    ///////////////////////////////////////////////////////
    // Internal methods
    ///////////////////////////////////////////////////////
     */

    /**
     * @param genericParams Mapping of formal parameter declarations (for generic
     *   types) into actual types
     */
    protected JavaType _fromClass(Class<?> clz, Map<String,JavaType> genericParams)
    {
        // First: do we have an array type?
        if (clz.isArray()) {
            return ArrayType.construct(_fromType(clz.getComponentType(), null));
        }
        /* Maps and Collections aren't quite as hot; problem is, due
         * to type erasure we often do not know typing and can only assume
         * base Object.
         */
        if (Map.class.isAssignableFrom(clz)) {
            MapType parentType = _findParentType(clz, MapType.class);
            if (parentType == null) {                
                JavaType unknown = _unknownType();
                return MapType.construct(clz, unknown, unknown);
            }
            return MapType.construct(clz, parentType.getKeyType(), parentType.getContentType());
        }
        if (Collection.class.isAssignableFrom(clz)) {
            CollectionType parentType = _findParentType(clz, CollectionType.class);
            return CollectionType.construct(clz, (parentType == null) ? _unknownType() : parentType.getContentType());
        }
        /* Otherwise, consider it a Bean; and due to type
         * erasure it must be simple (no generics available)
         */
        return new SimpleType(clz, genericParams);
    }

    /**
     * Factory method that can be used if type information is passed
     * as Java typing returned from <code>getGenericXxx</code> methods
     * (usually for a return or argument type).
     */
    public JavaType _fromType(Type type, JavaType context)
    {
        // may still be a simple type...
        if (type instanceof Class) {
            return _fromClass((Class<?>) type, null);
        }
        // But if not, need to start resolving.
        if (type instanceof ParameterizedType) {
            return _fromParamType((ParameterizedType) type, context);
        }
        if (type instanceof GenericArrayType) {
            return _fromArrayType((GenericArrayType) type, context);
        }
        if (type instanceof TypeVariable) {
            return _fromVariable((TypeVariable<?>) type, context);
        }
        if (type instanceof WildcardType) {
            return _fromWildcard((WildcardType) type, context);
        }
        // sanity check
        throw new IllegalArgumentException("Unrecognized Type: "+type.toString());
    }

    /**
     * This method deals with parameterized types, that is,
     * first class generic classes.
     *<p>
     * Since version 1.2, this resolves all generics types, not just
     * Maps or Collections.
     */
    protected JavaType _fromParamType(ParameterizedType type, JavaType context)
    {
        /* First: what is the actual base type? One odd thing
         * is that 'getRawType' returns Type, not Class<?> as
         * one might expect. But let's assume it is always of
         * type Class: if not, need to add more code to resolve
         * it to Class.
         */
        Class<?> rawType = (Class<?>) type.getRawType();

        // Ok: Map or Collection?
        if (Map.class.isAssignableFrom(rawType)) {
            Type[] args = type.getActualTypeArguments();
            return MapType.construct(rawType, fromType(args[0]), fromType(args[1]));
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            return CollectionType.construct(rawType, fromType(type.getActualTypeArguments()[0]));
        }

        // Maybe a generics version?
        Type[] args = type.getActualTypeArguments();
        Map<String,JavaType> types = null;

        if (args != null && args.length > 0) {
            /* If so, need mapping from name to type, to allow resolving
             * of generic types
             */
            TypeVariable<?>[] vars;

            vars = rawType.getTypeParameters();
            // Sanity check:
            if (vars.length != args.length) {
                throw new IllegalArgumentException
                    ("Strange parametrized type (raw: "+rawType+"): number of type arguments != number of type parameters ("+args.length+" vs "+vars.length+")");
            }
            types = new HashMap<String,JavaType>();
            for (int i = 0, len = args.length; i < len; ++i) {
                types.put(vars[i].getName(), _fromType(args[i], context));
            }
        }
        /* Neither: well, let's just consider it a bean or such;
         * may or not may not be a problem.
         */
        return _fromClass(rawType, types);
    }

    protected JavaType _fromArrayType(GenericArrayType type, JavaType context)
    {
        JavaType compType = _fromType(type.getGenericComponentType(), context);
        return ArrayType.construct(compType);
    }

    protected JavaType _fromVariable(TypeVariable<?> type, JavaType context)
    {
        /* 26-Sep-2009, tatus: It should be possible to try "partial"
         *  resolution; meaning that it is ok not to find bindings.
         *  For now this is indicated by passing null context.
         */
        if (context == null) {
            return _unknownType();
        }

        // Ok: here's where context might come in handy!
        String name = type.getName();
        JavaType actualType = context.findVariableType(name);
        if (actualType != null) {
            return actualType;
        }

        /* 16-Jun-2009, tatu: Instead of trying to figure out graceful
         *   fallback, let's just throw an Exception: chances are caller
         *   wouldn't find it very intuitive to get "untyped" binding.
         *   Plus variables just should not remain unexpanded.
         */
        throw new IllegalArgumentException
            ("Unresolved TypeVariable <"+name+"> (from "+type.getGenericDeclaration()+")");

        // Old code for reference:

        /*
        Type[] bounds = type.getBounds();

        // With type variables we must use bound information.
        // Theoretically this gets tricky, as there may be multiple
        // bounds ("... extends A & B"); and optimally we might
        // want to choose the best match. Also, bounds are optional;
        // but here we are lucky in that implicit "Object" is
        // added as bounds if so.
        // Either way let's just use the first bound, for now, and
        // worry about better match later on if there is need.
        return _fromType(bounds[0]);
        */
    }

    protected JavaType _fromWildcard(WildcardType type, JavaType context)
    {
        /* Similar to challenges with TypeVariable, we may have
         * multiple upper bounds. But it is also possible that if
         * upper bound defaults to Object, we might want to consider
         * lower bounds instead.
         *
         * For now, we won't try anything more advanced; above is
         * just for future reference.
         */
        return _fromType(type.getUpperBounds()[0], context);
    }

    /**
     * Method that is to figure out actual type parameters that given
     * class binds to generic types defined by given interface
     * type. This could mean, for example, trying to figure out
     * key and value types for Map implementations.
     */
    @SuppressWarnings("unchecked")
    protected <T extends JavaType> T _findParentType(Class<?> clz, Class<T> expType)
    {
        Type parentType = clz.getGenericSuperclass();
        if (parentType != null) {
            // Need to have context, for now
            JavaType parent = _fromType(parentType, null);
            // This should always be true, but let's ensure:
            if (expType.isAssignableFrom(parent.getClass())) {
                return (T) parent;
            }
        }
        // no, couldn't find
        return null;
    }

    protected JavaType _unknownType() {
        return _fromClass(Object.class, null);
    }
}
