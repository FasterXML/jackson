package org.codehaus.jackson.map.type;

import java.lang.reflect.*;
import java.util.*;

import org.codehaus.jackson.type.JavaType;

/**
 * Helper class used for resolving type parameters for given class
 * 
 * @since 1.5
 */
public class TypeBindings
{
    /**
     * Marker to use for (temporarily) unbound references.
     */
    public final static JavaType UNBOUND = new SimpleType(Object.class);

    /**
     * Context type used for resolving all types, if specified. May be null,
     * in which case {@link _contextClass} is used instead.
     */
    protected final JavaType _contextType;

    /**
     * Specific class to use for resolving all types, for methods and fields
     * class and its superclasses and -interfaces contain.
     */
    protected final Class<?> _contextClass;

    /**
     * Lazily-instantiated bindings of resolved type parameters
     */
    protected Map<String,JavaType> _bindings;

    /**
     * Also: we may temporarily want to mark certain named types
     * as resolved (but without exact type); if so, we'll just store
     * names here.
     */
    protected HashSet<String> _placeholders;
    
    public TypeBindings(Class<?> cc) {
        _contextClass = cc;
        _contextType = null;
    }

    public TypeBindings(JavaType type)
    {
        _contextType = type;
        _contextClass = type.getRawClass();
    }

    public int getBindingCount() {
        if (_bindings == null) {
            _resolve();
        }
        return _bindings.size();
    }
    
    public JavaType findType(String name)
    {
        if (_bindings == null) {
            _resolve();
        }
        JavaType t = _bindings.get(name);
        if (t == null) {
            if (_placeholders != null && _placeholders.contains(name)) {
                t = UNBOUND;
            } else {
                // Should we throw an exception or just return null?
                throw new IllegalArgumentException("Type variable '"+name
                        +"' can not be resolved (with context of class "+_contextClass.getName()+")");
                //t = UNBOUND;                
            }
        }
        return t;
    }

    /*
    /*******************************************************************8
    /* Internal methods
    /*******************************************************************8
     */
    
    protected void _resolve()
    {
        _resolveBindings(_contextClass);

        // finally: may have root level type info too
        if (_contextType != null) {
            int count = _contextType.containedTypeCount();
            if (count > 0) {
                if (_bindings == null) {
                    _bindings = new HashMap<String,JavaType>();
                }
                for (int i = 0; i < count; ++i) {
                    String name = _contextType.containedTypeName(i);
                    JavaType type = _contextType.containedType(i);
                    _bindings.put(name, type);
                }
            }
        }

        // nothing bound? mark with empty map to prevent further calls
        if (_bindings == null) {
            _bindings = Collections.emptyMap();
        }
    }

    public void _addPlaceholder(String name) {
        if (_placeholders == null) {
            _placeholders = new HashSet<String>();
        }
        _placeholders.add(name);
    }

    protected void _resolveBindings(Type t)
    {
        if (t == null) return;
        
        Class<?> raw;
        if (t instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) t;
            Type[] args = pt.getActualTypeArguments();
            if (args  != null && args.length > 0) {
                Class<?> rawType = (Class<?>) pt.getRawType();    
                TypeVariable<?>[] vars = rawType.getTypeParameters();
                if (vars.length != args.length) {
                    throw new IllegalArgumentException("Strange parametrized type (in class "+rawType.getName()+"): number of type arguments != number of type parameters ("+args.length+" vs "+vars.length+")");
                }
                for (int i = 0, len = args.length; i < len; ++i) {
                    TypeVariable<?> var = vars[i];
                    String name = var.getName();
                    if (_bindings == null) {
                        _bindings = new HashMap<String,JavaType>();
                    } else {
                        /* 24-Mar-2010, tatu: Better ensure that we do not overwrite something
                         *  collected earlier (since we descend towards super-classes):
                         */
                        if (_bindings.containsKey(name)) continue;
                    }
                    // first: add a placeholder to prevent infinite loops
                    _addPlaceholder(name);
                    // then resolve type
                    _bindings.put(name, TypeFactory.instance._fromType(args[i], this));
                }
            }
            raw = (Class<?>)pt.getRawType();
        } else if (t instanceof Class<?>) {
            raw = (Class<?>) t;
            /* 24-Mar-2010, tatu: Can not have true generics definitions, but can
             *   have lower bounds ("<T extends BeanBase>") in declaration itself
             */
            TypeVariable<?>[] vars = raw.getTypeParameters();
            if (vars != null && vars.length > 0) {
                for (TypeVariable<?> var : vars) {
                    String name = var.getName();
                    Type varType = var.getBounds()[0];
                    if (varType != null) {
                        if (_bindings == null) {
                            _bindings = new HashMap<String,JavaType>();
                        } else { // and no overwriting...
                            if (_bindings.containsKey(name)) continue;
                        }
                        _addPlaceholder(name); // to prevent infinite loops
                        _bindings.put(name, TypeFactory.instance._fromType(varType, this));
                    }
                }
            }
        } else { // probably can't be any of these... so let's skip for now
            //if (type instanceof GenericArrayType) {
            //if (type instanceof TypeVariable<?>) {
            // if (type instanceof WildcardType) {
            return;
        }
        // but even if it's not a parameterized type, its super types may be:
        _resolveBindings(raw.getGenericSuperclass());
        for (Type intType : raw.getGenericInterfaces()) {
            _resolveBindings(intType);
        }
    }

    @Override
    public String toString()
    {
        if (_bindings == null) {
            _resolve();
        }
        StringBuilder sb = new StringBuilder("[TypeBindings for ");
        if (_contextType != null) {
            sb.append(_contextType.toString());
        } else {
            sb.append(_contextClass.getName());
        }
        sb.append(": ").append(_bindings).append("]");
        return sb.toString();
    }
}
