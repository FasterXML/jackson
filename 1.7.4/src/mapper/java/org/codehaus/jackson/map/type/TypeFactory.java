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
 * mapType(String.class, Integer.class)
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

    private final static JavaType[] NO_TYPES = new JavaType[0];
    
    protected final TypeParser _parser;
    
    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    private TypeFactory() {
        _parser = new TypeParser(this);
    }

    /*
    /**********************************************************
    /* Public factory methods
    /**********************************************************
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
     * @param context Class used for resolving generic types; for example,
     *    for bean properties the actual bean class (not necessarily class
     *    that contains method or field, may be a sub-class thereof)
     */
    public static JavaType type(Type type, Class<?> context)
    {
        return type(type, new TypeBindings(context));
    }

    /**
     * @since 1.7.0
     */
    public static JavaType type(Type type, JavaType context)
    {
        return type(type, new TypeBindings(context));
    }
    
    public static JavaType type(Type type, TypeBindings bindings)
    {
        return instance._fromType(type, bindings);
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

    /**
     * Factory method for constructing {@link JavaType} that
     * represents a parameterized type. For example, to represent
     * type <code>Iterator&lt;String></code>, you could
     * call
     *<pre>
     *  TypeFactory.parametricType(Iterator.class, String.class)
     *</pre>
     *
     * @since 1.5
     */
    public static JavaType parametricType(Class<?> parametrized, Class<?>... parameterClasses)
    {
        int len = parameterClasses.length;
        JavaType[] pt = new JavaType[len];
        for (int i = 0; i < len; ++i) {
            pt[i] = instance._fromClass(parameterClasses[i], null);
        }
        return parametricType(parametrized, pt);
    }

    /**
     * Factory method for constructing {@link JavaType} that
     * represents a parameterized type. For example, to represent
     * type <code>List&lt;Set&lt;Integer>></code>, you could
     * call
     *<pre>
     *  JavaType inner = TypeFactory.parametricType(Set.class, Integer.class);
     *  TypeFactory.parametricType(List.class, inner);
     *</pre>
     *
     * @since 1.5
     */
    public static JavaType parametricType(Class<?> parametrized, JavaType... parameterTypes)
    {
        // Need to check kind of class we are dealing with...
        if (parametrized.isArray()) {
            // 19-Jan-2010, tatus: should we support multi-dimensional arrays directly?
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException("Need exactly 1 parameter type for arrays ("+parametrized.getName()+")");
            }
            return ArrayType.construct(parameterTypes[0]);
        }
        if (Map.class.isAssignableFrom(parametrized)) {
            if (parameterTypes.length != 2) {
                throw new IllegalArgumentException("Need exactly 2 parameter types for Map types ("+parametrized.getName()+")");
            }
            return MapType.construct(parametrized, parameterTypes[0], parameterTypes[1]);
        }
        if (Collection.class.isAssignableFrom(parametrized)) {
            if (parameterTypes.length != 1) {
                throw new IllegalArgumentException("Need exactly 1 parameter type for Collection types ("+parametrized.getName()+")");
            }
            return CollectionType.construct(parametrized, parameterTypes[0]);
        }
        return _constructSimple(parametrized, parameterTypes);
    }

    /**
     * Factory method for constructing a {@link JavaType} out of its canonical
     * representation (see {@link JavaType#toCanonical()}).
     * 
     * @param canonical Canonical string representation of a type
     * 
     * @throws IllegalArgumentException If canonical representation is malformed,
     *   or class that type represents (including its generic parameters) is
     *   not found
     * 
     * @since 1.5
     */
    public static JavaType fromCanonical(String canonical)
        throws IllegalArgumentException
    {
        return instance._parser.parse(canonical);
    }
    
    /*
    /**********************************************************
    /* Type conversions
    /**********************************************************
     */

    /**
     * Method that tries to create specialized type given base type, and
     * a sub-class thereof (which is assumed to use same parametrization
     * as supertype). Similar to calling {@link JavaType#narrowBy(Class)},
     * but can change underlying {@link JavaType} (from simple to Map, for
     * example), unliked <code>narrowBy</code> which assumes same logical
     * type.
     */
    public static JavaType specialize(JavaType baseType, Class<?> subclass)
    {
        // Currently only SimpleType instances can become something else
    	if (baseType instanceof SimpleType) {
            // and only if subclass is an array, Collection or Map
            if (subclass.isArray()
                || Map.class.isAssignableFrom(subclass)
                || Collection.class.isAssignableFrom(subclass)) {
                // need to assert type compatibility...
                if (!baseType.getRawClass().isAssignableFrom(subclass)) {
                    throw new IllegalArgumentException("Class "+subclass.getClass().getName()+" not subtype of "+baseType);
                }
                // this _should_ work, right?
                JavaType subtype = instance._fromClass(subclass, new TypeBindings(baseType.getRawClass()));
                // one more thing: handlers to copy?
                Object h = baseType.getValueHandler();
                if (h != null) {
                    subtype.setValueHandler(h);
                }
                h = baseType.getTypeHandler();
                if (h != null) {
                    subtype = subtype.withTypeHandler(h);
                }
                return subtype;
            }
        }
        // otherwise regular narrowing should work just fine
        return baseType.narrowBy(subclass);
    }

    /**
     * Method that can be used if it is known for sure that given type
     * is not a structured type (array, Map, Collection).
     * NOTE: use of this method is discouraged due to its potential
     * non-safety; in most cases you should just use basic
     * {@link #type(Type)} instead.
     * 
     * @since 1.6
     */
    public static JavaType fastSimpleType(Class<?> cls)
    {
        return new SimpleType(cls, null, null);
    }

    /**
     * Method that is to figure out actual type parameters that given
     * class binds to generic types defined by given (generic)
     * interface or class.
     * This could mean, for example, trying to figure out
     * key and value types for Map implementations.
     * 
     * @since 1.6
     */
    public static JavaType[] findParameterTypes(Class<?> clz, Class<?> expType)
    {
        return findParameterTypes(clz, expType, new TypeBindings(clz));
    }
        
    public static JavaType[] findParameterTypes(Class<?> clz, Class<?> expType, TypeBindings bindings)
    {
        // First: find full inheritance chain
        HierarchicType subType = _findSuperTypeChain(clz, expType);
        // Caller is supposed to ensure this never happens, so:
        if (subType == null) {
            throw new IllegalArgumentException("Class "+clz.getName()+" is not a subtype of "+expType.getName());
        }
        // Ok and then go to the ultimate super-type:
        HierarchicType superType = subType;
        while (superType.getSuperType() != null) {
            superType = superType.getSuperType();
            Class<?> raw = superType.getRawClass();
            TypeBindings newBindings = new TypeBindings(raw);
            if (superType.isGeneric()) { // got bindings, need to resolve
                ParameterizedType pt = superType.asGeneric();
                Type[] actualTypes = pt.getActualTypeArguments();
                TypeVariable<?>[] vars = raw.getTypeParameters();
                int len = actualTypes.length;
                for (int i = 0; i < len; ++i) {
                    String name = vars[i].getName();
                    JavaType type = instance._fromType(actualTypes[i], bindings);
                    newBindings.addBinding(name, type);
                }
            }
            bindings = newBindings;
        }

        // which ought to be generic (if not, it's raw type)
        if (!superType.isGeneric()) {
            return null;
        }
        return bindings.typesAsArray();
    }

    /**
     * Method that is to figure out actual type parameters that given
     * class binds to generic types defined by given (generic)
     * interface or class.
     * This could mean, for example, trying to figure out
     * key and value types for Map implementations.
     * 
     * @param type Sub-type (leaf type) that implements <code>expType</code>
     * 
     * @since 1.6
     */
    public static JavaType[] findParameterTypes(JavaType type, Class<?> expType)
    {
        /* Tricky part here is that some JavaType instances have been constructed
         * from generic type (usually via TypeReference); and in those case
         * types have been resolved. Alternative is that the leaf type is type-erased
         * class, in which case this has not been done.
         * For now simplest way to handle this is to split processing in two: latter
         * case actually fully works; and former mostly works. In future may need to
         * rewrite former part, which requires changes to JavaType as well.
         */
        Class<?> raw = type.getRawClass();
        if (raw == expType) {
            // Direct type info; good since we can return it as is
            int count = type.containedTypeCount();
            if (count == 0) return null;
            JavaType[] result = new JavaType[count];
            for (int i = 0; i < count; ++i) {
                result[i] = type.containedType(i);
            }
            return result;
        }
        /* Otherwise need to go through type-erased class. This may miss cases where
         * we get generic type; ideally JavaType/SimpleType would retain information
         * about generic declaration at main level... but let's worry about that
         * if/when there are problems; current handling is an improvement over earlier
         * code.
         */
        return findParameterTypes(raw, expType, new TypeBindings(type));
    }    
    
    /*
    /**********************************************************
    /* Legacy methods
    /**********************************************************
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
    public static JavaType fromType(Type type)
    {
        return instance._fromType(type, null);
    }

    /*
    /**********************************************************
    /* Internal methods
    /**********************************************************
     */
    
    /**
     * @param context Mapping of formal parameter declarations (for generic
     *   types) into actual types
     */
    protected JavaType _fromClass(Class<?> clz, TypeBindings context)
    {
        // First: do we have an array type?
        if (clz.isArray()) {
            return ArrayType.construct(_fromType(clz.getComponentType(), null));
        }
        /* Also: although enums can also be fully resolved, there's little
         * point in doing so (T extends Enum<T>) etc.
         */
        if (clz.isEnum()) {
            return new SimpleType(clz);
        }
        /* Maps and Collections aren't quite as hot; problem is, due
         * to type erasure we often do not know typing and can only assume
         * base Object.
         */
        if (Map.class.isAssignableFrom(clz)) {
            return _mapType(clz);
        }
        if (Collection.class.isAssignableFrom(clz)) {
            return _collectionType(clz);
        }
        return new SimpleType(clz);
    }
    
    /**
     * Method used by {@link TypeParser} when generics-aware version
     * is constructed.
     */
    protected JavaType _fromParameterizedClass(Class<?> clz, List<JavaType> paramTypes)
    {
        if (clz.isArray()) { // ignore generics (should never have any)
            return ArrayType.construct(_fromType(clz.getComponentType(), null));
        }
        if (clz.isEnum()) { // ditto for enums
            return new SimpleType(clz);
        }
        if (Map.class.isAssignableFrom(clz)) {
            // First: if we do have param types, use them
            JavaType keyType, contentType;
            if (paramTypes.size() > 0) {
                keyType = paramTypes.get(0);
                contentType = (paramTypes.size() >= 2) ?
                        paramTypes.get(1) : _unknownType();
                return MapType.construct(clz, keyType, contentType);
            }
            return _mapType(clz);
        }
        if (Collection.class.isAssignableFrom(clz)) {
            if (paramTypes.size() >= 1) {
                return CollectionType.construct(clz, paramTypes.get(0));
            }
            return _collectionType(clz);
        }
        if (paramTypes.size() == 0) {
            return new SimpleType(clz);
        }
        JavaType[] pt = paramTypes.toArray(new JavaType[paramTypes.size()]);
        return _constructSimple(clz, pt);
    }
    
    /**
     * Factory method that can be used if type information is passed
     * as Java typing returned from <code>getGenericXxx</code> methods
     * (usually for a return or argument type).
     */
    public JavaType _fromType(Type type, TypeBindings context)
    {
        // simple class?
        if (type instanceof Class<?>) {
            Class<?> cls = (Class<?>) type;
            /* 24-Mar-2010, tatu: Better create context if one was not passed;
             *   mostly matters for root serialization types
             */
            if (context == null) {
                context = new TypeBindings(cls);
            }
            return _fromClass(cls, context);
        }
        // But if not, need to start resolving.
        if (type instanceof ParameterizedType) {
            return _fromParamType((ParameterizedType) type, context);
        }
        if (type instanceof GenericArrayType) {
            return _fromArrayType((GenericArrayType) type, context);
        }
        if (type instanceof TypeVariable<?>) {
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
     * Since version 1.2, this resolves all parameterized types, not just
     * Maps or Collections.
     */
    protected JavaType _fromParamType(ParameterizedType type, TypeBindings context)
    {
        /* First: what is the actual base type? One odd thing
         * is that 'getRawType' returns Type, not Class<?> as
         * one might expect. But let's assume it is always of
         * type Class: if not, need to add more code to resolve
         * it to Class.
         */
        Class<?> rawType = (Class<?>) type.getRawType();
        Type[] args = type.getActualTypeArguments();
        int paramCount = (args == null) ? 0 : args.length;

        JavaType[] pt;
        
        if (paramCount == 0) {
            pt = NO_TYPES;
        } else {
            pt = new JavaType[paramCount];
            for (int i = 0; i < paramCount; ++i) {
                pt[i] = _fromType(args[i], context);
            }
        }

        // Ok: Map or Collection?
        if (Map.class.isAssignableFrom(rawType)) {
            JavaType subtype = _constructSimple(rawType, pt);
            JavaType[] mapParams = findParameterTypes(subtype, Map.class);
            if (mapParams.length != 2) {
                throw new IllegalArgumentException("Could not find 2 type parameters for Map class "+rawType.getName()+" (found "+mapParams.length+")");
            }
            return MapType.construct(rawType, mapParams[0], mapParams[1]);
        }
        if (Collection.class.isAssignableFrom(rawType)) {
            JavaType subtype = _constructSimple(rawType, pt);
            JavaType[] collectionParams = findParameterTypes(subtype, Collection.class);
            if (collectionParams.length != 1) {
                throw new IllegalArgumentException("Could not find 1 type parameter for Collection class "+rawType.getName()+" (found "+collectionParams.length+")");
            }
            return CollectionType.construct(rawType, collectionParams[0]);
        }
        if (paramCount == 0) { // no generics
            return new SimpleType(rawType);
        }
        return _constructSimple(rawType, pt);
    }

    protected static SimpleType _constructSimple(Class<?> rawType, JavaType[] parameterTypes)
    {
        // Quick sanity check: must match numbers of types with expected...
        TypeVariable<?>[] typeVars = rawType.getTypeParameters();
        if (typeVars.length != parameterTypes.length) {
            throw new IllegalArgumentException("Parameter type mismatch for "+rawType.getName()
                    +": expected "+typeVars.length+" parameters, was given "+parameterTypes.length);
        }
        String[] names = new String[typeVars.length];
        for (int i = 0, len = typeVars.length; i < len; ++i) {
            names[i] = typeVars[i].getName();
        }
        return new SimpleType(rawType, names, parameterTypes);
    } 
    
    protected JavaType _fromArrayType(GenericArrayType type, TypeBindings context)
    {
        JavaType compType = _fromType(type.getGenericComponentType(), context);
        return ArrayType.construct(compType);
    }

    protected JavaType _fromVariable(TypeVariable<?> type, TypeBindings context)
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
        JavaType actualType = context.findType(name);
        if (actualType != null) {
            return actualType;
        }

        /* 29-Jan-2010, tatu: We used to throw exception here, if type was
         *   bound: but the problem is that this can occur for generic "base"
         *   method, overridden by sub-class. If so, we will want to ignore
         *   current type (for method) since it will be masked.
         */
        Type[] bounds = type.getBounds();

        // With type variables we must use bound information.
        // Theoretically this gets tricky, as there may be multiple
        // bounds ("... extends A & B"); and optimally we might
        // want to choose the best match. Also, bounds are optional;
        // but here we are lucky in that implicit "Object" is
        // added as bounds if so.
        // Either way let's just use the first bound, for now, and
        // worry about better match later on if there is need.

        /* 29-Jan-2010, tatu: One more problem are recursive types
         *   (T extends Comparable<T>). Need to add "placeholder"
         *   for resolution to catch those.
         */
        context._addPlaceholder(name);        
        return _fromType(bounds[0], context);
    }

    protected JavaType _fromWildcard(WildcardType type, TypeBindings context)
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

    private JavaType _mapType(Class<?> rawClass)
    {
        JavaType[] typeParams = findParameterTypes(rawClass, Map.class);
        // ok to have no types ("raw")
        if (typeParams == null) {
            return MapType.construct(rawClass, _unknownType(), _unknownType());
        }
        // but exactly 2 types if any found
        if (typeParams.length != 2) {
            throw new IllegalArgumentException("Strange Map type "+rawClass.getName()+": can not determine type parameters");
        }
        return MapType.construct(rawClass, typeParams[0], typeParams[1]);
    }

    private JavaType _collectionType(Class<?> rawClass)
    {
        JavaType[] typeParams = findParameterTypes(rawClass, Collection.class);
        // ok to have no types ("raw")
        if (typeParams == null) {
            return CollectionType.construct(rawClass, _unknownType());
        }
        // but exactly 2 types if any found
        if (typeParams.length != 1) {
            throw new IllegalArgumentException("Strange Collection type "+rawClass.getName()+": can not determine type parameters");
        }
        return CollectionType.construct(rawClass, typeParams[0]);
    }    

    protected static JavaType _resolveVariableViaSubTypes(HierarchicType leafType, String variableName, TypeBindings bindings)
    {
        // can't resolve raw types; possible to have as-of-yet-unbound types too:
        if (leafType != null && leafType.isGeneric()) {
            TypeVariable<?>[] typeVariables = leafType.getRawClass().getTypeParameters();
            for (int i = 0, len = typeVariables.length; i < len; ++i) {
                TypeVariable<?> tv = typeVariables[i];
                if (variableName.equals(tv.getName())) {
                    // further resolution needed?
                    Type type = leafType.asGeneric().getActualTypeArguments()[i];
                    if (type instanceof TypeVariable<?>) {
                        return _resolveVariableViaSubTypes(leafType.getSubType(), ((TypeVariable<?>) type).getName(), bindings);
                    }
                    // no we're good for the variable (but it may have parameterization of its own)
                    return instance._fromType(type, bindings);
                }
            }
        }
        return instance._unknownType();
    }
    
    protected JavaType _unknownType() {
        return _fromClass(Object.class, null);
    }

    /**
     * Helper method used to find inheritance (implements, extends) path
     * between given types, if one exists (caller generally checks before
     * calling this method). Returned type represents given <b>subtype</b>,
     * with supertype linkage extending to <b>supertype</b>.
     */
    protected static HierarchicType  _findSuperTypeChain(Class<?> subtype, Class<?> supertype)
    {
        // If super-type is a class (not interface), bit simpler
        if (supertype.isInterface()) {
            return _findSuperInterfaceChain(subtype, supertype);
        }
        return _findSuperClassChain(subtype, supertype);
    }

    protected static HierarchicType _findSuperClassChain(Type currentType, Class<?> target)
    {
        HierarchicType current = new HierarchicType(currentType);
        Class<?> raw = current.getRawClass();
        if (raw == target) {
            return current;
        }
        // Otherwise, keep on going down the rat hole...
        Type parent = raw.getGenericSuperclass();
        if (parent != null) {
            HierarchicType sup = _findSuperClassChain(parent, target);
            if (sup != null) {
                sup.setSubType(current);
                current.setSuperType(sup);
                return current;
            }
        }
        return null;
    }

    protected static HierarchicType _findSuperInterfaceChain(Type currentType, Class<?> target)
    {
        HierarchicType current = new HierarchicType(currentType);
        Class<?> raw = current.getRawClass();
        if (raw == target) {
            return current;
        }
        // Otherwise, keep on going down the rat hole; first implemented interfaces
        Type[] parents = raw.getGenericInterfaces();
        // as long as there are superclasses
        // and unless we have already seen the type (<T extends X<T>>)
        if (parents != null) {
            for (Type parent : parents) {
                HierarchicType sup = _findSuperInterfaceChain(parent, target);
                if (sup != null) {
                    sup.setSubType(current);
                    current.setSuperType(sup);
                    return current;
                }
            }
        }
        // and then super-class if any
        Type parent = raw.getGenericSuperclass();
        if (parent != null) {
            HierarchicType sup = _findSuperInterfaceChain(parent, target);
            if (sup != null) {
                sup.setSubType(current);
                current.setSuperType(sup);
                return current;
            }
        }
        return null;
    }

    /*
    protected static final Class<?> _typeToClass(Type type)
    {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        }
        if (type instanceof ParameterizedType) {
            return (Class<?>)((ParameterizedType) type).getRawType();
        }
        // we don't really support other types; GenericArrayType may or may not need support in future?
        throw new IllegalArgumentException("Can not coerce Type "+type.getClass().getName()+" into Class<?>");
    }
    */
}
