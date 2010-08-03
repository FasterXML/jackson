package org.codehaus.jackson.map.type;

import java.util.List;

import org.codehaus.jackson.type.JavaType;

public abstract class TypeBase extends JavaType
{
    /**
     * Lazily initialized external representation of the type
     */
    volatile String _canonicalName;

    protected TypeBase(Class<?> raw) {
        super(raw);
    }
    
    @Override
    public String toCanonical()
    {
    	String str = _canonicalName;
    	if (str == null) {
            str = buildCanonicalName();
    	}
    	return str;
    }
    
    protected abstract String buildCanonicalName();

    protected final JavaType copyHandlers(JavaType fromType)
    {
        _valueHandler = fromType.getValueHandler();
        _typeHandler = fromType.getTypeHandler();
        return this;
    }

    public abstract StringBuilder getGenericSignature(StringBuilder sb);

    public abstract StringBuilder getErasedSignature(StringBuilder sb);

    public List<JavaType> findGenericTypesFor(Class<?> genericClass)
    {
        // first, sanity check: 
        if (!genericClass.isAssignableFrom(_class)) {
            return null;
        }
        // then need to start resolving...
        
        // !!! TBI
        throw new Error();
    }
    
    /*
    /**********************************************************
    /* Methods for sub-classes to use
    /**********************************************************
     */

    /**
     * @param trailingSemicolon Whether to add trailing semicolon for non-primitive
     *   (reference) types or not
     */
    protected static StringBuilder _classSignature(Class<?> cls, StringBuilder sb,
           boolean trailingSemicolon)
    {
        if (cls.isPrimitive()) {
            if (cls == Boolean.TYPE) {                
                sb.append('Z');
            } else if (cls == Byte.TYPE) {
                sb.append('B');
            }
            else if (cls == Short.TYPE) {
                sb.append('S');
            }
            else if (cls == Character.TYPE) {
                sb.append('C');
            }
            else if (cls == Integer.TYPE) {
                sb.append('I');
            }
            else if (cls == Long.TYPE) {
                sb.append('J');
            }
            else if (cls == Float.TYPE) {
                sb.append('F');
            }
            else if (cls == Double.TYPE) {
                sb.append('D');
            }
            else if (cls == Void.TYPE) {
                sb.append('V');
            } else {
                throw new IllegalStateException("Unrecognized primitive type: "+cls.getName());
            }
        } else {
            sb.append('L');
            String name = cls.getName();
            for (int i = 0, len = name.length(); i < len; ++i) {
                char c = name.charAt(i);
                if (c == '.') c = '/';
                sb.append(c);
            }
            if (trailingSemicolon) {
                sb.append(';');
            }
        }
        return sb;
    }
}
